package com.friendlyarm.thread;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.friendlyarm.AndroidSDK.FetchDevice;
import com.friendlyarm.AndroidSDK.SerialPort;
import com.friendlyarm.demo.GWMain;
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

	@Override
	public void run() {
		// TODO Auto-generated method stub
		FetchDevice device;
		String str = null; // 用于接收串口数据
		StringBuilder frameData = new StringBuilder(100 * 20); // 用于数据解析发送
		Message message = new Message();
		
		device = new SerialPort();
		Log.i(TAG, "Start to Fetch the data");

		try {
			while (true) {
				boolean deviceOpen = device.open();
				while (deviceOpen) {
					Thread.sleep(200);
					
					if (device.isDataReady()) {
						str = device.readData();
						if (Variable.isVisible) {
							// 发送str和message.what到主线程
							message = Message.obtain();
							Bundle bundle = new Bundle();
							bundle.putString("str", str);
							message.setData(bundle);
							message.what = Variable.TEXT_REFLESH;
							GWMain.handler.sendMessage(message);
						}
						
						if (!Variable.editEnable) {
							// 若button已按下，则解析数据并传递到相应的线程中
							frameData.append(str);
							frameData = analyzeData(frameData);
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
			device.close();
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
						if (Variable.socketConnected) {
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
