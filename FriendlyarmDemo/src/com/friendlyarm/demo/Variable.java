package com.friendlyarm.demo;

public class Variable {
	public static final int SOCKET_CONNECT = 1;
	public static final int SOCKET_DISCONNECT = 2;
	public static final int SOCKET_CONNECTING = 3;
	public static final int PING_CONNECT =4;
	public static final int TEXT_REFLESH = 5;
	public static final String MAC = "00:00:FF:FF:00:01";
	public static boolean editEnable = false;
	// editEnable:检查是否正在更改ip和port的标识(等同于editIP.isEnable())
	public static boolean isSocketConnected = false;
	// isSocketConnected:服务器连接状态，连接(true)，断开(false)
	public static boolean isVisible = true;
	public static String host = null;
	public static int port = 0;
	public static boolean sysBoot = false;
}
