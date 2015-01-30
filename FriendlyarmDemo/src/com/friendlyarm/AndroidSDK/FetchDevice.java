package com.friendlyarm.AndroidSDK;

public interface FetchDevice {
	public boolean open();
	public boolean isDataReady();
	public String readData();
	public void close();
}
