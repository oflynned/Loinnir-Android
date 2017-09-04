package com.syzible.loinnir.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Settings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ed on 02/09/2017.
 */

public class LocalCacheDatabaseHelper extends SQLiteOpenHelper {
    private static final int dbVersion = 1;

    private static final String CREATE_CACHE_TABLE_QUERY =
            "CREATE TABLE " + LocalCacheDatabase.Columns.TABLE_NAME + "(" +
                    LocalCacheDatabase.Columns._ID + " INTEGER PRIMARY KEY," +
                    LocalCacheDatabase.Columns.TIME_SUBMITTED + " INTEGER," +
                    LocalCacheDatabase.Columns.SENDER + " TEXT," +
                    LocalCacheDatabase.Columns.RECIPIENT + " TEXT," +
                    LocalCacheDatabase.Columns.MESSAGE_CONTENT + " TEXT," +
                    LocalCacheDatabase.Columns.IS_LOCALITY + " TEXT);";

    private static final String DELETE_CACHE_TABLE_QUERY =
            "DROP TABLE IF EXISTS " + LocalCacheDatabase.Columns.TABLE_NAME + ";";

    private LocalCacheDatabaseHelper(Context context) {
        super(context, LocalCacheDatabase.DATABASE_NAME, null, dbVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CACHE_TABLE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // if a user can upgrade versions then they already have internet
        db.execSQL(DELETE_CACHE_TABLE_QUERY);
        onCreate(db);
    }

    public static void cacheItem(Context context, LocalCacheDatabase.CachedItem cachedItem) {
        LocalCacheDatabaseHelper db = new LocalCacheDatabaseHelper(context);
        SQLiteDatabase writeDb = db.getWritableDatabase();
        String tableName = LocalCacheDatabase.Columns.TABLE_NAME;

        ContentValues contentValues = new ContentValues();
        contentValues.put(LocalCacheDatabase.Columns.TIME_SUBMITTED, System.currentTimeMillis());
        contentValues.put(LocalCacheDatabase.Columns.SENDER, cachedItem.getSender());
        contentValues.put(LocalCacheDatabase.Columns.RECIPIENT, cachedItem.getRecipient());
        contentValues.put(LocalCacheDatabase.Columns.IS_LOCALITY, cachedItem.isLocalityMessage());
        contentValues.put(LocalCacheDatabase.Columns.MESSAGE_CONTENT, cachedItem.getMessage());

        writeDb.insert(tableName, null, contentValues);
    }

    public static int getCachedMessagesSize(Context context) {
        LocalCacheDatabaseHelper db = new LocalCacheDatabaseHelper(context);
        SQLiteDatabase readDb = db.getReadableDatabase();
        String tableName = LocalCacheDatabase.Columns.TABLE_NAME;

        String query = "SELECT * FROM (" + tableName + ");";
        Cursor cursor = readDb.rawQuery(query, null);
        int count = cursor.getCount();

        cursor.close();
        readDb.close();
        db.close();

        return count;
    }

    public static JSONObject getCachedItem(Context context) {
        // get the top of the db and use it as a queue system all of equal importance
        LocalCacheDatabaseHelper db = new LocalCacheDatabaseHelper(context);
        SQLiteDatabase readDb = db.getReadableDatabase();
        String tableName = LocalCacheDatabase.Columns.TABLE_NAME;

        if (shouldQueryCachedItems(context)) {
            JSONObject o = new JSONObject();
            String query = "SELECT * FROM " + tableName +
                    " ORDER BY " + LocalCacheDatabase.Columns._ID +
                    " LIMIT 1;";
            Cursor cursor = readDb.rawQuery(query, null);

            if (cursor.getCount() > 0) {
                cursor.moveToFirst();

                String localId = cursor.getColumnName(0);
                long timeSubmitted = cursor.getLong(1);
                String sender = cursor.getString(2);
                String recipient = cursor.getString(3);
                boolean isLocality = cursor.getString(4).equals("locality");
                String messageContent = cursor.getString(5);
                JSONObject data = new JSONObject();

                try {
                    o.put("is_locality", isLocality);
                    o.put("local_id", localId);

                    data.put(isLocality ? "fb_id" : "from_id", sender);
                    data.put(isLocality ? "locality" : "to_id", recipient);
                    data.put("time", timeSubmitted);
                    data.put("message", messageContent);
                    o.put("data", data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            readDb.close();
            cursor.close();
            db.close();

            return o;
        }

        readDb.close();
        db.close();
        return null;
    }

    public static void removeCachedItem(Context context, String id) {
        LocalCacheDatabaseHelper db = new LocalCacheDatabaseHelper(context);
        SQLiteDatabase readDb = db.getReadableDatabase();
        SQLiteDatabase writeDb = db.getWritableDatabase();
        String tableName = LocalCacheDatabase.Columns.TABLE_NAME;

        if (shouldQueryCachedItems(context)) {
            String deleteQuery = "DELETE FROM " + tableName +
                    " WHERE " + LocalCacheDatabase.Columns._ID + "=" + id + ";";
            writeDb.rawQuery(deleteQuery, null);
        }

        readDb.close();
        writeDb.close();
        db.close();
    }

    private static boolean shouldQueryCachedItems(Context context) {
        LocalCacheDatabaseHelper db = new LocalCacheDatabaseHelper(context);
        SQLiteDatabase readDb = db.getReadableDatabase();
        String tableName = LocalCacheDatabase.Columns.TABLE_NAME;

        String query = "SELECT * FROM (" + tableName + ");";
        Cursor cursor = readDb.rawQuery(query, null);
        boolean shouldQuery = cursor.getCount() > 0;

        readDb.close();
        cursor.close();
        db.close();

        return shouldQuery;
    }

    public static void printCachedItemsContents(Context context) {
        LocalCacheDatabaseHelper db = new LocalCacheDatabaseHelper(context);
        SQLiteDatabase readDb = db.getReadableDatabase();
        String tableName = LocalCacheDatabase.Columns.TABLE_NAME;

        String query = "SELECT * FROM (" + tableName + ");";
        Cursor cursor = readDb.rawQuery(query, null);
        cursor.moveToFirst();

        int rowNum = cursor.getCount();
        int colNum = cursor.getColumnCount();

        System.out.println("Number of rows in " + tableName + " is " + rowNum);
        System.out.println("Number of columns in each row is " + colNum);

        for (int r = 0; r < rowNum; r++) {
            for (int col = 0; col < colNum; col++) {
                System.out.print(col + ". " + cursor.getString(col) + "; ");
            }
            System.out.print("\n");
            cursor.moveToNext();
        }

        readDb.close();
        cursor.close();
        db.close();
    }
}
