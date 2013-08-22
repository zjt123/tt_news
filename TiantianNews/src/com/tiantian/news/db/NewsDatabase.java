package com.tiantian.news.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.tiantian.news.bean.NewsItem;
import com.tiantian.news.util.FileUtils;

/**
 * 
 * @author zhangjiantian
 *
 */
public class NewsDatabase extends SQLiteOpenHelper {
	private static final String fTag = "NewsDatabase";
	private static final String DABABASE_NAME = "News.db";
	private static final int DABABASE_VERSION = 2;
	private final String fTableName = "newsItem";
	private final String[] fColumns = {
			"_id", 
			"title", 
			"link", 
			"guid", 
			"category", 
			"pubDate", 
			"description", 
			"source", 
			"imageLink",
			"imageDownloadUrl",
			"imagePath",
			"channelId", 
			"readStatus", 
			"pubDateTime",
			"createDate",
			"land"
		};
	private final String fDatabaseCreate = 
		"CREATE TABLE " + fTableName + " (" +
			fColumns[0] + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
			fColumns[1] + " TEXT, " +
			fColumns[2] + " TEXT, " +
			fColumns[3] + " TEXT, " +
			fColumns[4] + " TEXT, " +
			fColumns[5] + " TEXT, " +
			fColumns[6] + " TEXT, " +
			fColumns[7] + " TEXT, " +
			fColumns[8] + " TEXT, " +
			fColumns[9] + " TEXT, " +
			fColumns[10] + " TEXT, " +
			fColumns[11] + " INTEGER, " +
			fColumns[12] + " INTEGER, " +
			fColumns[13] + " LONG, " +
			fColumns[14] + " LONG, " +
			fColumns[15] + " TEXT " +
		");";
	private final String fOrderby = fColumns[13] + " DESC";
	private static NewsDatabase sINSTANCE;

	public synchronized static NewsDatabase getInstance(Context context) {
		if (sINSTANCE == null) {
			sINSTANCE = new NewsDatabase(context);
		}
		return sINSTANCE;
	}

	public static NewsDatabase getInstance() {
		return sINSTANCE;
	}

	private NewsDatabase(Context context) {
		super(context, DABABASE_NAME, null, DABABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(fDatabaseCreate);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.v(fTag, "onUpgrade old = " + oldVersion + "  new = " + newVersion);
		if (oldVersion == newVersion) {
			return;
		}
		if (oldVersion ==1) {
			FileUtils.deleteOldRootDir();
			db.execSQL("DROP TABLE IF EXISTS " + fTableName);
			onCreate(db);
		}
	}

	public NewsItem selectLatestByChannel(int channelId, String land) {
		NewsItem item = null;
//		String selection = fColumns[11] + " = ? AND " + fColumns[15] + " = ?";
//		String[] selectionArgs = {Integer.toString(channelId), land};
		String selection = fColumns[11] + " = ?";
		String[] selectionArgs = {Integer.toString(channelId)};

		Cursor cursor = null;
		try {
			cursor = getReadableDatabase().query(fTableName, fColumns, selection, selectionArgs, null, null, fOrderby);
			if (cursor != null && cursor.moveToFirst()) {
				item = createNewsItem(cursor);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
		return item;
	}

	public NewsItem selectByArgs(int channelId, String links) {
		NewsItem item = null;
		String selection = fColumns[11] + " = ? AND " + fColumns[2] + " = ?";
		String[] selectionArgs = {Integer.toString(channelId), links};

		Cursor cursor = null;
		try {
			cursor = getReadableDatabase().query(fTableName, fColumns, selection, selectionArgs, null, null, fOrderby);
			if (cursor != null && cursor.moveToFirst()) {
				item = createNewsItem(cursor);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
		return item;
	}

	/**
	 * @param channelId //哪个类型
	 * @param land //语音
	 * @param limit 如："2,5"    从第2行开始，取5条数据后结束；注：行数从0 开始，传null着不限制
	 * @return
	 */
	public ArrayList<NewsItem> selectByChannel(int channelId, String land, String limit) {
		ArrayList<NewsItem> items = null;
//		String selection = fColumns[11] + " = ? AND " + fColumns[15] + " = ?";
//		String[] selectionArgs = {Integer.toString(channelId), land};
		String selection = fColumns[11] + " = ?";
		String[] selectionArgs = {Integer.toString(channelId)};

		Cursor cursor = null;
		try {
			boolean distinct = false;//是否去重
			cursor = getReadableDatabase().query(distinct, fTableName, fColumns, selection, selectionArgs, null, null, fOrderby, limit);
			if (cursor != null && cursor.moveToFirst()) {
				items = new ArrayList<NewsItem>();
				do {
					items.add(createNewsItem(cursor));
				} while (cursor.moveToNext());
			}
		} finally {
			if (cursor != null) {
				cursor.close();
				cursor = null;
			}
		}
		return items;
	}

	public long insert(NewsItem item) {
		long row = -1;
		if(item != null) {
			ContentValues cv = createContentValues(item);
			SQLiteDatabase db = getWritableDatabase();
//			try {
				row = db.insert(fTableName, null, cv);
//			} finally {
//				db.close();
//			}
		}
		return row;
	}
	
	public long[] insert(List<NewsItem> items) {
		long[] rows = null;
		if(items != null) {
			int size = items.size();
			rows = new long[size];
			SQLiteDatabase db = getWritableDatabase();
//			try {
				ContentValues cv = null;
				for (int i = 0; i < size; i++) {
					cv = createContentValues(items.get(i));
					rows[i] = db.insert(fTableName, null, cv);
				}
//			} finally {
//				db.close();
//			}
		}
		return rows;
	}

	public int delete(int id) {
		int row = -1;
		String where = fColumns[0] + " = ?";
		String[] whereValue = {Integer.toString(id)};
		Cursor cur = null;
		try{
			cur = getReadableDatabase().query(fTableName, fColumns, where, whereValue, null, null, fOrderby);
			if (cur != null && cur.moveToFirst()) {
				String path = cur.getString(cur.getColumnIndex(fColumns[10]));
				if (path != null) {
					File file = new File(path);
					if (file.exists() && file.isFile()) {
						file.delete();
					}
					path = null;
				}
			}
		} finally {
			if(cur!= null) {
				cur.close();
				cur = null;
			}
		}
		SQLiteDatabase db = getWritableDatabase();
//		try {
			row = db.delete(fTableName, where, whereValue);
//		} finally {
//			db.close();
//		}
		return row;
	}
	
	public int updateReadStatus(long id) {
		int row = -1;
		String where = fColumns[0] + " = ?";
		String[] whereValue = { id + "" };
		ContentValues cv = new ContentValues();
		cv.put(fColumns[12], 1);
		SQLiteDatabase db = getWritableDatabase();
//		try {
			row = db.update(fTableName, cv, where, whereValue);
//		} finally {
//			db.close();
//		}
		return row;
	}
	
	public int updateNewsPicPath(long _id, String path) {
		int row = -1;
		String where = fColumns[0] + " = ?";
		String[] whereValue = { _id + "" };
		ContentValues cv = new ContentValues();
		cv.put(fColumns[10], path);
		SQLiteDatabase db = getWritableDatabase();
//		try {
			row = db.update(fTableName, cv, where, whereValue);
//		} finally {
//			db.close();
//		}
		return row;
	}
	
	private NewsItem createNewsItem(Cursor cursor) {
		NewsItem item = new NewsItem();
		item._id = cursor.getInt(cursor.getColumnIndex(fColumns[0]));
		item.title = cursor.getString(cursor.getColumnIndex(fColumns[1]));
		item.link = cursor.getString(cursor.getColumnIndex(fColumns[2]));
		item.guid = cursor.getString(cursor.getColumnIndex(fColumns[3]));
		item.category = cursor.getString(cursor.getColumnIndex(fColumns[4]));
		item.pubDate = cursor.getString(cursor.getColumnIndex(fColumns[5]));
		item.description = cursor.getString(cursor.getColumnIndex(fColumns[6]));
		item.source = cursor.getString(cursor.getColumnIndex(fColumns[7]));
		item.imageLink = cursor.getString(cursor.getColumnIndex(fColumns[8]));
		item.imageDownloadUrl = cursor.getString(cursor.getColumnIndex(fColumns[9]));
		item.imagePath = cursor.getString(cursor.getColumnIndex(fColumns[10]));
		item.channelId = cursor.getInt(cursor.getColumnIndex(fColumns[11]));
		item.readStatus = cursor.getInt(cursor.getColumnIndex(fColumns[12]));
		item.pubDateTime = cursor.getLong(cursor.getColumnIndex(fColumns[13]));
		item.createDate = cursor.getLong(cursor.getColumnIndex(fColumns[14]));
//		item.land = cursor.getString(cursor.getColumnIndex(fColumns[15]));
		return item;
	}

	private ContentValues createContentValues(NewsItem item) {
		ContentValues cv = new ContentValues();
		cv.put(fColumns[1], item.title);
		cv.put(fColumns[2], item.link);
		cv.put(fColumns[3], item.guid);
		cv.put(fColumns[4], item.category);
		cv.put(fColumns[5], item.pubDate);
		cv.put(fColumns[6], item.description);
		cv.put(fColumns[7], item.source);
		cv.put(fColumns[8], item.imageLink);
		cv.put(fColumns[9], item.imageDownloadUrl);
		cv.put(fColumns[10], item.imagePath);
		cv.put(fColumns[11], item.channelId);
		cv.put(fColumns[12], 0);
		cv.put(fColumns[13], item.pubDateTime);
		cv.put(fColumns[14], item.createDate = new Date().getTime());
//		cv.put(fColumns[15], land);
		return cv;
	}
	
	public int testUpdatePath() {
		int row = -1;
		ContentValues cv = new ContentValues();
		cv.put(fColumns[10], "");
		SQLiteDatabase db = getWritableDatabase();
		row = db.update(fTableName, cv, null, null);
		return row;
	}
	
}