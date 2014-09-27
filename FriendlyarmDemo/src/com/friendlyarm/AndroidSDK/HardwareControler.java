package com.friendlyarm.AndroidSDK;
import android.util.Log;

public class HardwareControler
{
	/* File */
	static public native int open(String devName, int flags);
	static public native int write(int fd, byte[] data);
	static public native int read(int fd, byte[] buf, int len);
	static public native int select(int fd, int sec, int usec);
	static public native void close(int fd);
    static public native int ioctlWithIntValue(int fd, int cmd, int value);
    
	/* Serial Port */
	static public native int openSerialPort( String devName, long baud, int dataBits, int stopBits );
    static public native int openSerialPortEx( String devName
    		, long baud
    		, int dataBits
    		, int stopBits
    		, String parityBit
    		, String flowCtrl
    		);
	
    static {
        try {
        	System.loadLibrary("friendlyarm-hardware");
        } catch (UnsatisfiedLinkError e) {
            Log.d("HardwareControler", "libfriendlyarm-hardware library not found!");
        }
    }
}
