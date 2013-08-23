package com.tiantian.view;

import com.tiantian.news.R;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

public class MyListView extends ListView implements OnScrollListener {

	private float mLastY = -1; // save event y
	private Scroller mScroller;
	private OnScrollListener mScrollListener;
	// -- header view
	private MyListViewHeader mHeaderView;
	// -- footer view
	private MyEmptyViewHeader mFooterView;

	// the interface to trigger refresh and load more.
	private MyListViewListener mListViewListener;
	
	private RelativeLayout mHeaderViewContent;
	private TextView mHeaderTimeView;
	private int mHeaderViewHeight; // header view's height
	private boolean mEnablePullRefresh = true;
	private boolean mPullRefreshing = false; // is refreashing.
	private boolean mIsFooterReady = false;
	private boolean mBeforeChangeViewHeight = false;
	private boolean mCanLoadMore = false;//是否能加载更多
	private boolean mLoadMoreRefreshing = false; // 加载中
	private View mLoadMoreView;

	// total list items, used to detect is at the bottom of listview.
	private int mTotalItemCount;

	private boolean mScrollBackHeader;
	private boolean mScrollBackFooter;

	private final static int SCROLL_DURATION = 400; // scroll back duration
	private final static int PULL_LOAD_MORE_DELTA = 50; // when pull up >= 50px at bottom, trigger load more.
	private final static float OFFSET_RADIO = 1.8f; // support iOS like pull feature.
	
	public MyListView(Context context) {
		super(context);
		initWithContext(context);
	}

	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initWithContext(context);
	}

	public MyListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initWithContext(context);
	}

	private void initWithContext(Context context) {
		mScroller = new Scroller(context, new DecelerateInterpolator());
		// ListView need the scroll event, and it will dispatch the event to
		// user's listener (as a proxy).
		super.setOnScrollListener(this);

		// init header view
		mHeaderView = new MyListViewHeader(context);
		mHeaderViewContent = (RelativeLayout) mHeaderView.findViewById(R.id.listview_header_content);
		mHeaderTimeView = (TextView) mHeaderView.findViewById(R.id.listview_header_time);
		addHeaderView(mHeaderView);

		// init footer view
		mFooterView = new MyEmptyViewHeader(context);

		// init header height
		mHeaderView.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						mHeaderViewHeight = mHeaderViewContent.getHeight();
						getViewTreeObserver()
								.removeGlobalOnLayoutListener(this);
					}
				});
	}

	private void updateHeaderHeight(float delta) {
		mHeaderView.setVisiableHeight((int) delta
				+ mHeaderView.getVisiableHeight());
		if (mEnablePullRefresh && !mPullRefreshing) { // 未处于刷新状态，更新箭头
			if (mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
				mHeaderView.setState(MyListViewHeader.STATE_READY);
			} else {
				mHeaderView.setState(MyListViewHeader.STATE_NORMAL);
			}
		}
		setSelection(0); // scroll to top each time
	}

	private void updateEmptyHeight(float delta) {
		mFooterView.setVisiableHeight((int) delta + mFooterView.getVisiableHeight());
		if (mEnablePullRefresh) {
			if (mFooterView.getVisiableHeight() > 0) {
				mFooterView.setState(MyEmptyViewHeader.STATE_READY);
			} else {
				mFooterView.setState(MyEmptyViewHeader.STATE_NORMAL);
			}
		}
	}

	/**
	 * set last refresh time
	 * 
	 * @param time
	 */
	public void setRefreshTime(String time) {
		mHeaderTimeView.setText(time);
	}

	/**
	 * enable or disable pull down refresh feature.
	 * 
	 * @param enable
	 */
	public void setPullRefreshEnable(boolean enable) {
		mEnablePullRefresh = enable;
		if (!mEnablePullRefresh) { // disable, hide the content
			mHeaderViewContent.setVisibility(View.INVISIBLE);
		} else {
			mHeaderViewContent.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mLastY == -1) {
			mLastY = ev.getRawY();
		}

		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mLastY = ev.getRawY();
			break;
		case MotionEvent.ACTION_MOVE:
			final float deltaY = ev.getRawY() - mLastY;
			mLastY = ev.getRawY();
			if (!mPullRefreshing && getFirstVisiblePosition() == 0
					&& (mHeaderView.getVisiableHeight() > 0 || deltaY > 0)) {
				if (mListViewListener != null && mBeforeChangeViewHeight) {
					mBeforeChangeViewHeight = false;
					mListViewListener.onBeforeChangeHeight();
				}
				// the first item is showing, header has shown or pull down.
				updateHeaderHeight(deltaY / OFFSET_RADIO);
				invokeOnScrolling();
			} else if (getLastVisiblePosition() == mTotalItemCount - 1
					&& (mFooterView.getVisiableHeight() > 0 || deltaY < 0)) {
				if (!mLoadMoreRefreshing) {
					mScroller.abortAnimation();
					updateEmptyHeight(-deltaY / OFFSET_RADIO);
				}
			} else if(getLastVisiblePosition()  >= mTotalItemCount - getFooterViewsCount() && mFooterView.getVisiableHeight() > 0) {
				if (!mLoadMoreRefreshing) {
					resetFooterHeight();
				}
			}
			break;
		default:
			mLastY = -1; // reset
			if (getFirstVisiblePosition() == 0) {
				// invoke refresh
				if (mEnablePullRefresh
						&& mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
					startRefresh(false);
				}
				resetHeaderHeight(true);
			}
			if (mLoadMoreRefreshing) {
			} else {
				if (getLastVisiblePosition() == mTotalItemCount - 1) {
					if (mCanLoadMore/* || mFooterView.getVisiableHeight() <= 0*/) {

					} else {
						// invoke load more.
						resetFooterHeight();
					}
				} else if (mFooterView.getVisiableHeight() > 0) {
					mFooterView.setVisiableHeight(0);
				}
			}
			break;
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public void computeScroll() {
		boolean mScrollOffset = mScroller.computeScrollOffset();
		if (mScrollOffset) {
			if(mScrollBackHeader) {
				mHeaderView.setVisiableHeight(mScroller.getCurrY());
			}
			if(mScrollBackFooter && !mLoadMoreRefreshing) {
				mFooterView.setVisiableHeight(mScroller.getCurrY());
			}
			postInvalidate();
			invokeOnScrolling();
		} else {
			mScrollBackHeader = false;
			mScrollBackFooter = false;
			if (!mLoadMoreRefreshing && mFooterView.getVisiableHeight() > 0) {
				mScroller.abortAnimation();
				resetFooterHeight();
			}
		}
		super.computeScroll();
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		// make sure ListViewFooter is the last footer view, and only add once.
		if (!mIsFooterReady) {
			mIsFooterReady = true;
			addFooterView(mFooterView);
		}
		super.setAdapter(adapter);
	}

	private void invokeOnScrolling() {
		if (mScrollListener instanceof OnMyScrollListener) {
			OnMyScrollListener l = (OnMyScrollListener) mScrollListener;
			l.onMyScrolling(this);
		}
	}

	/**
	 * reset header view's height.
	 */
	private void resetHeaderHeight(boolean isTouchStop) {
		int height = mHeaderView.getVisiableHeight();
		if (height == 0) // not visible.
			return;
		mScroller.abortAnimation();
		int finalHeight = 0; // default: scroll back to dismiss header.
		if(isTouchStop) {
			// refreshing and header isn't shown fully. do nothing.
			if (mPullRefreshing && height <= mHeaderViewHeight) {
				return;
			}
			// is refreshing, just scroll back to show all the header.
			if (mPullRefreshing && height > mHeaderViewHeight) {
				finalHeight = mHeaderViewHeight;
			}
		}
		mScrollBackHeader = true;
		mScroller.startScroll(0, height, 0, finalHeight - height,
				SCROLL_DURATION);
		// trigger computeScroll
		invalidate();
	}

	private void resetFooterHeight() {
		int height = mFooterView.getVisiableHeight();
		if (height > 0) {
			mScrollBackFooter = true;
			mScroller.startScroll(0, height, 0, -height,
					SCROLL_DURATION);
			invalidate();
		}
	}
	/**
	 * start refresh
	 */
	public void startRefresh(boolean isManualControl) {
		if (mPullRefreshing) {
			return;
		}
		if (mListViewListener != null && !mPullRefreshing) {
			mPullRefreshing = true;
			mListViewListener.onRefresh();
		}
		if (mHeaderView.getState() == MyListViewHeader.STATE_REFRESHING) {
			return;
		}
		mHeaderView.setState(MyListViewHeader.STATE_REFRESHING);
		if(isManualControl) {
			mHeaderView.setVisiableHeight(100);
			resetHeaderHeight(true);
		}
	}

	/**
	 * start refresh
	 * 不调用mListViewListener.onRefresh()
	 */
	public void startRefreshNot2OnRefresh() {
		if (mHeaderView.getState() == MyListViewHeader.STATE_REFRESHING) {
			return;
		}
		mHeaderView.setState(MyListViewHeader.STATE_REFRESHING);
		mHeaderView.setVisiableHeight(100);
		resetHeaderHeight(false);
	}

	/**
	 * stop refresh, reset header view.
	 */
	public void stopRefresh() {
		resetFooterHeight();
		if (mPullRefreshing) {
			resetHeaderHeight(false);
			mPullRefreshing = false;
		}
	}

	public void setListViewListener(MyListViewListener l) {
		mListViewListener = l;
	}

	@Override
	public void setOnScrollListener(OnScrollListener l) {
		mScrollListener = l;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch (scrollState) {
		case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
			mBeforeChangeViewHeight = mHeaderView.getVisiableHeight() == 0;
			break;
		case OnScrollListener.SCROLL_STATE_IDLE:
			mBeforeChangeViewHeight = false;
			boolean isLast = getLastVisiblePosition() >= mTotalItemCount - getFooterViewsCount();
			if (mCanLoadMore && isLast) {
				if (mListViewListener != null && !mLoadMoreRefreshing) {
					mLoadMoreRefreshing = true;
					mFooterView.show(mLoadMoreView);// TODO
					setSelection(mTotalItemCount + 1);
					postDelayed(mRunnable, 800);
				}
			}
			break;
		default:
			break;
		}
		if (mScrollListener != null) {
			mScrollListener.onScrollStateChanged(view, scrollState);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		mTotalItemCount = totalItemCount;
		if (mScrollListener != null) {
			mScrollListener.onScroll(view, firstVisibleItem, visibleItemCount,
					totalItemCount);
		}
	}

	public void setCanLoadMore(boolean canLoadMore) {
		mCanLoadMore = canLoadMore;
	}

	public void setLoadMoreView(View view) {
		mLoadMoreView = view;
	}

	/**
	 * stop LoadMore, reset foot view.
	 */
	public void stopLoadMore() {
		resetFooterHeight();
		if(mLoadMoreRefreshing) {
			mLoadMoreRefreshing = false;
			mFooterView.hide();
		}
	}

	/**
	 * you can listen ListView.OnScrollListener or this one. it will invoke
	 * onXScrolling when header/footer scroll back.
	 */
	public interface OnMyScrollListener extends OnScrollListener {
		public void onMyScrolling(View view);
	}

	/**
	 * implements this interface to get refresh/load more event.
	 */
	public interface MyListViewListener {
		public void onBeforeChangeHeight();
		public void onRefresh();
		public void onLoadMore();
	}
	
	private Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			mListViewListener.onLoadMore();
		}
	};
}
