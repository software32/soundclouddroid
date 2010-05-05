package org.urbanstew.soundclouddroid;

import org.apache.http.HttpResponse;
import org.urbanstew.SoundCloudBase.SoundCloudRequestClient;
import org.urbanstew.SoundCloudBase.SoundCloudApplicationBase.RequestType;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class ViewTracksActivity extends ListActivity
{
	public SoundCloudApplication getSCApplication()
	{
		return (SoundCloudApplication)getApplication();
	}
	
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
       
        setContentView(R.layout.view_tracks);
        
        // Read uploads
        mCursor = getContentResolver().query(DB.Tracks.CONTENT_URI, sTracksProjection, null, null, null);
        
        Log.w(getClass().getName(), "Read " + mCursor.getCount() + " tracks.");
        
        // Map uploads to ListView
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.tracks_item, mCursor,
                new String[] { DB.Tracks.TITLE, DB.Tracks.DURATION }, new int[] { android.R.id.text1, android.R.id.text2 });
        setListAdapter(adapter);
        getListView().setTextFilterEnabled(true);
        
        getListView().setOnCreateContextMenuListener(mCreateContextMenuListener);
        adapter.setViewBinder(new ViewBinder()
        {
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex)
			{
				TextView v = (TextView)view;
				if(v.getId() == android.R.id.text1)
					return false;
				long duration = cursor.getLong(4);
				
				String text;
				if(duration==0)
					text = "Processing (play/synchronize to check again)";
				else if(duration < 1000)
					text = duration + " ms";
				else if(duration < 60000)
					text = String.format("%.1f s", duration / 1000.0f);
				else
					text = String.format("%.1f min", duration / 60000.0f);
				
				v.setText(text);				
				return true;
			}
        });
        
        if(mCursor.getCount() == 0)
        	requestOffset(0);
        
        mPlaybackDialog = new PlaybackDialog(this);
  	}
	
	public void onPause()
	{
		mPlaybackDialog.onPause();
		super.onPause();
	}
	
	public void onResume()
	{
		super.onResume();
		mPlaybackDialog.onResume();
	}
	
	public void onDestroy()
	{
		mPlaybackDialog.onDestroy();
		mCursor.close();
		super.onDestroy();
	}
	
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
        mSynchronizeMenuItem = menu.add("Synchronize").setIcon(android.R.drawable.ic_popup_sync);
        mHelpMenuItem = menu.add("Help").setIcon(android.R.drawable.ic_menu_help);
        return true;
    }
    
    /**
     * Processes menu options.
     */
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	if(item == mSynchronizeMenuItem)
    		requestOffset(0);
    	else if(item == mHelpMenuItem)
    	{
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage("Select a track to play it, or long-press to download an mp3 or delete the track.\n\nUse \"Synchronize\" from the MENU to synchronize track status with SoundCloud.")
    			.create().show();

    	}
    	else
    		return false;
    	return true;
    }

	public void requestOffset(int offset)
	{
        requestedOffset=offset;
        getSCApplication().processRequest("me/tracks?offset=" + offset + "&limit=" + limit, tracksRequestClient);
	}

	SoundCloudRequestClient tracksRequestClient = new SoundCloudRequestClient()
	{
		public void requestCompleted(HttpResponse response)
		{
			int x = response.getStatusLine().getStatusCode();
			if(x != 200)
			{
				Log.e(ViewTracksActivity.class.getSimpleName(), "Tracks request returned with response " + x + ", " + response.getStatusLine().getReasonPhrase());
				return;
			}

			if(requestedOffset == 0)
				getContentResolver().delete(DB.Tracks.CONTENT_URI, null, null);
			int numTracks = getSCApplication().processTracks(response);
				
			if(numTracks == limit)
				requestOffset(requestedOffset+limit);
		}
		
		public void requestFailed(Exception e)
		{
			Log.e(ViewTracksActivity.class.getSimpleName(), "Tracks request failed with exception");
			e.printStackTrace();
		}
	};

	SoundCloudRequestClient trackRequestClient = new SoundCloudRequestClient()
	{
		public void requestCompleted(HttpResponse response)
		{
			int x = response.getStatusLine().getStatusCode();
			if(x != 200)
			{
				Log.e(ViewTracksActivity.class.getSimpleName(), "Track request returned with response " + x + ", " + response.getStatusLine().getReasonPhrase());
				return;
			}
			getSCApplication().processTracks(response, true);
		}
		
		public void requestFailed(Exception e)
		{
			Log.d(ViewTracksActivity.class.getSimpleName(), "Track request failed with exception");
			e.printStackTrace();
		}
	};
	
	class DeleteRequestClient implements SoundCloudRequestClient
	{
		DeleteRequestClient(long id)
		{
			mId = id;
		}
		
		public void requestCompleted(HttpResponse response)
		{
			int x = response.getStatusLine().getStatusCode();
			if(x != 200)
			{
				Log.e(ViewTracksActivity.class.getSimpleName(), "Delete request returned with response " + x + ", " + response.getStatusLine().getReasonPhrase());
				return;
			}
			
	    	getContentResolver().delete
	    	(
	    		ContentUris.withAppendedId(DB.Tracks.CONTENT_URI, mId),
	    		null,
	    		null
	    	);
		}
		
		public void requestFailed(Exception e)
		{
			Log.d(ViewTracksActivity.class.getSimpleName(), "Delete request failed with exception");
			e.printStackTrace();
		}
		
		private long mId;
	};
	
	public void onListItemClick(ListView parent, View v, int position, long id)
	{
		playback(position);
	
/*		try
		{
			String signedUrl = getSCApplication().getSoundCloudAPI().signStreamUrl(streamUrl);
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(signedUrl));
			intent.setType("audio/mp3");
			startActivity(intent);
		}
		catch (Exception e)
		{			
		}*/
		
	}
	
    public static final int MENU_ITEM_PLAYBACK = Menu.FIRST;
    public static final int MENU_ITEM_DOWNLOAD = Menu.FIRST+1;
    public static final int MENU_ITEM_DELETE = Menu.FIRST+2;

    View.OnCreateContextMenuListener mCreateContextMenuListener = new View.OnCreateContextMenuListener()
    {
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo)
		{
			menu.add(Menu.NONE, MENU_ITEM_PLAYBACK, 0, "Play");
			menu.add(Menu.NONE, MENU_ITEM_DOWNLOAD, 1, "Download MP3");
			menu.add(Menu.NONE, MENU_ITEM_DELETE, 2, "Delete from SoundCloud");
		}
    };
    
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(ViewTracksActivity.class.getSimpleName(), "bad menuInfo", e);
            return false;
        }
        final AdapterView.AdapterContextMenuInfo finalinfo = info;

        switch(item.getItemId())
        {
        case MENU_ITEM_PLAYBACK:
        	playback(info.position);
        	return true;
        case MENU_ITEM_DOWNLOAD:
    		mCursor.moveToPosition(info.position);
    		String streamUrl = mCursor.getString(3);
    		Log.d(ViewTracksActivity.class.getSimpleName(), "getting " + streamUrl);
    		String file = getSCApplication().downloadStream(streamUrl, mCursor.getString(1));
    		Toast.makeText(this, "Downloading file to " + file, Toast.LENGTH_LONG).show();
        	return true;
        	
        case MENU_ITEM_DELETE:
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage("This will permanently delete the track from SoundCloud.  ARE YOU SURE YOU WANT TO DO THIS?")
    		       .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
    	    		{
    					public void onClick(DialogInterface dialog, int which)
    					{
    						delete(finalinfo.position);
    					}
    	    		})
    		       .setNegativeButton(android.R.string.cancel, null).create().show();
        	return true;
        }
        return false;
    }
	
    public void playback(int position)
    {
		mCursor.moveToPosition(position);
		long duration = mCursor.getLong(4);
		if(duration==0)
			getSCApplication().processRequest("tracks/" + mCursor.getLong(2), trackRequestClient);

		String streamUrl = mCursor.getString(3);
		Log.d(ViewTracksActivity.class.getSimpleName(), "getting " + streamUrl);
		try
		{
			streamUrl = getSCApplication().getSoundCloudAPI().signStreamUrl(streamUrl);
	
		    mPlaybackDialog.displayPlaybackDialog(streamUrl);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
    }
    
    public void delete(int position)
    {
    	mCursor.moveToPosition(position);
    	getSCApplication().processRequest("tracks/" + mCursor.getLong(2), new DeleteRequestClient(mCursor.getLong(0)), RequestType.DELETE);
    }
    
	Cursor mCursor;
	int requestedOffset;
	int limit = 50;
	
	private MenuItem mSynchronizeMenuItem, mHelpMenuItem;
	PlaybackDialog mPlaybackDialog;
	
    protected static final String[] sTracksProjection = new String[]
	{
	      DB.Tracks._ID, // 0
	      DB.Tracks.TITLE, // 1
	      DB.Tracks.ID, // 2
	      DB.Tracks.STREAM_URL, // 3
	      DB.Tracks.DURATION, // 4
	};
}