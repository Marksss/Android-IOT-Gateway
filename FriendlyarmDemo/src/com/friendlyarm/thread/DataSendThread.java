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
import com.friendlyarm.demo.MainActivity;
import android.os.Message;
import android.util.Log;

/**
 * @author SXL 用于发送线程的数据
 * 
 */
public class DataSendThread extends Thread {
	private static final String TAG = "DataSendThread";
	private static final int SOCKET_CONNECT = 1, SOCKET_DISCONNECT = 2;
	private String host = null;
	private int port = 0;
	private boolean editEnable;// 检查是否正在更改ip和port的标识(等同于editIP.isEnable())
	private boolean socketConnected;// socket连接状态，连接(true)，断开(false)
	private boolean timeChecked;// 系统时间是否已经更改
	private Queue<String> sendQueue = null;// 暂时储存用于发送的数据
	//private Socket server = null;

	public DataSendThread(String host, int port) {
		sendQueue = new ConcurrentLinkedQueue<String>();
		editEnable = false;
		socketConnected = false;
		timeChecked = false;
		this.host = host;
		this.port = port;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Message message = new Message();
		Socket server = null;
		InputStream input = null;
		PrintWriter pout = null;
	
		while (true) {
			try {
				/* 如果button未按下，正在更改ip和port
				 * 则循环运行sleep(500) */
				if (editEnable || !socketConnected) {
					Thread.sleep(500);
					continue;
				}
	
				Log.i(TAG, "Ready to connect");
	
				// 连接服务器
				server = new Socket();
				SocketAddress address = new InetSocketAddress(host, port);
				server.connect(address, 1000);
	
				message = Message.obtain();
				message.what = SOCKET_CONNECT;
				MainActivity.handler.sendMessage(message);
				
				// 更改系统时间
				if (!timeChecked) {
					input = server.getInputStream();
					if (setSystemTime(input)) {
						Log.i(TAG, "System time changed");
						timeChecked = true;
					}
				}
				
				pout = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(server.getOutputStream())), true);

				Log.i(TAG, "Server connected");
	
				while (true) {
					/* “button未按下”和“socket突然断开” 
					 * 二者中满足其中一个就重新循环 */
					if (editEnable || !socketConnected) {
						message = Message.obtain();
						message.what = SOCKET_DISCONNECT;
						MainActivity.handler.sendMessage(message);
	
						server.close();
						pout.close();
	
						Log.i(TAG, "Restart Socket");
						break;
					}
					
					while (!sendQueue.isEmpty()) {
						// 取出删除头元素，并发送
						sendData(sendQueue.poll(), pout);
					}

					Thread.sleep(200);
				}
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				if (getServer(server)) {
					e.printStackTrace();
				}else{
					Log.i(TAG, host + ":" + port + "---Read time out");
					try {
						// 如果暂时无法连接服务器，则每隔2s重连一次
						Thread.sleep(2000);
					} catch (InterruptedException e2) {
						// 线程interrupt后自行退出
						Log.i(TAG, "Socket closed!!!");
						break;
					}
				}
			} catch (InterruptedException e) {
				// 线程interrupt后自行退出，关闭socket连接
				try {
					if (getServer(server)) {
						server.close();
					}
					if (pout!=null) {
						pout.close();
					}
					if (input!=null) {
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

	private boolean setSystemTime(InputStream input) throws IOException, InterruptedException {
		byte[] buf = new byte[1024];
		int i = 0;
		while (input.available() < 1 && !editEnable && (i++) < 6) {
			/* 检测到数据，重新更改ip和port，或者超过3s
			 * 则跳出循环，进入发送过程 */
			Thread.sleep(500);
		}

		if (input.available() > 0) {
			if (input.read(buf) >= 15) {
				String time = new String(buf, 0, 15);
				Process process = Runtime.getRuntime().exec("su");
				DataOutputStream os = new DataOutputStream(process.getOutputStream());
				os.writeBytes("setprop persist.sys.timezone GMT\n");
				os.writeBytes("/system/bin/date -s "+time+"\n");
				os.writeBytes("clock -w\n");
				os.writeBytes("exit\n");
				os.flush();
				
				return true;
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
	public boolean getServer(Socket soc) {
		if (soc != null) {
			return soc.isConnected();
		}else{
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

	/**
	 * @return sendQueue的大小
	 */
	public int getQueueSize() {
		return sendQueue.size();
	}
	
	/**
	 * 从MainActivity被动获取button按钮的状态,检查是否需要断开重连
	 * 
	 * @param ee
	 */
	public void setEditEnable(boolean ee) {
		this.editEnable = ee;
	}

	/**
	 * 从MainActivity被动获取soeketConnected的状态, 用于检测socketConnected是否连接
	 * 
	 * @param sc
	 */
	public void setSocketConnected(boolean sc) {
		this.socketConnected = sc;
	}

	/**
	 * 从MainActivity被动获取host
	 * 
	 * @param Host
	 */
	public void setHOST(String Host) {
		this.host = Host;
	}

	/**
	 * 从MainActivity被动获取port
	 * 
	 * @param Port
	 */
	public void setPORT(int Port) {
		this.port = Port;
	}
}
