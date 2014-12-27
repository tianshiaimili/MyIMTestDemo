package com.hua.fragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hua.activity.ChatActivity;
import com.hua.activity.FragmentCallBack;
import com.hua.activity.R;
import com.hua.adapter.RecentChatAdapter;
import com.hua.db.ChatProvider;
import com.hua.db.ChatProvider.ChatConstants;
import com.hua.service.ActionService;
import com.hua.swipelistview.BaseSwipeListViewListener;
import com.hua.swipelistview.SwipeListView;
import com.hua.util.L;
import com.hua.util.XMPPHelper;
import com.hua.view.AddRosterItemDialog;

/**
 * 最近回话记录..
 * @author zero
 *
 */
public class RecentChatFragment extends Fragment implements OnClickListener {

	private Handler mainHandler = new Handler();
	private ContentObserver mChatObserver = new ChatObserver();
	private ContentResolver mContentResolver;
	private SwipeListView mSwipeListView;
	private RecentChatAdapter mRecentChatAdapter;
	private TextView mTitleView;
	private ImageView mTitleAddView;
	private FragmentCallBack mFragmentCallBack;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mFragmentCallBack = (FragmentCallBack) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnHeadlineSelectedListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContentResolver = getActivity().getContentResolver();
		mRecentChatAdapter = new RecentChatAdapter(getActivity());
	}

	@Override
	public void onResume() {
		super.onResume();
		mRecentChatAdapter.requery();
		mContentResolver.registerContentObserver(ChatProvider.CONTENT_URI,
				true, mChatObserver);
	}

	@Override
	public void onPause() {
		super.onPause();
		mContentResolver.unregisterContentObserver(mChatObserver);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater
				.inflate(R.layout.recent_chat_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initView(view);
	}

	private void initView(View view) {
		mTitleView = (TextView) view.findViewById(R.id.ivTitleName);
		mTitleView.setText(R.string.recent_chat_fragment_title);
		mTitleAddView = (ImageView) view
				.findViewById(R.id.ivTitleBtnRightImage);
		mTitleAddView.setImageResource(R.drawable.setting_add_account_white);
		mTitleAddView.setVisibility(View.VISIBLE);
		mTitleAddView.setOnClickListener(this);
		mSwipeListView = (SwipeListView) view
				.findViewById(R.id.recent_listview);
		mSwipeListView.setEmptyView(view.findViewById(R.id.recent_empty));
		mSwipeListView.setAdapter(mRecentChatAdapter);
		mSwipeListView.setSwipeListViewListener(mSwipeListViewListener);

	}

	public void updateRoster() {
		mRecentChatAdapter.requery();
	}

	private class ChatObserver extends ContentObserver {
		public ChatObserver() {
			super(mainHandler);
		}

		public void onChange(boolean selfChange) {
			updateRoster();
			L.i("liweiping", "selfChange" + selfChange);
		}
	}

	BaseSwipeListViewListener mSwipeListViewListener = new BaseSwipeListViewListener() {
		@Override
		public void onClickFrontView(int position) {
			Cursor clickCursor = mRecentChatAdapter.getCursor();
			clickCursor.moveToPosition(position);
			String jid = clickCursor.getString(clickCursor
					.getColumnIndex(ChatConstants.JID));
			Uri userNameUri = Uri.parse(jid);
			Intent toChatIntent = new Intent(getActivity(), ChatActivity.class);
			toChatIntent.setData(userNameUri);
			toChatIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME,
					XMPPHelper.splitJidAndServer(jid));
			startActivity(toChatIntent);
		}

		@Override
		public void onClickBackView(int position) {
			mSwipeListView.closeOpenedItems();// 关闭打开的项
		}
	};

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ivTitleBtnRightImage:
			ActionService xxService = mFragmentCallBack.getService();
			if (xxService == null || !xxService.isAuthenticated()) {
				return;
			}
			new AddRosterItemDialog(mFragmentCallBack.getMainActivity(),
					xxService).show();// 添加联系人
			break;

		default:
			break;
		}
	}

}
