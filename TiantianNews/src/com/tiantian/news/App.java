package com.tiantian.news;

import com.tiantian.news.db.NewsDatabase;
import com.tiantian.news.util.FileUtils;
import com.tiantian.news.util.ImageLoader;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

/**
 * 
 * @author zhangjiantian
 *
 */
public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		//TODO check UIProcess
		FileUtils.mAppPackageName = getPackageName();
		NewsDatabase.getInstance(getApplicationContext());
		ImageLoader.getInstance().setLruCacheSize(
				((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass());
	}

}
