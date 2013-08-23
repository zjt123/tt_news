package com.tiantian.news;

import com.tiantian.news.db.NewsDatabase;
import com.tiantian.news.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 
 * @author zhangjiantian
 *
 */
public class BrowserActivity extends Activity implements OnClickListener {
    public final static int ICS_VERSION_R2 = 15;

	private final String fTag = "BrowserActivity";

	public static final String ACTION_URL = "com.tiantian.news.BrowserActivity.URL_IMG";
	public static final String ACTION_NEW_ITEM_ID = "com.tiantian.news.BrowserActivity.ACTION_NEW_ITEM_ID";
	public static final String ACTION_TITLE = "com.tiantian.news.BrowserActivity.ACTION_TITLE";
	public static final String ACTION_RESULT = "com.tiantian.news.BrowserActivity.ACTION_RESULT";

	private WebView mWebView;
	private ProgressBar mViewProgress;
	private String mHomeURL;
	private long _id;
	private int mResult = -1;

	private View mBackButton;
	private TextView mTitle;
	private ImageView mHome;
	private ImageView mBack;
	private ImageView mFroward;
	private ImageView mRefresh;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browser);

		mBackButton = findViewById(R.id.imageview_left);
		mBackButton.setVisibility(View.VISIBLE);
		mBackButton.setOnClickListener(this);
		mTitle = (TextView) findViewById(R.id.textview_title);
		mViewProgress = (ProgressBar) findViewById(R.id.webview_progressbar);

		mWebView = (WebView) findViewById(R.id.webview_browser);
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.removeAllViews();
				view.loadUrl(url);
				return true;
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				mViewProgress.setVisibility(View.VISIBLE);
				mFroward.setEnabled(view.canGoForward());
				mBack.setEnabled(view.canGoBack());
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				mViewProgress.setVisibility(View.GONE);
				mFroward.setEnabled(view.canGoForward());
				mBack.setEnabled(view.canGoBack());
			}
		});

		mWebView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int progress) {
				mViewProgress.setProgress(progress);
				super.onProgressChanged(view, progress);
			}

			@Override
			public void onReceivedTitle(WebView view, String title) {
				//设置当前activity的标题栏
				if(TextUtils.isEmpty(getIntent().getExtras().getString(ACTION_TITLE)) && title != null) {
//					BrowserActivity.this.setTitle(title);
					if (mTitle != null) {
						mTitle.setText(title);
					}
				}
				super.onReceivedTitle(view, title);
				if (_id > 0) {
					mResult = NewsDatabase.getInstance().updateReadStatus(_id);
				}
			}

		});

		mWebView.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
				//TODO download
				Log.v(fTag, "onDownloadStart  ============= url = "+url);
			}
		});

		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setBlockNetworkImage(false);
		webSettings.setUseWideViewPort(true);
		webSettings.setLoadWithOverviewMode(true);

		mHome = (ImageView) findViewById(R.id.imgview_browser_home);
		mBack = (ImageView) findViewById(R.id.imgview_browser_back);
		mFroward = (ImageView) findViewById(R.id.imgview_browser_forward);
		mRefresh = (ImageView) findViewById(R.id.imgview_browser_refresh);
		mHome.setOnClickListener(this);
		mBack.setOnClickListener(this);
		mFroward.setOnClickListener(this);
		mRefresh.setOnClickListener(this);

		Bundle bundle = getIntent().getExtras();
		String url = bundle.getString(ACTION_URL);
		_id = bundle.getLong(ACTION_NEW_ITEM_ID, -1);
		mHomeURL = url;
		mWebView.loadUrl(url);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		    case R.id.imageview_left: {
		    	onBackPressed();
		    }
			break;
			case R.id.imgview_browser_home: {
				if(mHomeURL != null) {
					mWebView.loadUrl(mHomeURL);
				}
			}
			break;
			case R.id.imgview_browser_refresh: {
				mWebView.reload();
			}
			break;
			case R.id.imgview_browser_forward: {
				mWebView.goForward();
			}
			break;
			case R.id.imgview_browser_back: {
				mWebView.goBack();
			}
			break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mWebView != null) {
			WebView w = mWebView;
			w.removeAllViews();
			w.destroy();
			w = null;
			mWebView = null;
		}
		int myPid = android.os.Process.myPid();
		Log.d(fTag, "exit.myPid = " + myPid);
		android.os.Process.killProcess(myPid);
	}
	
	@Override
	public void finish() {
		if (_id > 0) {
			setResult(mResult, getIntent().putExtra(ACTION_RESULT, mResult));
		}
		super.finish();
	}

}