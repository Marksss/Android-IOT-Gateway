package com.friendlyarm.AndroidSDK;

import android.util.Log;

public class SerialPort implements FetchDevice{
	private static final String TAG = "SerialPort";
	private final String devName = "/dev/s3c2410_serial3";
	private final int speed = 9600, dataBits = 8, stopBits = 1, BUFSIZE = 512;
	private int devfd = 0;
	private byte[] buf = new byte[BUFSIZE];
	
	public boolean open(){
		// 如果devfd = -1就说明串口连接失败
		devfd = HardwareControler.openSerialPort(devName, speed,
				dataBits, stopBits);
		boolean state = (devfd != -1);
		Log.i(TAG, "SerialPort open:" + state);
		return state;
	}
	
	public boolean isDataReady(){
		return (HardwareControler.select(devfd, 0, 0) == 1);
	}
	
	public String readData(){
		int retSize = HardwareControler.read(devfd, buf, BUFSIZE);
		return (new String(buf, 0, retSize));
	}
	
	public void close(){
		if (devfd != -1) {
			HardwareControler.close(devfd);
		}
		Log.i(TAG, "SerialPort closed!!!");
	}
}
