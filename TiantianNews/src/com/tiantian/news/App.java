package com.tiantian.news;

import com.tiantian.news.db.NewsDatabase;
import com.tiantian.news.util.FileUtils;

import android.app.Application;

public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		//TODO check UIProcess
		FileUtils.mAppPackageName = getPackageName();
		NewsDatabase.getInstance(getApplicationContext());
	}

}
