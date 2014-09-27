package com.friendlyarm.thread;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * @author SXL 用于存储本地数据的线程
 * 
 */
public class DataStoreThread extends Thread {
	private static final String TAG = "DataStoreThread";
	private boolean socketConnected;// socket连接状态，连接(true)，断开(false)
	private Queue<String> storeQueue = null;// 暂时储存插入到数据库中之前的数据
	private DataSendThread dataSendThread = null;
	private Context context = null;

	public DataStoreThread(Context cxt, DataSendThread dst) {
		this.dataSendThread = dst;
		this.context = cxt;
		socketConnected = true;
		storeQueue = new ConcurrentLinkedQueue<String>();
	}

	// selectCount：每次检索*条数据；insertCount：每次插入*条数据
	private final int selectCount = 100, insertCount = 100;
	// delayTime：发送延迟*ms
	private final long delayTime = 100;

	@Override
	public void run() {
		// TODO Auto-generated method stub
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(context
				.getFilesDir().getAbsolutePath() + "/test.db", null);
		// 打开或创建数据库 path:data/data/com.example.****/files
		db.execSQL("DROP TABLE IF EXISTS local_storage");
		String table = "create table local_storage(Data char[0,50]);";
		db.execSQL(table);

		Log.i(TAG, "SQLite opened");

		try {
			while (true) {
				if (socketConnected) {
					/*
					 * socket已连接 则将数据库和内存中的数据发送到DataSendThread中
					 */

					if (storeQueue.isEmpty()) {
						// 如果storeQueue中没有数据，则驻留于此
						Thread.sleep(1000);
						continue;
					}

					// 先检索读取出数据库中的数据
					while (true) {
						Cursor cursor = db.rawQuery(getSelectLine(selectCount),
								null);

						if (!cursor.moveToFirst()) {
							// 如果游标中没有数据则跳出循环
							cursor.close();
							break;
						}

						do {
							// 将从数据库中取出的数据发送到DataSendThread中
							dataSendThread.offerQueue(cursor.getString(0));
							Thread.sleep(delayTime);
						} while (cursor.moveToNext() && socketConnected);

						// 删除在数据库中的*条数据
						db.execSQL(getDeleteLine(cursor.getPosition() + 1));
						Log.i(TAG, "Clear Database:" + cursor.getPosition());
						cursor.close();
					}

					// 数据库清空后再发送内存(storeQueue)中的数据
					while (!storeQueue.isEmpty() && socketConnected) {
						dataSendThread.offerQueue(storeQueue.poll());
						Thread.sleep(delayTime);
					}
					Log.i(TAG, "Clear storeQueue");
				} else {
					/*
					 * socket未连接 则接收DataRevThread发送过来的数据
					 */

					if (storeQueue.size() >= insertCount) {
						// 手动设置开始事务
						db.beginTransaction();

						while (!storeQueue.isEmpty()) {
							// 将这*条数据插入到数据库中
							db.execSQL(getInsertLine(storeQueue.poll()));
						}

						// 设置事务标志为成功，当结束事务时就会提交事务
						db.setTransactionSuccessful();
						db.endTransaction();
						Log.i(TAG, "Insert " + insertCount);
					}

					Thread.sleep(200);
				}
			}
		} catch (InterruptedException e) {
			// 关闭数据库连接
			if (db.isOpen()) {
				db.close();
			}
			Log.i(TAG, "SQLite closed!!!");
		}
	}

	private final String SELECT = "select Data from local_storage order by rowid asc limit 0,";
	private final String DELETE = "delete from local_storage where Data in (select Data from local_storage order by rowid asc limit 0,";
	private final String INSERT = "insert into local_storage(Data) values('";

	/**
	 * @param num
	 * @return 返回查询操作的语句
	 */
	private String getSelectLine(int num) {
		return SELECT + num + ";";
	}

	/**
	 * @param num
	 * @return 返回删除操作的语句
	 */
	private String getDeleteLine(int num) {
		return DELETE + num + ");";
	}

	/**
	 * @param data
	 * @return 返回插入操作的语句
	 */
	private String getInsertLine(String data) {
		return INSERT + data + "');";
	}

	/**
	 * 在DataRevThread中将数据插入到队列中
	 * 
	 * @param str
	 */
	public void offerQueue(String str) {
		this.storeQueue.offer(str);
	}

	/**
	 * 从MainActivity被动获取soeketConnected的状态,用于检测socketConnected是否连接
	 * 
	 * @param sc
	 */
	public void setSocketConnected(boolean sc) {
		this.socketConnected = sc;
	}
}