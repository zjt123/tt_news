package com.tiantian.news.fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.tiantian.news.R;
import com.tiantian.news.adapter.NewsListAdapter;
import com.tiantian.news.bean.NewsItem;
import com.tiantian.news.db.NewsDatabase;
import com.tiantian.news.util.NetworkUtil;
import com.tiantian.news.util.RssReader;
import com.tiantian.view.MyListView;
import com.tiantian.view.MyListView.MyListViewListener;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Toast;

public class BaseFragment extends Fragment implements MyListViewListener, Runnable {
	private static final String fTag = "BaseFragment";
    private static final String KEY_CHANNELURL = "BaseFragment:ChannelUrl";
    private static final String KEY_CHANNELID = "BaseFragment:ChannelId";
    private static final String NED_LAND = "cn";
    private static final String PREFERENCES_FILE_NAME = "channel_update_times";
	private static final long DIFF_UPDATE_TIME = 30 * 1000 * 60;// 滑动界面更新间隔30分钟
	private static final long DIFF_LIST_REFRESH_UPDATE_TIME = 1000 * 60;//列表下拉更新时间间隔1分钟
    private static final int DATE_CHANGE_NOTIFY = 1;
    private static final int LIST_VIEW_TO_SHOW_REFRESH = 2;
    private static final int LIST_VIEW_TO_SHOW_UPDATE_TIME = 3;
    private static final int DATE_CHANGE_FAIL = 4;
    private static final int LIST_VIEW_TO_STOP_REFRESH = 5;

	private View mParentView;
	private MyListView mListView;
	private NewsListAdapter mAdapter;
	protected String mChannelUrl;
	protected int mChannelId;
	protected int mPosition;
	protected View.OnClickListener mOnClickListener;
	private Boolean isRefresh = false;//是否在刷新
	private Handler mHandler;
	private SharedPreferences mPreferences;
	private long mUpdateDateTime;//最近一次的更新时间
	private SimpleDateFormat mDateFormat;
	private boolean mCanLoadMore;//是否还有足够的数据加载
	private boolean mIsFristCount;//是否是第一次

    public static BaseFragment newInstance(int position, String channelUrl, int channelId, View.OnClickListener onClickListener) {
    	BaseFragment fragment = new BaseFragment();
    	fragment.mChannelUrl = channelUrl + NED_LAND;
    	fragment.mChannelId = channelId;
    	fragment.mPosition = position;
    	fragment.mOnClickListener = onClickListener;
        return fragment;
    }

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		if (mDateFormat == null) {
			mDateFormat = new SimpleDateFormat("MM月dd日");
		}
        mPreferences = getActivity().getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
		if (mHandler == null) {
			mHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case DATE_CHANGE_NOTIFY:
						stopListRefresh();
						mAdapter.notifyDataSetChanged(mListView, true);
						updateTextTime();//TODO
						break;
					case LIST_VIEW_TO_SHOW_REFRESH:
						mListView.startRefresh(true);
						break;
					case LIST_VIEW_TO_SHOW_UPDATE_TIME:
						updateTextTime();
						break;
					case DATE_CHANGE_FAIL:
						mListView.stopRefresh();
						if (getActivity() != null) {
							Toast.makeText(getActivity(), R.string.listview_header_load_fail, Toast.LENGTH_SHORT).show();
						}
						break;
					case LIST_VIEW_TO_STOP_REFRESH:
						stopListRefresh();
						mAdapter.notifyDataSetChanged(mListView);
						break;
					default:
						break;
					}
				}
			};
		}
		if ((savedInstanceState != null)) {
			if (savedInstanceState.containsKey(KEY_CHANNELURL)) {
				mChannelUrl = savedInstanceState.getString(KEY_CHANNELURL);
			}
			if (savedInstanceState.containsKey(KEY_CHANNELID)) {
				mChannelId = savedInstanceState.getInt(KEY_CHANNELID);
			}
		}
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_CHANNELURL, mChannelUrl);
		outState.putInt(KEY_CHANNELID, mChannelId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mParentView = inflater.inflate(getLayoutId(), container, false);
		mListView = (MyListView) mParentView.findViewById(R.id.news_list);
		mListView.setSelector(R.drawable.transparent);
		mListView.setDividerHeight(0);
		mListView.addFooterView(inflater.inflate(R.layout.list_foot_view, null));
		mListView.setCanLoadMore(true);
		mListView.setLoadMoreView(inflater.inflate(R.layout.load_more_view, null));
		mAdapter = new NewsListAdapter(mChannelId, getActivity(), mOnClickListener);
		return mParentView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		init(savedInstanceState);
	}

	/**step 1*/
	public int getLayoutId() {
		return R.layout.fragment_news_list;
	}

	/**step 2*/
	public void init(Bundle savedInstanceState) {
		new Thread() {
			@Override
			public void run() {
				ArrayList<NewsItem> items = mAdapter.getDate();
				if (items == null) {
					items = NewsDatabase.getInstance().selectByChannel(mChannelId, null, NewsItem.LIMIT);
					if (items == null) {
						items = new ArrayList<NewsItem>();
					}
					Log.v(fTag, " ------init - items === " + (items == null) + " ----- mChannelId = " + mChannelId);
					if (items.size() == NewsItem.LIMIT_SIZE) {
						mCanLoadMore = true;
					}
					mAdapter.setDate(items);
					mHandler.post(BaseFragment.this);
				}
				mUpdateDateTime = getUpdateDateTime();
				if (mPosition == 0) {// 1、还得时间间隔判断是否需要刷新 2、刷新的UI状态显示
					reflush2UpdateTextTime();
					mIsFristCount = true;
					reflush2UpdateListView();
					refreshNews(items, false);
				}
			}
		}.start();
	}

	@Override
	public void run() {
		mListView.setCanLoadMore(mCanLoadMore);
		mListView.setAdapter(mAdapter);
		mListView.setListViewListener(this);
//		mListView.setOnScrollListener(mScrollListener);
	}
	
	public void refreshNews(ArrayList<NewsItem> items, boolean isListRefresh) {
		boolean isTimeOver2Update = checkTimeIsOver(isListRefresh);
		if (!isTimeOver2Update) {// 是否超过时间间隔，从而更新
			if (items == null || items.size() != 0) {//这个条件的反面if (items != null && items.size() == 0) {
				mHandler.removeMessages(DATE_CHANGE_NOTIFY);
				mHandler.sendEmptyMessageDelayed(DATE_CHANGE_NOTIFY, 500);
			}
			return;
		} else {
			if(items== null) {
				reflush2UpdateTextTime();
				reflush2UpdateListView();
			}
		}
		synchronized (isRefresh) {
			if (isRefresh) {
				return;
			}
			isRefresh = true;
		}
		//访问网络处理数据
		Log.v(fTag, "refreshNews getRss --start == mChannelId = "+mChannelId);
		String result = NetworkUtil.getRss(mChannelUrl);
//		String result = NetworkUtil.getRss2(mChannelUrl);
//		Log.v(fTag, result);
		Log.v(fTag, "refreshNews getRss --end == mChannelId = " + mChannelId + " result is null ? " + (result == null));
		if (!TextUtils.isEmpty(result)) {
			if(items == null) {
				items = mAdapter.getDate();
			}
			mAdapter.setDate(RssReader.pullParseXML(items, result, mChannelId));
			setUpdateDateTime(System.currentTimeMillis());
			mHandler.removeMessages(DATE_CHANGE_NOTIFY);
			mHandler.sendEmptyMessage(DATE_CHANGE_NOTIFY);
		} else {
			mHandler.removeMessages(DATE_CHANGE_FAIL);
			mHandler.sendEmptyMessage(DATE_CHANGE_FAIL);
		}
		synchronized (isRefresh) {
			isRefresh = false;
		}
	}

	private Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			refreshNews(null, false);
		}
	};

	private Runnable mRunnableListReFresh = new Runnable() {
		@Override
		public void run() {
			refreshNews(null, true);
		}
	};

	public void changePage2Refresh(boolean isListRefresh) {
		if (isRefresh) {
			return;
		}
		if (checkTimeIsOver(isListRefresh)) {
			new Thread(isListRefresh ? mRunnableListReFresh : mRunnable).start();
		} else {
			mHandler.removeMessages(LIST_VIEW_TO_STOP_REFRESH);
			mHandler.sendEmptyMessageDelayed(LIST_VIEW_TO_STOP_REFRESH, 300);
		}
	}

	OnScrollListener mScrollListener = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			switch (scrollState) {
			case OnScrollListener.SCROLL_STATE_FLING:
				mAdapter.setFlagBusy(true);
				break;
			case OnScrollListener.SCROLL_STATE_IDLE:
				mAdapter.setFlagBusy(false);
//				mAdapter.notifyDataSetChanged();
				break;
			case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				mAdapter.setFlagBusy(false);
				break;
			default:
				break;
			}
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
//			Log.v(fTag, "onScroll -- firstVisibleItem = "+firstVisibleItem+" - visibleItemCount = "+visibleItemCount+" - totalItemCount = "+totalItemCount);
		}
	};
	
	public void onDestroy() {
//		mPreferences = null;
		super.onDestroy();
	}
	
	/** 获取channel的刷新时间 **/
	private long getUpdateDateTime() {
		return mPreferences.getLong(PREFERENCES_FILE_NAME + "_" + mChannelId, 0);
	}

	/** 设置channel的刷新时间 **/
	private void setUpdateDateTime(long nowTime) {
		if (mPreferences.edit().putLong(PREFERENCES_FILE_NAME + "_" + mChannelId, nowTime).commit()) {
			mUpdateDateTime = nowTime;
		}
	}

	@Override
	public void onBeforeChangeHeight() {
		updateTextTime();
	}

	@Override
	public void onRefresh() {
		if (mIsFristCount) {
			mIsFristCount = false;
			changePage2Refresh(mIsFristCount);
		} else {
			changePage2Refresh(true);
		}
	}

	@Override
	public void onLoadMore() {
		int size = mAdapter.getCount();
		String limit = size + "," + NewsItem.LIMIT_SIZE;
		ArrayList<NewsItem> items = NewsDatabase.getInstance().selectByChannel(mChannelId, null, limit);
		size = items == null ? 0 : items.size();
		if (size >= NewsItem.LIMIT_SIZE) {
			mCanLoadMore = true;
		} else {
			mCanLoadMore = false;
		}
		if (size != 0) {
			mAdapter.addDate(items);
			mListView.stopLoadMore();
			mAdapter.notifyDataSetChanged(mListView, false);
		} else {
			mListView.stopLoadMore();
		}
		Log.v("peng", "onLoadMore =mAdapter.getCount()== "+mAdapter.getCount());
		mListView.setCanLoadMore(mCanLoadMore);
	}

	/** 非ui线程中调用，更新ui **/
	private void reflush2UpdateListView() {
		mHandler.removeMessages(LIST_VIEW_TO_SHOW_REFRESH);
		mHandler.sendEmptyMessage(LIST_VIEW_TO_SHOW_REFRESH);
	}

	/** ui线程中检查是否要显示进度 **/
	public void check2UpdateListView() {
		if ((System.currentTimeMillis() - mUpdateDateTime) >= DIFF_UPDATE_TIME) {
			updateTextTime();
//			mListView.setSelection(0);//TODO看需求把
			mListView.startRefreshNot2OnRefresh();
		}
	}

	private void reflush2UpdateTextTime() {
		mHandler.removeMessages(LIST_VIEW_TO_SHOW_UPDATE_TIME);
		mHandler.sendEmptyMessage(LIST_VIEW_TO_SHOW_UPDATE_TIME);
	}

	private void updateTextTime() {
		boolean isAdded = isAdded();
		if (!isAdded) {//avoid java.lang.IllegalStateException: Fragment BaseFragment{44b01260} not attached to Activity
			return;
		}
		if(mUpdateDateTime == 0) {//初始化更新
			mListView.setRefreshTime(getResources().getString(R.string.listview_header_last_time));
		} else {//其他
			long diffTimeSecs = (System.currentTimeMillis() - mUpdateDateTime) / 1000;
			//1min = 60s ; 1h = 60min
			if (diffTimeSecs < 3600) {//一小时内，显示分钟
				Resources resources = getResources();
				if (resources != null) {
					mListView.setRefreshTime(resources.getString(
							R.string.listview_header_last_time_for_min,
							diffTimeSecs < 60 ? 1 : diffTimeSecs / 60));
				}
			} else {
				long diffTimeHours = diffTimeSecs / 3600;
				if(diffTimeHours < 24) {//一天内更新，显示小时
					mListView.setRefreshTime(getResources().getString( R.string.listview_header_last_time_for_hour, diffTimeHours));
				} else if(diffTimeHours == 24) {//一天更新，显示1天
					mListView.setRefreshTime(getResources().getString( R.string.listview_header_last_time_for_day, 1));
				} else {//大于24小时显示xx月xx日
					mListView.setRefreshTime(getResources().getString( R.string.listview_header_last_time_for_date, mDateFormat.format(new Date(mUpdateDateTime))));
				}
			}
		}
	}

	private boolean checkTimeIsOver(boolean isListRefresh) {
		long diffUpdateTime = isListRefresh ? DIFF_LIST_REFRESH_UPDATE_TIME : DIFF_UPDATE_TIME;
		return (System.currentTimeMillis() - mUpdateDateTime) >= diffUpdateTime;
	}

	private void stopListRefresh() {
		mListView.stopRefresh();
		mCanLoadMore = true;
		mListView.setCanLoadMore(mCanLoadMore);
		mListView.stopLoadMore();
	}
}
