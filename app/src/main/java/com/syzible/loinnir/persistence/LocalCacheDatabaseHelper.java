package com.syzible.loinnir.persistence;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONObject;

/**
 * Created by ed on 02/09/2017.
 */

public class LocalCacheDatabaseHelper extends SQLiteOpenHelper {
    private Context context;
    private static final int dbVersion = 1;

    private static final String CREATE_CACHE_TABLE_QUERY =
            "CREATE TABLE " + LocalCacheDatabase.Columns.TABLE_NAME + "(" +
                    LocalCacheDatabase.Columns._ID + " INTEGER PRIMARY KEY," +
                    LocalCacheDatabase.Columns.TIME_SUBMITTED + " INTEGER," +
                    LocalCacheDatabase.Columns.RECIPIENT + " TEXT," +
                    LocalCacheDatabase.Columns.MESSAGE_CONTENT + " TEXT," +
                    LocalCacheDatabase.Columns.IS_LOCALITY + " BOOLEAN);";

    private static final String DELETE_CACHE_TABLE_QUERY =
            "DROP TABLE IF EXISTS " + LocalCacheDatabase.Columns.TABLE_NAME + ";";

    private LocalCacheDatabaseHelper(Context context) {
        super(context, LocalCacheDatabase.DATABASE_NAME, null, dbVersion);
        this.context = context;
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

    public static void insertCachedItem(Context context, LocalCacheDatabase.CachedItem cachedItem) {

    }

    public static JSONObject getCachedItem(Context context, String id) {
        return null;
    }

    public static void removeCachedItem(Context context, String id) {

    }

    public static boolean shouldQueryCachedItems(Context context) {
        LocalCacheDatabaseHelper db = new LocalCacheDatabaseHelper(context);
        SQLiteDatabase readDb = db.getReadableDatabase();
        String tableName = LocalCacheDatabase.Columns.TABLE_NAME;

        String query = "SELECT * FROM (" + tableName + ")";
        Cursor cursor = readDb.rawQuery(query, null);
        boolean shouldQuery = cursor.getCount() > 0;

        readDb.close();
        cursor.close();

        return shouldQuery;
    }

    public static void printCachedItemsContents(Context context) {
        LocalCacheDatabaseHelper db = new LocalCacheDatabaseHelper(context);
        SQLiteDatabase readDb = db.getReadableDatabase();
        String tableName = LocalCacheDatabase.Columns.TABLE_NAME;

        String q = "SELECT * FROM (" + tableName + ")";
        Cursor c = readDb.rawQuery(q, null);
        c.moveToFirst();

        int rowNum = c.getCount();
        int colNum = c.getColumnCount();

        System.out.println("Number of rows in " + tableName + " is " + rowNum);
        System.out.println("Number of columns in each row is " + colNum);

        for (int r = 0; r < rowNum; r++) {
            for (int col = 0; col < colNum; col++) {
                System.out.print(col + ". " + c.getString(col) + "; ");
            }
            System.out.print("\n");
            c.moveToNext();
        }

        c.close();
        readDb.close();
    }
}
