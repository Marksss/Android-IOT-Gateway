package com.friendlyarm.demo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.friendlyarm.AndroidSDK.HardwareControler;
import com.friendlyarm.demo.R;
import com.friendlyarm.thread.ConnStatusThread;
import com.friendlyarm.thread.DataRevThread;
import com.friendlyarm.thread.DataSendThread;
import com.friendlyarm.thread.DataStoreThread;

import android.os.Handler;
import android.os.Message;

/**
 * @author SXL 主程序
 * 
 */
public class MainActivity extends Activity implements OnClickListener {

	private static final String TAG = "MainActivity";
	private static TextView dataView = null, socketConnect = null;
	private EditText editIP1 = null, editIP2 = null, editIP3 = null,
			editIP4 = null, editPORT = null;
	private Button button = null;

	private DataSendThread dataSendThread = null;
	private DataRevThread dataRevThread = null;
	private DataStoreThread dataStoreThread = null;
	private ConnStatusThread connStatusThread = null;
	private static final int MAXLINES = 12; // UI界面显示的行数

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		socketConnect = (TextView) findViewById(R.id.textview1);
		dataView = (TextView) findViewById(R.id.textview2);
		editIP1 = (EditText) findViewById(R.id.editip1);
		editIP2 = (EditText) findViewById(R.id.editip2);
		editIP3 = (EditText) findViewById(R.id.editip3);
		editIP4 = (EditText) findViewById(R.id.editip4);
		editPORT = (EditText) findViewById(R.id.editport);
		button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(this);

		Variable.host = this.getString(R.string.defaultIP1) + "."
				+ this.getString(R.string.defaultIP2) + "."
				+ this.getString(R.string.defaultIP3) + "."
				+ this.getString(R.string.defaultIP4);
		Variable.port = Integer.parseInt(this.getString(R.string.defaultPORT));

		dataSendThread = new DataSendThread();
		dataStoreThread = new DataStoreThread(getApplicationContext(),
				dataSendThread);
		dataRevThread = new DataRevThread(dataSendThread, dataStoreThread);
		connStatusThread = new ConnStatusThread();
		dataRevThread.start();// 开启数据接收线程
		dataSendThread.start();// 开启数据发送线程
		dataStoreThread.start();// 开启本地数据存储线程
		connStatusThread.start();// 开启网络监控线程

		HardwareControler.setLedState(2, 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 * 监听button（连接或断开按钮），重新连接，更改IP和端口
	 */
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.button1) {
			// TODO Auto-generated method stub
			if (editIP1.isEnabled()) {
				Variable.host = editIP1.getText().toString() + "."
						+ editIP2.getText().toString() + "."
						+ editIP3.getText().toString() + "."
						+ editIP4.getText().toString();

				String strPort = editPORT.getText().toString();
				if ("".equals(strPort)) {
					strPort = "0";
				}
				Variable.port = Integer.parseInt(strPort);

				editIP1.setEnabled(false);
				editIP2.setEnabled(false);
				editIP3.setEnabled(false);
				editIP4.setEnabled(false);
				editPORT.setEnabled(false);
				button.setText("断开重连");

				Variable.editEnable = false;

			} else {
				editIP1.setEnabled(true);
				editIP2.setEnabled(true);
				editIP3.setEnabled(true);
				editIP4.setEnabled(true);
				editPORT.setEnabled(true);
				button.setText("连接");
				socketConnect.setText("socket is not connected!");
				socketConnect.setTextColor(android.graphics.Color.RED);

				// 重启数据发送线程，重新连接服务器
				Variable.editEnable = true;
			}
		}
	}

	public static Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case Variable.SOCKET_CONNECT:
				socketConnect.setText("socket is connected!");
				socketConnect.setTextColor(android.graphics.Color.GREEN);
				break;
			case Variable.SOCKET_DISCONNECT:
				socketConnect.setText("socket is not connected!");
				socketConnect.setTextColor(android.graphics.Color.RED);
				break;
			case Variable.PING_CONNECT:
				socketConnect.setText("ip connect,socket disconnect");
				socketConnect.setTextColor(android.graphics.Color.YELLOW);
				break;
			case Variable.REFLESH_TEXT:
				if (dataView.getLineCount() >= MAXLINES) {
					dataView.setText(msg.getData().getString("str"));
				} else {
					dataView.append(msg.getData().getString("str"));
				}
				break;
			}
		}
	};

	@Override
	public void onDestroy() {
		HardwareControler.setLedState(0, 0);
		HardwareControler.setLedState(2, 0);

		// 同时中断4个线程，然后自行终止
		dataSendThread.interrupt();
		dataRevThread.interrupt();
		dataStoreThread.interrupt();
		connStatusThread.interrupt();

		Log.i(TAG, "MainActivity closed!!!");
		super.onDestroy();
	}
}
