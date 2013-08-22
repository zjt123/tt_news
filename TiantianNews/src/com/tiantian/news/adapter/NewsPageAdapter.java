package com.tiantian.news.adapter;

import com.tiantian.news.fragment.BaseFragment;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.View;

public class NewsPageAdapter extends FragmentPagerAdapter {
	private final String[] fChannelNames;
	private final String[] fChannelUrls;
	private final int[] fChannelIds;
	private BaseFragment[] mFragments;
	private View.OnClickListener mOnClickListener;

	public NewsPageAdapter(FragmentManager fm, String[] channelNames, String[] channelUrls, int[] ids, View.OnClickListener onClickListener) {
		super(fm);
		fChannelNames = channelNames;
		fChannelUrls = channelUrls;
		fChannelIds = ids;
		mFragments = new BaseFragment[fChannelNames.length];
		mOnClickListener = onClickListener;
	}

	@Override
	public BaseFragment getItem(int position) {
		BaseFragment fragment = mFragments[position];
		if (fragment == null) {
			fragment = BaseFragment.newInstance(position, fChannelUrls[position], fChannelIds[position], mOnClickListener);
			mFragments[position] = fragment;
		}
		return fragment;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		// return CONTENT[position % CONTENT.length].toUpperCase();
		return fChannelNames[position];
	}

	@Override
	public int getCount() {
		return fChannelNames.length;
	}
	
	public void destory() {
		if (mFragments != null) {
			int size = mFragments.length;
			for (int i = 0; i < size; i++) {
				if (mFragments[i] != null) {
					mFragments[i].onDestroy();
					mFragments[i] = null;
				}
			}
//			mFragments = null;
		}
		mOnClickListener = null;
	}
}