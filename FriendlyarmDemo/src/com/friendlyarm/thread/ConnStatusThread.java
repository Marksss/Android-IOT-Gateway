package com.friendlyarm.thread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
	private boolean socketConnected, queueOverflow;

	public ConnStatusThread(DataSendThread dataSendThread,
			DataRevThread dataRevThread, DataStoreThread dataStoreThread) {
		this.dataSendThread = dataSendThread;
		this.dataRevThread = dataRevThread;
		this.dataStoreThread = dataStoreThread;
		socketConnected = false;
		queueOverflow = false;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			while (true) {
				//ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);   
				
				if (dataSendThread.getServer()) {
					InetAddress inet = InetAddress.getByName(host);

					if (inet.isReachable(1000)) {
						setSocketConn(true);
					} else {
						setSocketConn(false);
					}
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
				Log.i(TAG, "Server is available now");
			} else {
				message.what = SOCKET_RECONNECT;
				Log.i(TAG, "Server is not available now");
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