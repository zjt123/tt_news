package com.tiantian.news.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.tiantian.news.db.NewsDatabase;
import com.tiantian.news.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

public class ImageLoader {
	private static final String TAG = "ImageLoader";
	private static final int MAX_CAPACITY = 20;// 一级缓存的最大空间
	private static final int DEFAULT_BUFFER_SIZE = 8 * 1024; // 8 KB
	private static final int DEFAULT_DURATION_MILLIS = 600;

	private static ImageLoader sINSTANCE = new ImageLoader();

	public static ImageLoader getInstance() {
		return sINSTANCE;
	}

	private ImageLoader() {

	}

	/** 正在下载或者下载的集合 **/
	private ArrayList<String> mLoading = new ArrayList<String>();
	
	// 0.75是加载因子为经验值，true则表示按照最近访问量的高低排序，false则表示按照插入顺序排序
	private HashMap<String, Bitmap> mFirstLevelCache = new LinkedHashMap<String, Bitmap>(
			MAX_CAPACITY / 2, 0.75f, true) {
		private static final long serialVersionUID = 1L;

		protected boolean removeEldestEntry(Entry<String, Bitmap> eldest) {
			if (size() > MAX_CAPACITY) {// 当超过一级缓存阈值的时候，将老的值从一级缓存搬到二级缓存
				mSecondLevelCache.put(eldest.getKey(),
						new SoftReference<Bitmap>(eldest.getValue()));
				return true;
			}
			return false;
		};
	};

	// 二级缓存，采用的是软应用，只有在内存吃紧的时候软应用才会被回收，有效的避免了oom
	private ConcurrentHashMap<String, SoftReference<Bitmap>> mSecondLevelCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>(
			MAX_CAPACITY / 2);

	/**
	 * 清理缓存
	 * 退出时候调用
	 */
	public void destoryClearCache() {
		ImageLoadTask.destoryClearQueue();
		Collection<Bitmap> bitmaps = mFirstLevelCache.values();
		mFirstLevelCache.clear();
		if (bitmaps.size() > 0) {
			for (Bitmap bitmap : bitmaps) {
				bitmap.recycle();
				bitmap = null;
			}
		}
		mSecondLevelCache.clear();
		mLoading.clear();
	}

	/**
	 * 返回缓存，如果没有则返回null
	 * 
	 * @param url
	 * @return
	 */
	public Bitmap getBitmapFromCache(String url) {
		Bitmap bitmap = null;
		bitmap = getFromFirstLevelCache(url);// 从一级缓存中拿
		if (bitmap != null) {
			return bitmap;
		}
		bitmap = getFromSecondLevelCache(url);// 从二级缓存中拿
		return bitmap;
	}

	/**
	 * 从二级缓存中拿
	 * 
	 * @param url
	 * @return
	 */
	private Bitmap getFromSecondLevelCache(String url) {
		Bitmap bitmap = null;
		SoftReference<Bitmap> softReference = mSecondLevelCache.get(url);
		if (softReference != null) {
			bitmap = softReference.get();
			if (bitmap == null) {// 由于内存吃紧，软引用已经被gc回收了
				mSecondLevelCache.remove(url);
			}
		}
		return bitmap;
	}

	/**
	 * 从一级缓存中拿
	 * 
	 * @param url
	 * @return
	 */
	private Bitmap getFromFirstLevelCache(String url) {
//		Bitmap bitmap = null;
//		synchronized (mFirstLevelCache) {
//			bitmap = mFirstLevelCache.get(url);
//			if (bitmap != null) {// 将最近访问的元素放到链的头部，提高下一次访问该元素的检索速度（LRU算法）
//				mFirstLevelCache.remove(url);
//				mFirstLevelCache.put(url, bitmap);
//			}
//		}
//		return bitmap;
		return mFirstLevelCache.get(url);
	}

	/**
	 * 加载图片，如果缓存中有就直接从缓存中拿，缓存中没有就下载
	 * @param url
	 * @param adapter
	 * @param holder
	 */
	public void loadImage(String url, ImageView imageView, String path, int channelId, long _id) {
		if(TextUtils.isEmpty(url)) {//没有新闻的图片
			imageView.setImageResource(R.drawable.ic_launcher);//默认图片
			return ;
		}
		Bitmap bitmap = getBitmapFromCache(url);// 从缓存中读取
		if (bitmap == null) {
			imageView.setImageResource(R.drawable.ic_launcher);//缓存没有设为默认图片
			//TODO 判断是否正在下载,加载
			String loadingTag = channelId + "_" + url;
			synchronized (mLoading) {
				boolean contains = mLoading.contains(loadingTag);
//				Log.v("zhang", contains+ " -- "+loadingTag);
				if (contains) {
					return;
				}
				mLoading.add(loadingTag);
			}
			ImageLoadTask imageLoadTask = new ImageLoadTask();
			imageLoadTask.execute(url, imageView, path, channelId, _id);
		} else {
			imageView.setImageBitmap(bitmap);//设为缓存图片
		}
	}

	/**
	 * 放入缓存
	 * 
	 * @param url
	 * @param value
	 */
	public void addImage2Cache(String url, Bitmap value) {
		if (value == null || url == null) {
			return;
		}
		synchronized (mFirstLevelCache) {
			mFirstLevelCache.put(url, value);
		}
	}

	class ImageLoadTask extends MyAsyncTask<Object, Void, Bitmap> {
		private String mUrl, mPath;
		private ImageView mImageView;
		private int mChannelId;
		private boolean isDownload;

		@Override
		protected Bitmap doInBackground(Object... params) {
			mUrl = (String) params[0];
			mImageView = (ImageView) params[1];
			mPath = (String) params[2];
			mChannelId = (Integer) params[3];
			File imageFile = null;
			Bitmap drawable = null;
			if(TextUtils.isEmpty(mPath)) {
				imageFile = FileUtils.getFileByChannelId(mChannelId, mUrl);
			} else {
				imageFile = new File(mPath);
			}
			boolean isExists = imageFile.exists();
			Log.v("peng"," - imageFile = "+isExists+" -- "+imageFile.getAbsolutePath());
			if (isExists) {//加载
				drawable = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
			} else {//下载
				isDownload = true;
				drawable = loadImageFromInternet(mUrl, imageFile, (Long) params[4]);// 获取网络图片
			}
			synchronized (mLoading) {
				mLoading.remove(mChannelId + "_" + mUrl);
			}
			return drawable;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result == null) {
				return;
			}
			Object tag = mImageView.getTag();
			if (tag != null && tag.toString().equals(mUrl)) {
				mImageView.setImageBitmap(result);
				if (isDownload) {
					fdeInAnimationBitmap(mImageView, DEFAULT_DURATION_MILLIS);
				}
			}
			addImage2Cache(mUrl, result);// 放入缓存
//			adapter.notifyDataSetChanged();// 触发getView方法执行，这个时候getView实际上会拿到刚刚缓存好的图片
		}
	}

	public Bitmap loadImageFromInternet(String url, File file, long _id) {
		Bitmap bitmap = null;
		HttpClient client = AndroidHttpClient.newInstance("Android");
		HttpParams params = client.getParams();
		HttpConnectionParams.setConnectionTimeout(params, 10000);
		HttpConnectionParams.setSoTimeout(params, 10000);
		HttpConnectionParams.setSocketBufferSize(params, DEFAULT_BUFFER_SIZE);
		HttpResponse response = null;
		InputStream is = null;
		ByteArrayOutputStream baos = null;
		BufferedOutputStream os = null;
		HttpGet httpGet = null;
		byte[] buffer = null;
		HttpEntity entity = null;
		try {
			httpGet = new HttpGet(url);
			response = client.execute(httpGet);
			int stateCode = response.getStatusLine().getStatusCode();
			Log.d(TAG, "  loadImageFromInternet  [loadImage] stateCode = " + stateCode);
			if (stateCode != HttpStatus.SC_OK) {
				return bitmap;
			}
			entity = response.getEntity();
			if (entity != null) {
				is = entity.getContent();
				baos = new ByteArrayOutputStream();
				buffer = new byte[DEFAULT_BUFFER_SIZE];
				int len = 0;
				while ((len = is.read(buffer, 0, DEFAULT_BUFFER_SIZE)) != -1) {
					baos.write(buffer, 0, len);
				}
				baos.flush();
				buffer = baos.toByteArray();
				int length = buffer == null ? 0 : buffer.length;
				if (length > 0) {
					bitmap = BitmapFactory.decodeByteArray(buffer, 0, length);
					if (bitmap == null) {
						return bitmap;
					}
					boolean isCreate = file != null && checkParentIsCreate(file);
					if (isCreate) {
						os = new BufferedOutputStream(new FileOutputStream(file), DEFAULT_BUFFER_SIZE);
						os.write(buffer);
						os.flush();
					}
					int ret = NewsDatabase.getInstance().updateNewsPicPath(_id, file.getAbsolutePath());
					Log.v("zhang", " _id = "+_id+" --ret =  "+ret + " -- willBeCreate = "+isCreate);
				}
//				return bitmap = BitmapFactory.decodeStream(is);
			}
		} catch (ClientProtocolException e) {
			httpGet.abort();
			e.printStackTrace();
		} catch (IOException e) {
			httpGet.abort();
			e.printStackTrace();
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				os = null;
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			buffer = null;
			((AndroidHttpClient) client).close();
		}
		return bitmap;
	}

	/** 检测父文件夹是否存在,若不存在创建父目录 **/
	private boolean checkParentIsCreate(File imageFile) {//该方法是异步的 cacheDir.mkdirs()的时候有可能是并发访问，所以return的时候要cacheDir.exists()
		File cacheDir = imageFile.getParentFile();
		if (cacheDir == null || (!cacheDir.exists() && !cacheDir.mkdirs())) {
			return cacheDir.exists();
		}
		return true;
	}

	/**
	 * Animates {@link ImageView} with "fade-in" effect
	 * 
	 * @param imageView {@link ImageView} which display image in
	 * @param durationMillis The length of the animation in milliseconds
	 */
	public static void fdeInAnimationBitmap(ImageView imageView, int durationMillis) {
		AlphaAnimation fadeImage = new AlphaAnimation(0, 1);
		fadeImage.setDuration(durationMillis);
		fadeImage.setInterpolator(new DecelerateInterpolator());
		imageView.startAnimation(fadeImage);
	}

}
