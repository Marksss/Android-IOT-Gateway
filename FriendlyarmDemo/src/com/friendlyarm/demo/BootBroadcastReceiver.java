package com.friendlyarm.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Intent intentBoot = new Intent(context, MainActivity.class);
        intentBoot.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intentBoot);
	}
}