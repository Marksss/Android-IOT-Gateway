package com.friendlyarm.thread;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.friendlyarm.AndroidSDK.HardwareControler;
import com.friendlyarm.demo.MainActivity;
import com.friendlyarm.demo.Variable;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

/**
 * @author SXL 用于从串口接收并解析数据的线程
 * 
 */
public class DataRevThread extends Thread {

	private static final String TAG = "DataRevThread";
	private SimpleDateFormat df = null;
	private DataSendThread dataSendThread = null;
	private DataStoreThread dataStoreThread = null;

	public DataRevThread(DataSendThread dst1,
			DataStoreThread dst2) {
		this.dataSendThread = dst1;
		this.dataStoreThread = dst2;
		df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 日期格式
	}

	private final String devName = "/dev/s3c2410_serial3";
	private final int speed = 9600, dataBits = 8, stopBits = 1;
	private final int BUFSIZE = 512;

	@Override
	public void run() {
		// TODO Auto-generated method stub
		byte[] buf = new byte[BUFSIZE];
		int retSize = 0, devfd = -1;
		String str = null; // 用于接收串口数据
		StringBuilder frameData = new StringBuilder(100 * 20); // 用于数据解析发送
		Message message = new Message();

		try {
			while (true) {
				// 如果devfd = -1就说明串口连接失败
				devfd = HardwareControler.openSerialPort(devName, speed,
						dataBits, stopBits);
				Log.i(TAG, "SerialPort opened");

				while (devfd != -1) {
					Thread.sleep(200);

					if (HardwareControler.select(devfd, 0, 0) == 1) {
						retSize = HardwareControler.read(devfd, buf, BUFSIZE);
						if (retSize > 0) {
							str = new String(buf, 0, retSize);

							if (Variable.isVisible) {
								// 发送str和message.what到主线程
								message = Message.obtain();
								Bundle bundle = new Bundle();
								bundle.putString("str", str);
								message.setData(bundle);
								message.what = Variable.TEXT_REFLESH;
								MainActivity.handler.sendMessage(message);
							}
							
							if (!Variable.editEnable) {
								// 若button已按下，则解析数据并传递到相应的线程中
								frameData.append(str);
								frameData = analyzeData(frameData);
							}
						}
					}
				}

				// 休眠1s后重连
				Thread.sleep(1000);
			}
		} catch (StringIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// 线程interrupt后关闭串口连接，自行退出
			if (devfd != -1) {
				HardwareControler.close(devfd);
			}
			Log.i(TAG, "SerialPort closed!!!");
		}
	}

	/**
	 * 数据解析的过程
	 * 
	 * @param tempData
	 * @return 返回frameData
	 */
	private StringBuilder analyzeData(StringBuilder tempData) {
		while (tempData.length() > 9) {
			if (tempData.charAt(0) == '$') {
				int dataLength = (tempData.charAt(7) - 48) * 10
						+ tempData.charAt(8) - 48;

				if (dataLength < 0 || dataLength > 20) {
					// data数据长度异常，丢弃前一部分数据
					tempData.delete(0, 9);
				} else {
					if (dataLength > tempData.length() - 9) {
						// data数据不完整，留到下一次循环再处理
						break;
					} else {
						// 帧完整，则发送到DataSendThread或DataStoreThread
						if (Variable.isSocketConnected) {
							dataSendThread.offerQueue(tempData.substring(0,
									9 + dataLength) + df.format(new Date()));
						} else {
							dataStoreThread.offerQueue(tempData.substring(0,
									9 + dataLength) + df.format(new Date()));
						}

						tempData.delete(0, dataLength + 9);
					}
				}
			} else {
				// tempData.charAt(0)不是'$'，则删除
				tempData.deleteCharAt(0);
			}
		}

		return tempData;
	}
}
