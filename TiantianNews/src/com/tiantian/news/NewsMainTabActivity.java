package com.tiantian.news;

import com.tiantian.news.adapter.NewsPageAdapter;
import com.tiantian.news.bean.NewsItem;
import com.tiantian.news.util.ImageLoader;
import com.tiantian.view.TabPageIndicator;
import com.tiantian.news.R;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author zhangjiantian
 *
 */
public class NewsMainTabActivity extends FragmentActivity {
	
	private static final long DIFF_DEFAULT_BACK_TIME = 2000;
	
	private ViewPager mViewPager;
	private NewsPageAdapter mAdapter;
	private View mUpdateView;
	private NewsItem mNewsItem;
	private long mBackTime = -1;

	private View.OnClickListener mOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			mUpdateView = null;
			mNewsItem = null;
			switch (v.getId()) {
			case R.id.news_img://新闻图标
				String imageLink = (String) v.getTag(R.id.new_image_link_tag);
				if(TextUtils.isEmpty(imageLink)) {
					Toast.makeText(getApplicationContext(), "图标没有连接 --- ", Toast.LENGTH_SHORT).show();
				} else {
					startBrowser(imageLink, -1);
				}
				break;
			case R.id.news_context_item://新闻item
				String ling = (String) v.getTag(R.id.new_item_link_tag);
				mUpdateView = v;
				if(TextUtils.isEmpty(ling)) {
					Toast.makeText(getApplicationContext(), "item没有连接 --- ", Toast.LENGTH_SHORT).show();
				} else {
					mNewsItem = (NewsItem) v.getTag(R.id.new_item_obj);
					startBrowser(ling, mNewsItem.readStatus == 1 ? -1 : mNewsItem._id);
				}
				break;
			default:
				break;
			}
		}
	};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news_main_tabs);

        TextView title = (TextView) findViewById(R.id.textview_title);
        title.setText(getTitle());
        Resources res = getResources();
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(8);
        mAdapter = new NewsPageAdapter(getSupportFragmentManager(), res.getStringArray(R.array.channel_names), res.getStringArray(R.array.channel_urls), res.getIntArray(R.array.channel_ids), mOnClickListener);
        mViewPager.setAdapter(mAdapter);

        TabPageIndicator indicator = (TabPageIndicator)findViewById(R.id.indicator);
        indicator.setViewPager(mViewPager);
        indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
        	private int mPageIndex;
			
			@Override
			public void onPageSelected(int position) {
				Log.v("zhang", "ViewPager onPageSelected ======== position = "+position);
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
//				Log.v("zhang", "ViewPager onPageScrolled == arg0 = "+arg0+" -- arg1 = "+arg1+" -- arg2 = "+arg2);
				if (arg1 == 0 || arg2 == 0) {
					return;
				}
				int index = -1;
				if (mPageIndex == 0) {
					index = mPageIndex + 1;
				} else {
					if (mPageIndex == arg0) {
						index = mPageIndex + 1;
					} else {
						index = arg0;
					}
				}
				Log.v("zhang", "ViewPager onPageScrolled == index = "+index);
				mAdapter.getItem(index).check2UpdateListView();
			}
			
			@Override
			public void onPageScrollStateChanged(int state) {
				Log.v("zhang", "ViewPager onPageScrollStateChanged == state = "+state+" -- mPageIndex = "+mPageIndex);
				switch (state) {
					case ViewPager.SCROLL_STATE_IDLE://换页结束
						int currentIndex = mViewPager.getCurrentItem();
						Log.v("zhang", "ViewPager onPageScrollStateChanged == currentIndex "+currentIndex+"-- mPageIndex = "+mPageIndex);
						if(currentIndex !=  mPageIndex) {
							mPageIndex = currentIndex;
							mAdapter.getItem(currentIndex).changePage2Refresh(false);
						}
						break;
					case ViewPager.SCROLL_STATE_DRAGGING://换页开始
						mPageIndex = mViewPager.getCurrentItem();
						break;
					case ViewPager.SCROLL_STATE_SETTLING:
						break;
					default:
						break;
				}
				
			}
		});
    }

    @Override
    protected void onDestroy() {
    	ImageLoader.getInstance().destoryClearCache();
		if (mAdapter != null) {
			mAdapter.destory();
			mAdapter = null;
		}
		if (mViewPager != null) {
			mViewPager.destroyDrawingCache();
			mViewPager = null;
		}
		mOnClickListener = null;
    	super.onDestroy();
		int myPid = android.os.Process.myPid();
		Log.d("NewsMainTabActivity", "exit.myPid = " + myPid);
		android.os.Process.killProcess(myPid);
    }

    private void startBrowser(String url, long _id){
    	Intent intent = new Intent(this, BrowserActivity.class);
    	intent.putExtra(BrowserActivity.ACTION_URL, url);
		if (_id > 0) {
			intent.putExtra(BrowserActivity.ACTION_NEW_ITEM_ID, _id);
			startActivityForResult(intent, 0);
		} else {
			startActivity(intent);
		}
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (mUpdateView != null && data != null) {
			int result = data.getIntExtra(BrowserActivity.ACTION_RESULT, 0);
			if (result > 0) {
				mNewsItem.readStatus = 1;
				((TextView) mUpdateView.getTag(R.id.new_item_title_view)).setTextColor(0xff9C9C9C);
				mUpdateView = null;
				mNewsItem = null;
			}
		}
	}
    
    @Override
    public void onBackPressed() {
    	long nowTime = System.currentTimeMillis();
    	long diff = nowTime - mBackTime;
		if (diff >= DIFF_DEFAULT_BACK_TIME) {
			mBackTime = nowTime;
			Toast.makeText(getApplicationContext(), R.string.exit_news_str, Toast.LENGTH_SHORT).show();
		} else {
	    	super.onBackPressed();
		}
    }
}
