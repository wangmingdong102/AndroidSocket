package com.jimmy.im.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jimmy.im.server.adapter.ChatMsgViewAdapter;
import com.jimmy.im.server.data.MsgEntity;
import com.jimmy.im.server.data.TextMsgEntity;
import com.jimmy.im.server.data.VoiceMsgEntity;
import com.jimmy.im.server.media.MediaPlay.OnPlayCallbackListener;
import com.jimmy.im.server.media.MediaWrapper;
import com.jimmy.im.server.socket.MsgParam;
import com.jimmy.im.server.socket.MsgRequest;
import com.jimmy.im.server.socket.RequestQueueManager;
import com.jimmy.im.server.socket.SocketServerManager;
import com.jimmy.im.server.util.ApMgr;
import com.jimmy.im.server.util.CommonUtil;
import com.jimmy.im.server.util.WifiMgr;

import de.greenrobot.event.EventBus;

import static android.net.wifi.WifiManager.WIFI_STATE_DISABLING;
import static android.net.wifi.WifiManager.WIFI_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLING;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_UNKNOWN;
/**
 * @author keshuangjie
 * @date 2014-12-1 下午7:42:59
 * @package com.jimmy.im.server
 * @version 1.0
 * 主界面
 */
public class MainActivity extends Activity implements OnClickListener {
	private static final String TAG = MainActivity.class.getSimpleName();

	private Button mBtnSend;
	private Button mBtnRcd;
	private Button mWifiBtn;
	private Button mWifiAPBtn;
	private EditText mEditTextContent;
	private RelativeLayout mBottom;
	private ListView mListView;
	private ChatMsgViewAdapter mAdapter;
	private List<MsgEntity> mDataArrays = new ArrayList<MsgEntity>();
	private ImageView chatting_mode_btn;
	private boolean btn_vocie = false;
	private String voiceName;
	private long startVoiceT = -1, endVoiceT = -1;

	private WifiMgr mWifiMgr;
	private String[] mWifiStatus = new String[3];

	private Handler mHandler = new Handler(){
		
		public void handleMessage(android.os.Message msg) {
			MsgEntity entity = (MsgEntity) msg.obj;
			if(entity != null){
				mDataArrays.add(entity);
				mAdapter.notifyDataSetChanged();
				mListView.setSelection(mListView.getCount() - 1);
			}
		};
		
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		mWifiStatus[0] = "wifi disalbed";
        mWifiStatus[1] = "wifi no scanned";
        mWifiStatus[2] = "wifi disconnected";
		mWifiMgr = new WifiMgr(this);

		initView();

		initData();
		
		SocketServerManager.getInstance().startConnect();

		registerBroadcast();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mBroadcastReceiver);
	}

	public void initView() {
		mListView = (ListView) findViewById(R.id.listview);
		mBtnSend = (Button) findViewById(R.id.btn_send);
		mBtnRcd = (Button) findViewById(R.id.btn_rcd);
		mBtnSend.setOnClickListener(this);
		mBottom = (RelativeLayout) findViewById(R.id.btn_bottom);
		chatting_mode_btn = (ImageView) this.findViewById(R.id.ivPopUp);
		mEditTextContent = (EditText) findViewById(R.id.et_sendmessage);

		chatting_mode_btn.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				if (btn_vocie) {
					mBtnRcd.setVisibility(View.GONE);
					mBottom.setVisibility(View.VISIBLE);
					btn_vocie = false;
					chatting_mode_btn
							.setImageResource(R.drawable.chatting_setmode_msg_btn);

				} else {
					mBtnRcd.setVisibility(View.VISIBLE);
					mBottom.setVisibility(View.GONE);
					chatting_mode_btn
							.setImageResource(R.drawable.chatting_setmode_voice_btn);
					btn_vocie = true;
				}
			}
		});
		mBtnRcd.setOnTouchListener(new OnTouchListener() {

			@SuppressLint("ClickableViewAccessibility")
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					Log.i(TAG, "onTouch() -> ACTION_DOWN");
					if (!Environment.getExternalStorageState().equals(
							Environment.MEDIA_MOUNTED)) {
						Toast.makeText(MainActivity.this, "No SDCard",
								Toast.LENGTH_LONG).show();
						return false;
					}
					startVoiceT = System.currentTimeMillis();
					voiceName = startVoiceT + "";
					MediaWrapper.getInstance().startRecord();
					break;
				case MotionEvent.ACTION_UP:
					Log.i(TAG, "onTouch() -> ACTION_UP");
					if(TextUtils.isEmpty(voiceName)){
						return false;
					}
					endVoiceT = System.currentTimeMillis();
					
					MediaWrapper.getInstance().stopRecord();
					
					int time = (int) ((endVoiceT - startVoiceT) / 1000);
					Log.i(TAG, "onTouchEvent() -> recorder time: " + time + "s");
					if (time < 1) {
						Log.i(TAG, "onTouchEvent() -> 录音时间太短");
						Toast.makeText(MainActivity.this, "录音时间太短",
								Toast.LENGTH_LONG).show();
						return false;
					}
					
					String recordPath = MediaWrapper.getInstance().getRecordFilePath();
					
					if(!TextUtils.isEmpty(recordPath)){
						
						VoiceMsgEntity entity = new VoiceMsgEntity();
						entity.date = CommonUtil.getDate();
						entity.userName = "Rose";
						entity.isSelf = true;
						entity.time = time;
						entity.filePath = recordPath;
						entity.fileName = CommonUtil.getAmrFileName(recordPath);
						
						Message msg = mHandler.obtainMessage();
						msg.obj = entity;
						mHandler.sendMessage(msg);
						
						
						sendMsg(entity);
						
						voiceName = "";
					}
					
					break;

				default:
					break;
				}
				return false;
			}

		
		});

		View serverInfo = findViewById(R.id.server_info);
		serverInfo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				showDialog();
			}
		});

		Button wifiSwitch = (Button)findViewById(R.id.wifi_switch);
		mWifiBtn = wifiSwitch;
		boolean isWifiEnabled = mWifiMgr.isWifiEnabled();
		if(isWifiEnabled){
			wifiSwitch.setText("WIFI is enabled");
		}else{
			wifiSwitch.setText("WIFI is disalbed");
		}
		wifiSwitch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				boolean isWifiEnabled = mWifiMgr.isWifiEnabled();
				if(isWifiEnabled) {
					mWifiMgr.closeWifi();
				}else{
					mWifiMgr.openWifi();
				}
			}
		});

		Button wifiAPSwitch = (Button)findViewById(R.id.wifiap_switch);
		mWifiAPBtn = wifiAPSwitch;
		boolean isWifiAPOpened = ApMgr.isApOn(this);
		if(isWifiAPOpened){
			wifiAPSwitch.setText("WIFI AP is opened");
		}else{
			wifiAPSwitch.setText("WIFI AP is closed");
		}
		wifiAPSwitch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				boolean isWifiAPOpened = ApMgr.isApOn(MainActivity.this);
				if(isWifiAPOpened){
					ApMgr.closeAp(MainActivity.this);
				}else{
					ApMgr.openAp(MainActivity.this, "spoamss2018", "20182018");
				}
			}
		});
	}


	private void registerBroadcast ()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);//监听扫描周围可用WiFi列表结果
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);//监听WiFi连接与断开
		registerReceiver(mBroadcastReceiver, filter);
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction() == WifiManager.WIFI_STATE_CHANGED_ACTION) {
				switch (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WIFI_STATE_UNKNOWN)) {
					case WIFI_STATE_DISABLED: {
						mWifiStatus[0] = "wifi disabled";
						break;
					}
					case WIFI_STATE_DISABLING: {
                        mWifiStatus[0] = "wifi disabling";
						break;
					}
					case WIFI_STATE_ENABLED: {
                        mWifiStatus[0] = "wifi enabled";
						break;
					}
					case WIFI_STATE_ENABLING: {
                        mWifiStatus[0] = "wifi enabling";
						break;
					}
					case WIFI_STATE_UNKNOWN: {
                        mWifiStatus[0] = "wifi unknown";
						break;
					}
				}
			} else if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				WifiMgr wifiMgr = new WifiMgr(context);
				List<ScanResult> scanResults = wifiMgr.getScanResults();
				if (wifiMgr.isWifiEnabled() && scanResults != null && scanResults.size() > 0) { //成功扫描
                    mWifiStatus[1] = " scan ok";
				}else{
                    mWifiStatus[1] = " scan failed";
				}
			} else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) { //网络状态改变的广播
				NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				if (info != null) {
					if (info.getState().equals(NetworkInfo.State.CONNECTED)) { //WiFi已连接
						WifiMgr wifiMgr = new WifiMgr(context);
						String connectedSSID = wifiMgr.getConnectedSSID();
                        mWifiStatus[2] = " connected: "+connectedSSID;
					} else if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) { //WiFi已断开连接
                        mWifiStatus[2] = " disconnected";
					}
				}
			}
			mWifiBtn.setText(mWifiStatus[0] + "|" + mWifiStatus[1]+ "|" + mWifiStatus[2]);
		}
	};

	private String[] msgArray = new String[] { "Rose,Rose,Where are you",
			"Jack,I am here,please", "I am coming ", "Jack, I miss you ",
			"I miss you too", "we will always together" };

	private String[] dateArray = new String[] { "2012-10-31 18:00",
			"2012-10-31 18:10", "2012-10-31 18:11", "2012-10-31 18:20",
			"2012-10-31 18:30", "2012-10-31 18:35" };
	private final static int COUNT = 6;

	public void initData() {
		for (int i = 0; i < COUNT; i++) {
			TextMsgEntity entity = new TextMsgEntity();
			entity.date = dateArray[i];
			if (i % 2 == 0) {
				entity.userName = "Jack";
				entity.isSelf = false;
			} else {
				entity.userName = "Rose";
				entity.isSelf = true;
			}

			entity.msgContent = msgArray[i];
			mDataArrays.add(entity);
		}

		mAdapter = new ChatMsgViewAdapter(this, mDataArrays,
				new ChatMsgViewAdapter.OnPlayListener() {

					public void onPlay(VoiceMsgEntity entity) {
						if (entity != null
								&& !TextUtils.isEmpty(entity.filePath)) {
							MediaWrapper.getInstance().startPlay(
									entity.filePath, callback);
						}
					}
				});
		mListView.setAdapter(mAdapter);

	}

	OnPlayCallbackListener callback = new OnPlayCallbackListener() {

		public void onError(MediaPlayer mp) {

		}

		public void onComplete(MediaPlayer mp) {

		}
	};

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_send:
			send();
			break;
		}
	}

	private void send() {
		String contString = mEditTextContent.getText().toString();
		if (contString.length() > 0) {
			TextMsgEntity entity = new TextMsgEntity();
			entity.date = CommonUtil.getDate();
			entity.userName = "Rose";
			entity.isSelf = true;
			entity.msgContent = contString;
//			entity.type = TextMsgEntity.TYPE_TEXT;
			
			Message msg = mHandler.obtainMessage();
			msg.obj = entity;
			mHandler.sendMessage(msg);
			
//			EventBus.getDefault().postSticky(entity);
//			MsgQueueManager.getInstance().push(entity);
			sendMsg(entity);

			mEditTextContent.setText("");
		}
	}
	
	/**
	 * 发送消息
	 * @param entity
	 */
	private void sendMsg(MsgEntity entity){
		MsgParam param = new MsgParam();
		param.setMsgEntity(entity);
		MsgRequest request = new MsgRequest(param, new MsgRequest.SendCallback() {
			
			public void onFinish() {
				
			}
			
			public void onError() {
				
			}
		});
		
		RequestQueueManager.getInstance().push(request);
		
//		MsgQueueManager.getInstance().push(entity);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.serverInfo:
			showDialog();
			return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}
	
	private void showDialog(){
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		View view = LayoutInflater.from(this).inflate(R.layout.dialog, null);
		TextView tv_ip = (TextView) view.findViewById(R.id.ip);
		TextView tv_port = (TextView) view.findViewById(R.id.port);
		tv_ip.setText(CommonUtil.getLocalIP());
		tv_port.setText(SocketServerManager.CONNET_PORT + "");
		dialog.setView(view);
		dialog.setTitle("服务器信息");
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}
	
	@Override
	protected void onStart() {
		super.onStart();

		EventBus.getDefault().register(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		MediaWrapper.getInstance().stopPlay();
		MediaWrapper.getInstance().stopRecord();
	}

	@Override
	protected void onStop() {
		super.onStop();

		EventBus.getDefault().unregister(this);

		MediaWrapper.getInstance().releasePlay();
		MediaWrapper.getInstance().releaseRecord();

	}
	
	/** 
	 * 接收消息事件
	 * @param entity
	 */
	public void onEvent(MsgEntity entity){
		
		Log.i(TAG, "onEvent() -> entity: " + entity);
		
		if(entity == null || entity.isSelf){
			return;
		}
		
		entity.userName = "Jack";
		entity.date = new Date().toString();
		
		Message msg = mHandler.obtainMessage();
		msg.obj = entity;
		mHandler.sendMessage(msg);
	}

}