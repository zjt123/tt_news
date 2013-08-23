package com.tiantian.news.test;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class CacheDemo {

	// 开辟8M硬缓存空间
	private final int hardCachedSize = 8 * 1024 * 1024;
	// hard cache
	private final LruCache<String, Bitmap> sHardBitmapCache = new LruCache<String, Bitmap>(
			hardCachedSize) {
		public int sizeOf(String key, Bitmap value) {
			return value.getRowBytes() * value.getHeight();
		}

		protected void entryRemoved(boolean evicted, String key,
				Bitmap oldValue, Bitmap newValue) {
			Log.v("tag", "hard cache is full , push to soft cache");
			// 硬引用缓存区满，将一个最不经常使用的oldvalue推入到软引用缓存区
			sSoftBitmapCache.put(key, new SoftReference<Bitmap>(oldValue));
		}
	};
	// 软引用
	private static final int SOFT_CACHE_CAPACITY = 40;
	private final static LinkedHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache = new LinkedHashMap<String, SoftReference<Bitmap>>(
			SOFT_CACHE_CAPACITY, 0.75f, true) {
		@Override
		public SoftReference<Bitmap> put(String key, SoftReference<Bitmap> value) {
			return super.put(key, value);
		}

		@Override
		protected boolean removeEldestEntry(
				LinkedHashMap.Entry<String,SoftReference<Bitmap>> eldest) {
			if (size() > SOFT_CACHE_CAPACITY) {
				Log.v("tag", "Soft Reference limit , purge one");
				return true;
			}
			return false;
		}
	};

	// 缓存bitmap
	public boolean putBitmap(String key, Bitmap bitmap) {
		if (bitmap != null) {
			synchronized (sHardBitmapCache) {
				sHardBitmapCache.put(key, bitmap);
			}
			return true;
		}
		return false;
	}

	// 从缓存中获取bitmap
	public Bitmap getBitmap(String key) {
		synchronized (sHardBitmapCache) {
			final Bitmap bitmap = sHardBitmapCache.get(key);
			if (bitmap != null)
				return bitmap;
		}
		// 硬引用缓存区间中读取失败，从软引用缓存区间读取
		synchronized (sSoftBitmapCache) {
			SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(key);
			if (bitmapReference != null) {
				final Bitmap bitmap2 = bitmapReference.get();
				if (bitmap2 != null)
					return bitmap2;
				else {
					Log.v("tag", "soft reference 已经被回收");
					sSoftBitmapCache.remove(key);
				}
			}
		}
		return null;
	}

}
