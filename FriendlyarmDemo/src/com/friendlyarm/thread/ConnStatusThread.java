package com.friendlyarm.thread;

import java.io.IOException;
import java.net.UnknownHostException;

import com.friendlyarm.AndroidSDK.HardwareControler;
import com.friendlyarm.demo.MainActivity;
import android.os.Message;
import android.util.Log;

/**
 * @author SXL 检测服务器是否实时连接
 * 
 */
public class ConnStatusThread extends Thread {
	private static final String TAG = "ConnStatusThread";
	private static final int SOCKET_CONNECT = 1, SOCKET_RECONNECT = 4;
	private String host = null;
	private DataSendThread dataSendThread = null;
	private DataRevThread dataRevThread = null;
	private DataStoreThread dataStoreThread = null;
	private boolean socketConnected;

	public ConnStatusThread(DataSendThread dataSendThread,
			DataRevThread dataRevThread, DataStoreThread dataStoreThread,
			String host) {
		this.dataSendThread = dataSendThread;
		this.dataRevThread = dataRevThread;
		this.dataStoreThread = dataStoreThread;
		socketConnected = false;
		this.host = host;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			while (true) {
				Process p = Runtime.getRuntime().exec(
						"ping -c 1 -w 100 " + host);
				int status = p.waitFor();

				if (status == 0) {
					setSocketConn(true);
				} else {
					setSocketConn(false);
					Log.i(TAG, host + ":ping failed");
				}

				Thread.sleep(3000);
			}
		} catch (InterruptedException e1) {
			Log.i(TAG, "ConnStatusThread closed!!!");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 服务器断开后或重连后设置各线程socketConnected
	 * 
	 * @param sc
	 */
	private void setSocketConn(boolean sc) {
		if (socketConnected != sc) {
			dataRevThread.setSocketConnected(sc);
			dataSendThread.setSocketConnected(sc);
			dataStoreThread.setSocketConnected(sc);
			Message message = new Message();

			if (sc) {
				message.what = SOCKET_CONNECT;
				HardwareControler.setLedState(0,1);
				Log.i(TAG, host + ":ping success");
			} else {
				message.what = SOCKET_RECONNECT;
				HardwareControler.setLedState(0,0);
			}

			MainActivity.handler.sendMessage(message);
			socketConnected = sc;
		}
	}

	/**
	 * 从MainActivity被动获取host
	 * 
	 * @param Host
	 */
	public void setHOST(String Host) {
		this.host = Host;
	}
}