package org.urbanstew.SoundCloudDroid;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class SoundCloudData extends ContentProvider {

	@Override
	public boolean onCreate()
	{
		// Access the database.
		mOpenHelper = new DatabaseHelper(getContext());
		
		return true;
	}
		

	enum Uploads { _ID, TITLE, PATH }
	
	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		DatabaseHelper(Context context)
		{
			super(context, "soundcloud_droid.db", null, 9);
			mContext = context;
		}

		public void onCreate(SQLiteDatabase db)
		{
			createUploadedFilesTable(db);
		}
				
		void createUploadedFilesTable(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + DB.Uploads.TABLE_NAME + "("
					+ DB.Uploads._ID + " INTEGER PRIMARY KEY,"
					+ DB.Uploads.TITLE + " TEXT,"
					+ DB.Uploads.PATH + " TEXT,"
					+ DB.Uploads.STATUS + " TEXT"
					+ ");");
		}
		
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            Log.w(this.getClass().getName(), "Upgrading database from version " + oldVersion + " to " + newVersion);

            db.execSQL("DROP TABLE IF EXISTS " + DB.Uploads.TABLE_NAME);
            onCreate(db);
        }
		
		Context mContext;
	}
	
    private static final int UPLOADS = 1;
    private static final int UPLOAD_ID = 2;

    private static final UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(DB.AUTHORITY, "uploads", UPLOADS);
        sUriMatcher.addURI(DB.AUTHORITY, "uploads/#", UPLOAD_ID);
    }

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case UPLOADS:
        	Cursor c = db.query(DB.Uploads.TABLE_NAME, new String[] {DB.Uploads._ID}, selection, selectionArgs, null, null, null);
        	count = 0;
        	for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
        		count += delete(ContentUris.withAppendedId(DB.Uploads.CONTENT_URI, c.getLong(0)), null, null);
            break;

        case UPLOAD_ID:
            String uploadId = uri.getPathSegments().get(1);
            count = db.delete(DB.Uploads.TABLE_NAME, DB.Uploads._ID + "=" + uploadId
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}
	
	@Override
	public String getType(Uri uri)
	{
        switch (sUriMatcher.match(uri)) {
        case UPLOADS:
            return DB.Uploads.CONTENT_TYPE;

        case UPLOAD_ID:
            return DB.Uploads.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues)
	{
        ContentValues values;
        if (initialValues != null)
            values = new ContentValues(initialValues);
        else
            values = new ContentValues();
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
        long rowId=0;
        Uri contentURI;

		switch(sUriMatcher.match(uri))
		{
			case UPLOADS:
			{
		        rowId = db.insert(DB.Uploads.TABLE_NAME, DB.Uploads.TITLE, values);
		        contentURI = DB.Uploads.CONTENT_URI;
		        break;
			}
			default:
		        throw new IllegalArgumentException("Unknown URI " + uri);
		}
        if (rowId >= 0)
        {
            Uri noteUri = ContentUris.withAppendedId(contentURI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
        case UPLOADS:
            qb.setTables(DB.Uploads.TABLE_NAME);
//            qb.setProjectionMap(sUploadsProjectionMap);
            break;

        case UPLOAD_ID:
            qb.setTables(DB.Uploads.TABLE_NAME);
//            qb.setProjectionMap(sUploadsProjectionMap);
            qb.appendWhere(DB.Uploads._ID + "=" + uri.getPathSegments().get(1));
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = DB.Uploads.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
	}

	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs)
	{
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        if(uri.getPathSegments().size()>1)
        	selection = BaseColumns._ID + "=" + uri.getPathSegments().get(1)
        		+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
        
        switch (sUriMatcher.match(uri)) {
        case UPLOAD_ID:
        	count = db.update(DB.Uploads.TABLE_NAME, values, selection, selectionArgs);
        	break;
        	
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}
	
	private DatabaseHelper mOpenHelper;
}
