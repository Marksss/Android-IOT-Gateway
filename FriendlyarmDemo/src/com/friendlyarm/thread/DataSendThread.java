package com.friendlyarm.thread;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.friendlyarm.demo.GWMain;
import com.friendlyarm.demo.Variable;

import android.os.Message;
import android.util.Log;

/**
 * @author SXL 用于发送线程的数据
 * 
 */
public class DataSendThread extends Thread {
	private static final String TAG = "DataSendThread";
	private boolean timeChecked;// 系统时间是否已经更改
	private Queue<String> sendQueue = null;// 暂时储存用于发送的数据

	public DataSendThread() {
		sendQueue = new ConcurrentLinkedQueue<String>();
		timeChecked = false;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Message message = new Message();
		Socket client = null;
		InputStream input = null;
		PrintWriter print = null;

		while (true) {
			try {
				if (Variable.editEnable || !Variable.socketConnected) {
					// 如果button未按下，正在更改ip和port，则循环运行sleep(500)
					Thread.sleep(500);
					continue;
				}

				message = Message.obtain();
				message.what = Variable.SOCKET_CONNECTING;
				GWMain.handler.sendMessage(message);
				Log.i(TAG, "Ready to connect");

				// 连接服务器
				client = new Socket();
				SocketAddress address = new InetSocketAddress(Variable.host, Variable.port);
				client.connect(address, 1000);

				message = Message.obtain();
				message.what = Variable.SOCKET_CONNECT;
				GWMain.handler.sendMessage(message);

				// 更改系统时间
				input = client.getInputStream();
				if (!timeChecked) {
					if (setSystemTime(input)) {
						Log.i(TAG, "System time changed");
						timeChecked = true;
					}
				}
				print = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(client.getOutputStream())), true);

				Log.i(TAG, "Server connected");
				int i = 0;

				while (true) {
					if (Variable.editEnable || !Variable.socketConnected) {
						// “button未按下”和“socket突然断开” 二者中满足其中一个就重新循环
						client.close();
						print.close();
						input.close();

						Log.i(TAG, "Restart Socket");
						break;
					}

					// 发送数据
					while (!sendQueue.isEmpty()) {
						sendData(sendQueue.poll(), print);
					}

					// 接收关闭socket标识
					if((i++) >= 20){
						Variable.socketConnected = !isServerSocetClosed(input);
						i = 0;
					}
					
					Thread.sleep(200);
				}
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				if (getClient(client)) {
					e.printStackTrace();
				} else {
					Log.i(TAG, Variable.host + ":" + Variable.port + "---Read time out");
					try {
						// 如果暂时无法连接服务器，则每隔2s重连一次
						Thread.sleep(3000);
					} catch (InterruptedException e2) {
						Log.i(TAG, "Socket closed!!!");
						break;
					}
				}
			} catch (InterruptedException e) {
				// 线程interrupt后自行退出，关闭socket连接
				try {
					if (getClient(client)) {
						client.close();
					}
					if (print != null) {
						print.close();
					}
					if (input != null) {
						input.close();
					}
				} catch (IOException e2) {
					e2.printStackTrace();
				}
				Log.i(TAG, "Socket closed!!!");
				break;
			}
		}
	}

	/**
	 * @param input
	 * @return 判断服务器端socket是否连接
	 * @throws IOException
	 */
	private boolean isServerSocetClosed(InputStream input) throws IOException {
		if(input.available() > 0){
			byte[] buf = new byte[1024];
			int length = input.read(buf);
			if(length > 5){
				String strClose = new String(buf, 0, 5);
				if (strClose.equals("close")) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param input
	 * @return 设置系统时间
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private boolean setSystemTime(InputStream input) throws IOException,
			InterruptedException {
		int i = 0;
		while (input.available() < 1 && !Variable.editEnable && (i++) < 6) {
			//检测到数据，重新更改ip和port，或者超过3s，则跳出循环，进入发送过程
			Thread.sleep(500);
		}

		if (input.available() > 0) {
			byte[] buf = new byte[1024];
			if (input.read(buf) >= 15) {
				String time = new String(buf, 0, 15);
				if (time.substring(0, 2).equals("20")) {
					Process process = Runtime.getRuntime().exec("su");
					DataOutputStream os = new DataOutputStream(
							process.getOutputStream());
					os.writeBytes("setprop persist.sys.timezone GMT\n");
					os.writeBytes("/system/bin/date -s " + time + "\n");
					os.writeBytes("clock -w\n");
					os.writeBytes("exit\n");
					os.flush();
					process.waitFor();
					
					os.close();
					process.destroy();
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 用于发送从队列中取出的数据
	 * 
	 * @param tempData
	 */
	private void sendData(String tempData, PrintWriter pout) {
		int dataLength = (tempData.charAt(7) - 48) * 10 + tempData.charAt(8)
				- 48;

		pout.print("POST NodeID=" + tempData.substring(1, 6) + ",NodeType="
				+ tempData.charAt(6) + ",Data="
				+ tempData.substring(9, 9 + dataLength) + ",UploadDate="
				+ tempData.substring(9 + dataLength, tempData.length())
				+ "\r\n");
		pout.flush();
	}

	/**
	 * @return socket是否连接
	 */
	public boolean getClient(Socket soc) {
		if (soc != null) {
			return soc.isConnected();
		} else {
			return false;
		}
	}

	/**
	 * 在DataRevThread和DataStoreThread中将数据插入到队列中
	 * 
	 * @param str
	 */
	public void offerQueue(String str) {
		sendQueue.offer(str);
	}
}
