package com.friendlyarm.demo;

import java.io.DataOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.friendlyarm.AndroidSDK.HardwareControler;
import com.friendlyarm.demo.R;
import com.friendlyarm.thread.NetStatusThread;
import com.friendlyarm.thread.DataRevThread;
import com.friendlyarm.thread.DataSendThread;
import com.friendlyarm.thread.DataStoreThread;

import android.os.Handler;
import android.os.Message;

/**
 * @author SXL 主程序
 * 
 */
public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";
	private static TextView dataView = null, socketConnect = null;
	private EditText editIP = null, editPORT = null;
	private Button button = null;
	private CheckBox checkBox = null;

	private DataSendThread dataSendThread = null;
	private DataRevThread dataRevThread = null;
	private DataStoreThread dataStoreThread = null;
	private NetStatusThread connStatusThread = null;
	private static final int MAXLINES = 12; // UI界面显示的行数

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		HardwareControler.setLedState(2, 1);
		if (Variable.sysBoot) {
			setEthernet(Variable.MAC, 5555);
		}
		
		iniWidgets();
		iniThreads();
	}

	private void iniWidgets() {
		socketConnect = (TextView) findViewById(R.id.textview1);
		dataView = (TextView) findViewById(R.id.textview2);
		editIP = (EditText) findViewById(R.id.editip);
		editPORT = (EditText) findViewById(R.id.editport);
		
		button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	BtnSocket(editIP.isEnabled());
		    }
		});
		
		checkBox = (CheckBox)this.findViewById(R.id.checkBox1);
		checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){ 
            @Override 
            public void onCheckedChanged(CompoundButton buttonView, 
                    boolean isChecked) { 
                // TODO Auto-generated method stub 
                if(isChecked){
                	Variable.isVisible = true;
                }else{
                	Variable.isVisible = false;
                	dataView.setText("");
                } 
            } 
        }); 
	}

	private void iniThreads() {
		Variable.host = this.getString(R.string.defaultIP);
		Variable.port = Integer.parseInt(this.getString(R.string.defaultPORT));

		dataSendThread = new DataSendThread();
		dataStoreThread = new DataStoreThread(getApplicationContext(),
				dataSendThread);
		dataRevThread = new DataRevThread(dataSendThread, dataStoreThread);
		connStatusThread = new NetStatusThread();
		dataRevThread.start();// 开启数据接收线程
		dataSendThread.start();// 开启数据发送线程
		dataStoreThread.start();// 开启本地数据存储线程
		connStatusThread.start();// 开启网络监控线程
	}

	/**
	 * @param mac 设置mac地址,打开网络调试
	 */
	private static void setEthernet(String mac, int port) {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes("ifconfig eth0 down\n");
			os.writeBytes("ifconfig eth0 hw ether " + mac + "\n");
			os.writeBytes("ifconfig eth0 up\n");
			os.writeBytes("stop adbd\n");
			os.writeBytes("setprop service.adb.tcp.port " + port + "\n");
			os.writeBytes("start adbd\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if(os != null){
					os.close();
				}
				process.destroy();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void BtnSocket(boolean socketConn) {
		if (socketConn) {
			Variable.host = editIP.getText().toString();
			String strPort = editPORT.getText().toString();
			if ("".equals(strPort)) {
				strPort = "0";
			}
			Variable.port = Integer.parseInt(strPort);

			editIP.setEnabled(false);
			editPORT.setEnabled(false);
			button.setText("断开重连");

			Variable.editEnable = false;

		} else {
			editIP.setEnabled(true);
			editPORT.setEnabled(true);
			button.setText("连接");
			if (Variable.isSocketConnected) {
				socketConnect.setText("ip connect,   socket disconnect");
				socketConnect.setTextColor(android.graphics.Color.YELLOW);
			}
			// 重启数据发送线程，重新连接服务器
			Variable.editEnable = true;
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
			case Variable.SOCKET_CONNECTING:
				socketConnect.setText("connecting...!");
				socketConnect.setTextColor(android.graphics.Color.BLUE);
				break;
			case Variable.PING_CONNECT:
				socketConnect.setText("ip connect---socket disconnect");
				socketConnect.setTextColor(android.graphics.Color.YELLOW);
				break;
			case Variable.TEXT_REFLESH:
				if (Variable.isVisible) {
					if (dataView.getLineCount() >= MAXLINES) {
						dataView.setText(msg.getData().getString("str"));
					} else {
						dataView.append(msg.getData().getString("str"));
					}
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
