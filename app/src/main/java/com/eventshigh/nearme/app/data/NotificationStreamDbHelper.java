package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NotificationStreamDbHelper extends SQLiteOpenHelper {
  public static final String TABLE_NAME = "stream";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_TIMESTAMP = "timestamp";
  public static final String COLUMN_TYPE = "type";
  public static final String COLUMN_BLOB = "blob";

  private static final String DATABASE_NAME = "stream.db";
  private static final int DATABASE_VERSION = 1;

  private static final String DATABASE_CREATE = "create table "
      + TABLE_NAME + "("
      + COLUMN_ID + " integer primary key autoincrement, "
      + COLUMN_TIMESTAMP + " integer not null, "
      + COLUMN_TYPE + " integer not null, "
      + COLUMN_BLOB + " blob);";

  public NotificationStreamDbHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(DATABASE_CREATE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // Nothing to upgrade yet
  }
}
