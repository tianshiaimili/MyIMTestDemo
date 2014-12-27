package com.hua.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hua.adapter.RosterAdapter;
import com.hua.broadcastReceiver.XXBroadcastReceiver;
import com.hua.broadcastReceiver.XXBroadcastReceiver.EventHandler;
import com.hua.constants.PreferenceConstants;
import com.hua.db.RosterProvider;
import com.hua.db.RosterProvider.RosterConstants;
import com.hua.fragment.RecentChatFragment;
import com.hua.iphonetreeview.IphoneTreeView;
import com.hua.pulltorefresh.PullToRefreshBase;
import com.hua.pulltorefresh.PullToRefreshBase.OnRefreshListener;
import com.hua.pulltorefresh.PullToRefreshScrollView;
import com.hua.service.ActionService;
import com.hua.service.IConnectionStatusCallback;
import com.hua.slidingmenu.BaseSlidingFragmentActivity;
import com.hua.slidingmenu.SlidingMenu;
import com.hua.util.L;
import com.hua.util.LogUtils2;
import com.hua.util.PreferenceUtils;
import com.hua.util.T;
import com.hua.util.XMPPHelper;

public class MainActivity extends BaseSlidingFragmentActivity  implements
OnClickListener, IConnectionStatusCallback, EventHandler,
FragmentCallBack {
	
	private static final int ID_CHAT = 0;
	private static final int ID_AVAILABLE = 1;
	private static final int ID_AWAY = 2;
	private static final int ID_XA = 3;
	private static final int ID_DND = 4;
	public static HashMap<String, Integer> mStatusMap;

	static {
		/**表示在线状态*/
		mStatusMap = new HashMap<String, Integer>();
		mStatusMap.put(PreferenceConstants.OFFLINE, -1);
		mStatusMap.put(PreferenceConstants.DND, R.drawable.status_shield);
		mStatusMap.put(PreferenceConstants.XA, R.drawable.status_invisible);
		mStatusMap.put(PreferenceConstants.AWAY, R.drawable.status_leave);
		mStatusMap.put(PreferenceConstants.AVAILABLE, R.drawable.status_online);
		mStatusMap.put(PreferenceConstants.CHAT, R.drawable.status_qme);
	}
	
	private Handler mainHandler = new Handler();
	private ActionService mActionService;
	private SlidingMenu mSlidingMenu;
	/**显示网路连接错误*/
	private View mNetErrorView;
	/**用户名称*/
	private TextView mTitleNameView;
	/**显示在线状态的图片*/
	private ImageView mTitleStatusView;
	private ProgressBar mTitleProgressBar;
	private PullToRefreshScrollView mPullRefreshScrollView;
	/**ExpanDableListView一个继承ListView的可伸展item的View*/
	private IphoneTreeView mIphoneTreeView;
	private RosterAdapter mRosterAdapter;
	private ContentObserver mRosterObserver = new RosterObserver();
	private int mLongPressGroupId, mLongPressChildId;

	/**绑定对服务的监听*/
	ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mActionService.unRegisterConnectionStatusCallback();
			mActionService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			
			mActionService = ((ActionService.ActionBinder) service).getService();
			mActionService.registerConnectionStatusCallback(MainActivity.this);
			// 开始连接xmpp服务器
			if (!mActionService.isAuthenticated()) {
				String usr = PreferenceUtils.getPrefString(MainActivity.this,
						PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(
						MainActivity.this, PreferenceConstants.PASSWORD, "");
				mActionService.Login(usr, password);
				// mTitleNameView.setText(R.string.login_prompt_msg);
				// setStatusImage(false);
				// mTitleProgressBar.setVisibility(View.VISIBLE);
			} else {
				mTitleNameView.setText(XMPPHelper
						.splitJidAndServer(PreferenceUtils.getPrefString(
								MainActivity.this, PreferenceConstants.ACCOUNT,
								"")));
				setStatusImage(true);
				mTitleProgressBar.setVisibility(View.GONE);
			}
			
		}
	};
	

	/**判断网络的连接 来显示User的在线状态 和连接状态*/
	private void setStatusImage(boolean isConnected) {
		if (!isConnected) {
			mTitleStatusView.setVisibility(View.GONE);
			return;
		}
		String statusMode = PreferenceUtils.getPrefString(this,
				PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
		int statusId = mStatusMap.get(statusMode);
		if (statusId == -1) {
			mTitleStatusView.setVisibility(View.GONE);
		} else {
			mTitleStatusView.setVisibility(View.VISIBLE);
			mTitleStatusView.setImageResource(statusId);
		}
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(MainActivity.this, ActionService.class));
		//
		initSlidingMenu();
		//
		setContentView(R.layout.main_center_layout);
		///
		initViews();
		//
		registerListAdapter();
		
	}
	

	/**
	 * 连续按两次返回键就退出
	 */
	private long firstTime;

	@Override
	public void onBackPressed() {
		if (System.currentTimeMillis() - firstTime < 3000) {
			finish();
		} else {
			firstTime = System.currentTimeMillis();
				T.showShort(this, R.string.press_again_backrun);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		
		
	}
	

	@Override
	protected void onPause() {
		super.onPause();
		getContentResolver().unregisterContentObserver(mRosterObserver);
		unbindXMPPService();
		XXBroadcastReceiver.mListeners.remove(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	/**接触绑定的服务Service*/
	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
			LogUtils2.i("[SERVICE] Unbind");
		} catch (IllegalArgumentException e) {
			L.e(LoginActivity.class, "Service wasn't bound!");
		}
	}
	
	/**绑定开启服务*/
	private void bindXMPPService() {
		L.i(LoginActivity.class, "[SERVICE] Unbind");
		bindService(new Intent(MainActivity.this, ActionService.class),
				mServiceConnection, Context.BIND_AUTO_CREATE
						+ Context.BIND_DEBUG_UNBIND);
	}
	
	
	public void initSlidingMenu(){
		
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		int mScreenWidth = metrics.widthPixels;
		//设置左边隐藏的layout
		setBehindContentView(R.layout.main_left_layout);
		//
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		//
		Fragment mFrag = new RecentChatFragment();
		transaction.replace(R.id.main_left_fragment, mFrag);
		transaction.commit();
		
		///customize the SlidingMenu
		mSlidingMenu = getSlidingMenu();
		//set the Slide mode
		mSlidingMenu.setMode(SlidingMenu.LEFT_RIGHT);
		//set the shadow Width of left 
		mSlidingMenu.setShadowWidth(mScreenWidth / 50);
		//set the shadow picture of left 
		mSlidingMenu.setShadowDrawable(R.drawable.shadow_left);
		//set the behind offset of the left(设置左边滑动的偏移量)
		mSlidingMenu.setBehindOffset(mScreenWidth/5);
		//设置滑动过程中behind图层的淡入淡出Animation的比例 
		mSlidingMenu.setFadeDegree(0.35f);
		//设置滑动的模式 是从边边才滑动 还是整个屏幕都可以滑动 或者不可滑动 
		mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		//// 设置右菜单阴影图片
		mSlidingMenu.setSecondaryShadowDrawable(R.drawable.shadow_right);
		//设置滑动菜单时 是否可以淡入淡出
		mSlidingMenu.setFadeEnabled(true);
		// 设置滑动时拖拽效果
		mSlidingMenu.setBehindScrollScale(0.333f);
	};
	
	public void initViews(){
		
		mNetErrorView = findViewById(R.id.net_status_bar_top);
		//设置第二个menu 这里指的是 the menu of right
		mSlidingMenu.setSecondaryMenu(R.layout.main_left_layout);
		//
		FragmentTransaction mTransaction = getSupportFragmentManager().beginTransaction();
		Fragment mRightFragment = new RecentChatFragment();
		mTransaction.replace(R.id.main_left_fragment, mRightFragment);
		mTransaction.commit();
		
		//
		
		ImageButton mLeftBtn = ((ImageButton) findViewById(R.id.show_left_fragment_btn));
		mLeftBtn.setVisibility(View.VISIBLE);
		mLeftBtn.setOnClickListener(this);
		ImageButton mRightBtn = ((ImageButton) findViewById(R.id.show_right_fragment_btn));
		mRightBtn.setVisibility(View.VISIBLE);
		mRightBtn.setOnClickListener(this);
		mTitleNameView = (TextView) findViewById(R.id.ivTitleName);
		mTitleProgressBar = (ProgressBar) findViewById(R.id.ivTitleProgress);
		mTitleStatusView = (ImageView) findViewById(R.id.ivTitleStatus);
		mTitleNameView.setText(XMPPHelper.splitJidAndServer(PreferenceUtils
				.getPrefString(this, PreferenceConstants.ACCOUNT, "")));
		mTitleNameView.setOnClickListener(this);
		
		///
		
		mPullRefreshScrollView = (PullToRefreshScrollView) findViewById(R.id.pull_refresh_scrollview);
		mPullRefreshScrollView.setOnRefreshListener(new OnRefreshListener<ScrollView>() {

			@Override
			public void onRefresh(PullToRefreshBase<ScrollView> refreshView) {
				//
				new GetDataTask().execute();
			}
		});
		
		
		///
		mIphoneTreeView = (IphoneTreeView) findViewById(R.id.iphone_tree_view);
		//添加 
//		mIphoneTreeView.addHeaderView(getLayoutInflater().
//				inflate(R.layout.contact_buddy_list_group, mIphoneTreeView, false));
		mIphoneTreeView.setHeaderView(getLayoutInflater().inflate(
				R.layout.contact_buddy_list_group, mIphoneTreeView, false));
		
		//设置adapter为null时显示的view
		mIphoneTreeView.setEmptyView(findViewById(R.id.empty));
		
		//TODO 设置长按....
		mIphoneTreeView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				return false;
			}
		});
		
		//TODO 设置点击...
		mIphoneTreeView.setOnChildClickListener(new OnChildClickListener() {
			
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				return false;
			}
		});
		
	};
	
	//initialize the adapter 
	private void registerListAdapter() {
		mRosterAdapter = new RosterAdapter(this, mIphoneTreeView,
				mPullRefreshScrollView);
		mIphoneTreeView.setAdapter(mRosterAdapter);
		mRosterAdapter.requery();
	}
	

	private static final String[] GROUPS_QUERY = new String[] {
		RosterConstants._ID, RosterConstants.GROUP, };
	
	/**获取列表组*/
	public List<String> getRosterGroups() {
		// we want all, online and offline
		List<String> list = new ArrayList<String>();
		Cursor cursor = getContentResolver().query(RosterProvider.GROUPS_URI,
				GROUPS_QUERY, null, null, RosterConstants.GROUP);
		int idx = cursor.getColumnIndex(RosterConstants.GROUP);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			list.add(cursor.getString(idx));
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}
	
	//FragmentCallBack 
	@Override
	public ActionService getService() {
		return null;
	}

	@Override
	public MainActivity getMainActivity() {
		return null;
	}
	//FragmentCallBack 
	
	//OnClickListener 
	@Override
	public void onClick(View v) {
		
	}

	
	
	//EventHandler
	@Override
	public void onNetChange() {
		
	}

	
	//IConnectionStatusCallback
	@Override
	public void connectionStatusChanged(int connectedState, String reason) {
		// TODO Auto-generated method stub
		
	}



	/**观察数据变化 做操作*/
	private class RosterObserver extends ContentObserver {
		public RosterObserver() {
			super(mainHandler);
		}

		public void onChange(boolean selfChange) {
			L.d(MainActivity.class, "RosterObserver.onChange: " + selfChange);
			if (mRosterAdapter != null)
				mainHandler.postDelayed(new Runnable() {
					public void run() {
//						updateRoster();
					}
				}, 100);
		}
	}
	
	/**下拉刷新用AsyncTask 异步获取数据*/
	private class GetDataTask extends AsyncTask<Void, Void, String[]> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// if (mPullRefreshScrollView.getState() != State.REFRESHING)
			// mPullRefreshScrollView.setState(State.REFRESHING, true);
		}

		@Override
		protected String[] doInBackground(Void... params) {
			// Simulates a background job.
			if (!isConnected()) {// 如果没有连接重新连接
				String usr = PreferenceUtils.getPrefString(MainActivity.this,
						PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(
						MainActivity.this, PreferenceConstants.PASSWORD, "");
				mActionService.Login(usr, password);
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			return null;
		}

		@Override
		protected void onPostExecute(String[] result) {
			// Do some stuff here
			// Call onRefreshComplete when the list has been refreshed.
			mRosterAdapter.requery();// 重新查询一下数据库
			mPullRefreshScrollView.onRefreshComplete();
			// mPullRefreshScrollView.getLoadingLayoutProxy().setLastUpdatedLabel(
			// "最近更新：刚刚");
			T.showShort(MainActivity.this, "刷新成功!");
			super.onPostExecute(result);
		}
	}
	

	/**判断是否登录验证成功*/
	private boolean isConnected() {
		return mActionService != null && mActionService.isAuthenticated();
	}
	
	
}
