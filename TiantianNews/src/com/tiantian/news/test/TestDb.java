package com.tiantian.news.test;

import java.util.ArrayList;

import com.tiantian.news.bean.NewsItem;
import com.tiantian.news.db.NewsDatabase;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class TestDb extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ArrayList<NewsItem> items = NewsDatabase.getInstance().selectByChannel(0, null, null);
		NewsItem item = NewsDatabase.getInstance().selectLatestByChannel(0, null);
		Log.v("zhan", item.toString());
		int size = items.size();
		for (int i = 0; i < size; i++) {
//			Log.v("zhang", i+ " is equals ? "+items.get(i).equals(item));
			NewsItem n = items.get(i);
			NewsItem newItem = NewsDatabase.getInstance().selectByArgs(n.channelId, n.link);
			Log.v("zhang", i+ " newItem is empty ? "+(newItem == null));
		}
	}

}
