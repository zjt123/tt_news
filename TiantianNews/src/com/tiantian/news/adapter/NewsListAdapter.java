package com.tiantian.news.adapter;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.tiantian.news.bean.NewsItem;
import com.tiantian.news.util.ImageLoader;
import com.tiantian.news.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class NewsListAdapter extends BaseAdapter {

	private LayoutInflater mInflater = null;
	private ArrayList<NewsItem> mItems;
	private final int mChannelId;
	private SimpleDateFormat mDateFormat;

	private boolean mBusy = false;
	private ImageLoader mImageLoader;
	private View.OnClickListener mOnClickListener;

	public NewsListAdapter(int channelId, Context context, View.OnClickListener onClickListener) {
		mInflater = LayoutInflater.from(context);
		mChannelId = channelId;
		mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E");
		mImageLoader = ImageLoader.getInstance();
		mOnClickListener = onClickListener;
	}

	@Override
	public int getCount() {
		return mItems == null ? 0 : mItems.size();
	}

	@Override
	public Object getItem(int position) {
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHodler viewHodler = null;
		View newsContextItem = null;
		ImageView newsImg = null;
		TextView newsTitle = null;
		TextView newsSummary = null;
		TextView newsSource = null;
		TextView newsPubDateTime = null;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.news_item, null);
			viewHodler = new ViewHodler(); 
			newsContextItem = convertView.findViewById(R.id.news_context_item);
			newsImg = (ImageView) convertView.findViewById(R.id.news_img);
			newsTitle = (TextView) convertView.findViewById(R.id.news_title);
			newsSummary = (TextView) convertView.findViewById(R.id.news_summary);
			newsSource = (TextView) convertView.findViewById(R.id.news_source);
			newsPubDateTime = (TextView) convertView.findViewById(R.id.news_pub_date_time);
			viewHodler.newsContextItem = newsContextItem;
			viewHodler.newsImg = newsImg;
			viewHodler.newsTitle = newsTitle;
			viewHodler.newsSummary = newsSummary;
			viewHodler.newsSource = newsSource;
			viewHodler.newsPubDateTime = newsPubDateTime;
			convertView.setTag(viewHodler);
			newsContextItem.setOnClickListener(mOnClickListener);
			newsImg.setOnClickListener(mOnClickListener);
		} else {
			viewHodler = (ViewHodler) convertView.getTag();
			newsContextItem = viewHodler.newsContextItem;
			newsImg = viewHodler.newsImg;
			newsTitle = viewHodler.newsTitle;
			newsSummary = viewHodler.newsSummary;
			newsSource = viewHodler.newsSource;
			newsPubDateTime = viewHodler.newsPubDateTime;
		}
//		Log.v("peng", "getView --- mBusy =========== "+mBusy);
		NewsItem item = mItems.get(position);
		String imageDownloadUrl = item.imageDownloadUrl;
		newsImg.setTag(imageDownloadUrl);
		newsTitle.setText(item.title);
		newsSummary.setText(item.description);
		newsSource.setText(item.source);
		String formatDate = item.formatDate;
		if (formatDate == null) {
			formatDate = item.formatDate = mDateFormat.format(new Date(item.pubDateTime));
		}
		newsPubDateTime.setText(formatDate);
		newsContextItem.setTag(R.id.new_item_link_tag, item.link);
		newsContextItem.setTag(R.id.new_item_obj, item);
		newsContextItem.setTag(R.id.new_item_title_view, newsTitle);
		newsImg.setTag(R.id.new_image_link_tag, item.imageLink);
		if (item.readStatus == 0) {
			newsTitle.setTextColor(0xff000000);
		} else {
			newsTitle.setTextColor(0xff9C9C9C);
		}
		mImageLoader.loadImage(imageDownloadUrl, newsImg, item.imagePath, item.channelId, item._id);
		return convertView;
	}

	public void setDate(ArrayList<NewsItem> items) {
		if (items == null) {
			return;
		}
		if (mItems == null) {
			mItems = items;
		} else {
			int size = items.size();
			if (size == 0) {
				return;
			}
			mItems.addAll(0, items);
		}
	}

	public void addDate(ArrayList<NewsItem> items) {
		if (mItems == null) {
			return;
		}
		mItems.addAll(items);
	}

	private void cutDate() {
		int size = mItems == null ? 0 : mItems.size();
		if (size > NewsItem.LIMIT_SIZE) {
			for (; size > NewsItem.LIMIT_SIZE; size--) {
				mItems.remove(size - 1);
			}
		}
	}

	public ArrayList<NewsItem> getDate() {
		return mItems;
	}
	
	static class ViewHodler {
		View newsContextItem;
		ImageView newsImg;
		TextView newsTitle;
		TextView newsSummary;
		TextView newsSource;
		TextView newsPubDateTime;
	}

	public void setFlagBusy(boolean busy) {
		this.mBusy = busy;
	}

	public void destory() {
		mOnClickListener = null;
		mDateFormat = null;
		mImageLoader = null;
		mItems.clear();
		mItems = null;
	}

	public void notifyDataSetChanged(ListView listView, boolean isCut) {
		listView.smoothScrollBy(0, 0);//停止滚动
		if (isCut) {
			cutDate();
		}
		notifyDataSetChanged();
	}

	public void notifyDataSetChanged(ListView listView) {
		if (getCount() > NewsItem.LIMIT_SIZE) {
			listView.smoothScrollBy(0, 0);//停止滚动
			cutDate();
			notifyDataSetChanged();
		}
	}
}
