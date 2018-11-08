package com.jimmy.im.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
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
import android.widget.Toast;

import com.jimmy.im.client.adapte.ChatMsgViewAdapter;
import com.jimmy.im.client.data.MsgEntity;
import com.jimmy.im.client.data.TextMsgEntity;
import com.jimmy.im.client.data.VoiceMsgEntity;
import com.jimmy.im.client.media.MediaWrapper;
import com.jimmy.im.client.media.MediaPlay.OnPlayCallbackListener;
import com.jimmy.im.client.socket.MsgParam;
import com.jimmy.im.client.socket.MsgRequest;
import com.jimmy.im.client.socket.RequestQueueManager;
import com.jimmy.im.client.socket.SocketManager;
import com.jimmy.im.client.util.CommonUtil;

import de.greenrobot.event.EventBus;

/**
 * @author keshuangjie
 * @date 2014-12-1 下午7:37:35
 * @package com.jimmy.im.client
 * @version 1.0 主界面
 */
public class MainActivity extends Activity implements OnClickListener {
	private static final String TAG = MainActivity.class.getSimpleName();

	private Button mBtnSend;
	private Button mBtnRcd;
	private EditText mEditTextContent;
	private RelativeLayout mBottom;
	private ListView mListView;
	private ChatMsgViewAdapter mAdapter;
	private List<MsgEntity> mDataArrays = new ArrayList<MsgEntity>();
	private ImageView chatting_mode_btn;
	private boolean btn_vocie = false;
	private String voiceName;
	private long startVoiceT = -1, endVoiceT = -1;

	private Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			MsgEntity entity = (MsgEntity) msg.obj;
			if (entity != null) {
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
		initView();

		initData();
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
					if (TextUtils.isEmpty(voiceName)) {
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

					String recordPath = MediaWrapper.getInstance()
							.getRecordFilePath();
					if (!TextUtils.isEmpty(recordPath)) {

						Log.i(TAG, "ACTION_UP -> recordPath: " + recordPath);

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
	}

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
			// entity.type = TextMsgEntity.TYPE_TEXT;

			Message msg = mHandler.obtainMessage();
			msg.obj = entity;
			mHandler.sendMessage(msg);

			sendMsg(entity);

			mEditTextContent.setText("");
		}
	}

	/**
	 * 发送消息
	 * 
	 * @param entity
	 */
	private void sendMsg(MsgEntity entity) {
		MsgParam param = new MsgParam();
		param.setMsgEntity(entity);
		MsgRequest request = new MsgRequest(param,
				new MsgRequest.SendCallback() {

					public void onFinish() {

					}

					public void onError() {

					}
				});

		RequestQueueManager.getInstance().push(request);
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
		case R.id.setting:
			Intent intent = new Intent(this, SettingActivity.class);
			startActivity(intent);
			return true;
		case R.id.connect:
			connectServer();
			return true;

		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}

	private void connectServer() {
		SocketManager.getInstance().startSocket("");
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
	 * 
	 * @param entity
	 */
	public void onEvent(MsgEntity entity) {
		if (entity == null || entity.isSelf) {
			return;
		}

		entity.userName = "Jack";
		entity.date = new Date().toString();

		Message msg = mHandler.obtainMessage();
		msg.obj = entity;
		mHandler.sendMessage(msg);
	}

}