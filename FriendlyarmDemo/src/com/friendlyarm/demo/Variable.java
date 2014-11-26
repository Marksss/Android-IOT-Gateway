package com.friendlyarm.demo;

public class Variable {
	public static final int SOCKET_CONNECT = 1;
	public static final int SOCKET_DISCONNECT = 2;
	public static final int PING_CONNECT =3;
	public static final int REFLESH_TEXT = 4;
	// editEnable:检查是否正在更改ip和port的标识(等同于editIP.isEnable())
	public static boolean editEnable = false;
	// socketConnected:socket连接状态，连接(true)，断开(false)
	public static boolean socketConnected = false;
	public static String host = null;
	public static int port = 0;
}
