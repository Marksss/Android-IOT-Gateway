package com.friendlyarm.demo;

import java.io.DataOutputStream;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		setMAC(new String("00:00:FF:FF:00:01"));
		
		Intent intentBoot = new Intent(context, MainActivity.class);
        intentBoot.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intentBoot);
	}

	/**
	 * @param mac 设置mac地址
	 */
	private void setMAC(String mac) {
		Process process;
		try {
			process = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(process.getOutputStream());
			os.writeBytes("/system/busybox/sbin/ifconfig eth0 down\n");
			os.writeBytes("/system/busybox/sbin/ifconfig eth0 hw ether " + mac + "\n");
			os.writeBytes("/system/busybox/sbin/ifconfig eth0 up\n");
			os.writeBytes("exit\n");
			os.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
