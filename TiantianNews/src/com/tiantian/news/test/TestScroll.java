package com.tiantian.news.test;

import com.tiantian.news.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class TestScroll extends Activity {
	
	Button mButton;
	View view;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = getLayoutInflater().inflate(R.layout.test_scroll_view, null);
		setContentView(view);
		mButton = (Button) findViewById(R.id.textview_title);
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		view.scrollTo(100, 100);
//		view.invalidate();
	}
}
