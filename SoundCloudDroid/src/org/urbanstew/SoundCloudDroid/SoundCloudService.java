package org.urbanstew.SoundCloudDroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;


public class SoundCloudService extends android.app.Service
{

	public void onCreate()
	{
		super.onCreate();
	}

	public void onDestroy()
	{
		super.onDestroy();
	}
	
	public void onStart(Intent intent, int startId)
	{
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final Uri path = intent.getData();
    	Log.d(this.getClass().toString(), "Uploading file:" + path);
    	
        // WARNING: the following resources are not a part of the repository for security reasons
        // to build and test this app, you should register your build of the app with SoundCloud:
        //  http://soundcloud.com/settings/applications/new
        // and add your Consumer Key and Consumer Secret as string resources to the project.
        // (with names "consumer_key" and "s5rmEGv9Rw7iulickCZl", respectively)
    	String consumerKey = getResources().getString(R.string.consumer_key);
        String consumerSecret = getResources().getString(R.string.s5rmEGv9Rw7iulickCZl);

    	final SoundCloudRequest request = new SoundCloudRequest
    	(
    		consumerKey,
    		consumerSecret,
    		preferences.getString("oauth_access_token", ""),
    		preferences.getString("oauth_access_token_secret", "")
    	);
    	
    	new Thread(new Runnable()
    	{
			public void run()
			{
	    		request.uploadFile(path);
			}
    	}).start();
	}
	
	@Override
	public IBinder onBind(Intent arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
