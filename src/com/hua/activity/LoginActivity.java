package com.hua.activity;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.hua.constants.Constants;
import com.hua.constants.PreferenceConstants;
import com.hua.service.ActionService;
import com.hua.service.IConnectionStatusCallback;
import com.hua.util.DialogUtil;
import com.hua.util.L;
import com.hua.util.LogUtils2;
import com.hua.util.PreferenceUtils;
import com.hua.util.T;

public class LoginActivity extends FragmentActivity implements IConnectionStatusCallback,TextWatcher{

	
	public static final String LOGIN_ACTION = "com.hua.action.LOGIN";
	private static final int LOGIN_OUT_TIME = 0;
	private Button mLoginBtn;
	/**账户名*/
	private EditText mAccountEt;
	/**密码*/
	private EditText mPasswordEt;
	/**记住密码*/
	private CheckBox mAutoSavePasswordCK;
	/**隐身登录*/
	private CheckBox mHideLoginCK;
	/**使用TLS加密*/
	private CheckBox mUseTlsCK;
	/**
	 * 静音模式CheckBox
	 */
	private CheckBox mSilenceLoginCK;
	private ActionService mActionService;
	/**登陆中提示框*/
	private Dialog mLoginDialog;
	/**登录超时处理线程*/
	private ConnectionOutTimeProcess mLoginOutTimeProcess;
	private String mAccount;
	private String mPassword;
	/**提示page上面的关于登录帐号的温馨提示*/
	private View mTipsViewRoot;
	/**提示page上的 上滑可关闭页面*/
	private TextView mTipsTextView;
	/**一开始提示的page*/
	private Animation mTipsAnimation;
	
	
	private String tempUserName = "admin@172.20.171.1";
	private String tempPassword = "huyue52099";

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case LOGIN_OUT_TIME:
				if (mLoginOutTimeProcess != null
						&& mLoginOutTimeProcess.running)
					mLoginOutTimeProcess.stop();
				if (mLoginDialog != null && mLoginDialog.isShowing())
					mLoginDialog.dismiss();
				T.showShort(LoginActivity.this, R.string.timeout_try_again);
				break;

			default:
				break;
			}
		}

	};
	
	/**用来坚挺服务器的连接 */
	ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mActionService.unRegisterConnectionStatusCallback();
			mActionService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LogUtils2.i("************onServiceConnected = ");
			mActionService = ((ActionService.ActionBinder)service).getService();
			///
			mActionService.registerConnectionStatusCallback(LoginActivity.this);
		}
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loginpage);
		//开始服务
		//第一步很简单，当用户启动该应用时，即启动本应用关健服务，并与界面Activity完成绑定，同时完成xmpp的参数配置    /在SmackImpl中
		startService(new Intent(LoginActivity.this,ActionService.class));
		BindXMPPService();
		initView() ;

	}

	/**初始化组件*/
	private void initView() {
		mTipsAnimation = AnimationUtils.loadAnimation(this, R.anim.connection);
		mAutoSavePasswordCK = (CheckBox) findViewById(R.id.auto_save_password);
		mHideLoginCK = (CheckBox) findViewById(R.id.hide_login);
		mSilenceLoginCK = (CheckBox) findViewById(R.id.silence_login);
		mUseTlsCK = (CheckBox) findViewById(R.id.use_tls);
		mTipsViewRoot = findViewById(R.id.login_help_view);
		mTipsTextView = (TextView) findViewById(R.id.pulldoor_close_tips);
		mAccountEt = (EditText) findViewById(R.id.account_input);
		mAccountEt.setText(tempUserName);
		mPasswordEt = (EditText) findViewById(R.id.password);
		mPasswordEt.setText(tempPassword);
		
		mLoginBtn = (Button) findViewById(R.id.login);
		String account = PreferenceUtils.getPrefString(this,
				PreferenceConstants.ACCOUNT, "");
		String password = PreferenceUtils.getPrefString(this,
				PreferenceConstants.PASSWORD, "");
		if (!TextUtils.isEmpty(account))
			mAccountEt.setText(account);
		if (!TextUtils.isEmpty(password))
			mPasswordEt.setText(password);
		mAccountEt.addTextChangedListener(this);
		mLoginDialog = DialogUtil.getLoginDialog(this);
		mLoginOutTimeProcess = new ConnectionOutTimeProcess();
	}
	
	/**点击登录*/
	public void onLoginClick(View v) {
		mAccount = mAccountEt.getText().toString();
		mAccount = splitAndSaveServer(mAccount);
		mPassword = mPasswordEt.getText().toString();
		if (TextUtils.isEmpty(mAccount)) {
			T.showShort(this, R.string.null_account_prompt);
			return;
		}
		if (TextUtils.isEmpty(mPassword)) {
			T.showShort(this, R.string.password_input_prompt);
			return;
		}
		if (mLoginOutTimeProcess != null && !mLoginOutTimeProcess.running)
			mLoginOutTimeProcess.start();
		if (mLoginDialog != null && !mLoginDialog.isShowing())
			mLoginDialog.show();
		if (mActionService != null) {
			LogUtils2.i("************start loginng....");
			mActionService.Login(mAccount, mPassword);
		}
	}
	
	private void BindXMPPService() {
		L.i(LoginActivity.class, "[SERVICE] Unbind");
		Intent mServiceIntent = new Intent(LoginActivity.this, ActionService.class);
		mServiceIntent.setAction(LOGIN_ACTION);
//		mServiceIntent.addc
//		mServiceIntent.setd
		bindService(mServiceIntent, mServiceConnection,
				Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
	}
	
	private String splitAndSaveServer(String account) {
		LogUtils2.i("***********account == "+account); 
		if (!account.contains(Constants.SERVERSPLITCODE)){
			LogUtils2.e("*****errror");
			return account;
		}
		String customServer = PreferenceUtils.getPrefString(this,
				PreferenceConstants.CUSTOM_SERVER, "");
		String[] res = account.split(Constants.SERVERSPLITCODE);
		String userName = res[0];
		String server = res[1];
		LogUtils2.i("username = "+userName);
		LogUtils2.i("server = "+server);
		
		if (Constants.TESTCONNECT_IP.equals(server) || Constants.LOCALHOST.equals(server) || Constants.TESTCONNECT_IP2.equals(server) 
				|| PreferenceConstants.GMAIL_SERVER.equals(customServer)) {
			// work around for gmail's incompatible jabber implementation:
			// send the whole JID as the login, connect to talk.google.com
//			userName = account;
			userName = res[0];
			PreferenceUtils.setPrefString(mActionService, PreferenceConstants.Server, server);

		}
		
		PreferenceUtils.setPrefString(this, PreferenceConstants.Server, server);
		LogUtils2.i("********username == "+userName); 
		return userName;
	}
	
	
	//IConnectionStatusCallback 服务器的监听接口 对各种返回信息做对应操作
	@Override
	public void connectionStatusChanged(int connectedState, String reason) {
		
				if (mLoginDialog != null && mLoginDialog.isShowing() && connectedState == ActionService.CONNECTED){
					LogUtils2.i("*******will close the dialog*******");
					mLoginDialog.dismiss();
				}
				if (mLoginOutTimeProcess != null && mLoginOutTimeProcess.running) {
					mLoginOutTimeProcess.stop();
					mLoginOutTimeProcess = null;
				}
				if (connectedState == ActionService.CONNECTED) {
					save2Preferences();
					T.showLong(LoginActivity.this, "^_^");
//					startActivity(new Intent(this, MainActivity.class));
//					finish();
				} else if (connectedState == ActionService.DISCONNECTED){
					T.showLong(LoginActivity.this, getString(R.string.request_failed)
							+ reason);
				}
	}

	/**保存上次登录时user选择的操作（例如保存password）*/
	private void save2Preferences() {
		boolean isAutoSavePassword = mAutoSavePasswordCK.isChecked();
		boolean isUseTls = mUseTlsCK.isChecked();
		boolean isSilenceLogin = mSilenceLoginCK.isChecked();
		boolean isHideLogin = mHideLoginCK.isChecked();
		PreferenceUtils.setPrefString(this, PreferenceConstants.ACCOUNT,
				mAccount);// 帐号是一直保存的
		if (isAutoSavePassword)
			PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD,
					mPassword);
		else
			PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD,
					"");

		PreferenceUtils.setPrefBoolean(this, PreferenceConstants.REQUIRE_TLS,
				isUseTls);
		PreferenceUtils.setPrefBoolean(this, PreferenceConstants.SCLIENTNOTIFY,
				isSilenceLogin);
		if (isHideLogin)
			PreferenceUtils.setPrefString(this,
					PreferenceConstants.STATUS_MODE, PreferenceConstants.XA);
		else
			PreferenceUtils.setPrefString(this,
					PreferenceConstants.STATUS_MODE,
					PreferenceConstants.AVAILABLE);
	}
	
	
	////////////////////////////////start 
	//TextWatcher 的回调接口
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}

	//	TextWatcher 的回调接口
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		
	}

//	TextWatcher 的回调接口
	@Override
	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub
		
	}
	
	////////////////////////////////over
	
	/**登录超时处理线程*/
		class ConnectionOutTimeProcess implements Runnable {
			public boolean running = false;
			private long startTime = 0L;
			private Thread thread = null;

			ConnectionOutTimeProcess() {
			}

			public void run() {
				while (true) {
					if (!this.running){
						return;
					}
					if (System.currentTimeMillis() - this.startTime > 20 * 1000L) {
						mHandler.sendEmptyMessage(LOGIN_OUT_TIME);
					}
					try {
						Thread.sleep(10L);
					} catch (Exception localException) {
					}
				}
			}

			public void start() {
				try {
					this.thread = new Thread(this);
					this.running = true;
					this.startTime = System.currentTimeMillis();
					this.thread.start();
				} finally {
				}
			}

			public void stop() {
				try {
					this.running = false;
					this.thread = null;
					this.startTime = 0L;
				} finally {
				}
			}
		}
	
	
	
}
