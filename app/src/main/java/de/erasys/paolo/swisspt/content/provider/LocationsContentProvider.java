package de.erasys.paolo.swisspt.content.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by paolo on 17.04.15.
 */
public class LocationsContentProvider extends ContentProvider {

    private static class LocationsDbHelper extends SQLiteOpenHelper {

        private static final String LOG_TAG = LocationsContentProvider.class.getSimpleName();

        private static final String DATABASE_NAME = "locationstable.db";
        private static final int DATABASE_VERSION = 1;

        // Database creation SQL statement
        private static final String DATABASE_CREATE = "create table "
                + LocationsTable.TABLE
                + "("
                + LocationsTable.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + LocationsTable.COLUMN_NAME + " TEXT UNIQUE NOT NULL"
                + ");";

        public LocationsDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        // Method is called during creation of the database
        @Override
        public void onCreate(SQLiteDatabase database) {
            Log.d(LOG_TAG, "DATABASE ON CREATE DROPPING TABLE");
            database.execSQL("drop table if exists " + LocationsTable.TABLE + ";"); // REMOVE LATER
            Log.d(LOG_TAG, "DATABASE ON CREATE RECREATING TABLE");
            database.execSQL(DATABASE_CREATE);
        }

        // Method is called during an upgrade of the database,
        // e.g. if you increase the database version
        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion,
                              int newVersion) {
            Log.w(LocationsTable.class.getName(), "Upgrading database from version "
                    + oldVersion + " to " + newVersion
                    + ".");

            // new queries put here
        }
    }

    private LocationsDbHelper dbHelper;

    // for URI Matcher
    private static final int LOCATIONS = 10;
    private static final int LOCATIONS_ID = 20;

    private static final String AUTHORITY = "de.erasys.paolo.swisspt.content";

    private static final String BASE_PATH = "locations";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH);

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/locations";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, LOCATIONS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", LOCATIONS_ID);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new LocationsDbHelper(getContext());
        return false;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Uisng SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Set the table
        queryBuilder.setTables(LocationsTable.TABLE);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case LOCATIONS:
                break;
            case LOCATIONS_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(LocationsTable.COLUMN_ID + "="
                        + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);

        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case LOCATIONS:
                id = sqlDB.replace(LocationsTable.TABLE, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case LOCATIONS:
                rowsDeleted = sqlDB.delete(LocationsTable.TABLE, selection,
                        selectionArgs);
                break;
            case LOCATIONS_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(LocationsTable.TABLE,
                            LocationsTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(LocationsTable.TABLE,
                            LocationsTable.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case LOCATIONS:
                rowsUpdated = sqlDB.update(LocationsTable.TABLE,
                        values,
                        selection,
                        selectionArgs);
                break;
            case LOCATIONS_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(LocationsTable.TABLE,
                            values,
                            LocationsTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(LocationsTable.TABLE,
                            values,
                            LocationsTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

}
