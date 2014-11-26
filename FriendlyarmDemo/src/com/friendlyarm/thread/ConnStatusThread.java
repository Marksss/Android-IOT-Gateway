package com.friendlyarm.thread;

import java.io.IOException;
import com.friendlyarm.AndroidSDK.HardwareControler;
import com.friendlyarm.demo.MainActivity;
import com.friendlyarm.demo.Variable;

import android.os.Message;
import android.util.Log;

/**
 * @author SXL 检测服务器是否实时连接
 * 
 */
public class ConnStatusThread extends Thread {
	private static final String TAG = "ConnStatusThread";

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Process process;
		try {
			while (true) {
				process = Runtime.getRuntime().exec(
						"ping -c 1 -w 100 " + Variable.host);
				int status = process.waitFor();

				if (status == 0) {
					setSocketConn(true);
				} else {
					setSocketConn(false);
					Log.i(TAG, Variable.host + ":ping failed");
				}
				process.destroy();
				
				Thread.sleep(3000);
			}
		} catch (InterruptedException e1) {
			Log.i(TAG, "ConnStatusThread closed!!!");
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
		if (Variable.socketConnected != sc) {
			Message message = new Message();

			if (sc) {
				message.what = Variable.PING_CONNECT;
				HardwareControler.setLedState(0,1);
				Log.i(TAG, Variable.host + ":ping success");
			} else {
				message.what = Variable.SOCKET_DISCONNECT;
				HardwareControler.setLedState(0,0);
			}

			MainActivity.handler.sendMessage(message);
			Variable.socketConnected = sc;
		}
	}
}