package org.urbanstew.SoundCloudDroid;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class UploadsActivity extends Activity
{
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uploads);

        // Read uploads
        Cursor cursor = getContentResolver().query(DB.Uploads.CONTENT_URI, sUploadsProjection, null, null, null);
        
        Log.w(getClass().getName(), "Read " + cursor.getCount() + " uploads.");
        
        // Map uploads to ListView
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.uploads_item, cursor,
                new String[] { DB.Uploads.TITLE, DB.Uploads.STATUS }, new int[] { android.R.id.text1, android.R.id.text2 });
        ListView list = (ListView)findViewById(R.id.uploads_list);
        list.setAdapter(adapter);
	}
	
    protected static final String[] sUploadsProjection = new String[]
	{
	      DB.Uploads._ID, // 0
	      DB.Uploads.TITLE, // 1
	      DB.Uploads.STATUS, // 2
	};
}
