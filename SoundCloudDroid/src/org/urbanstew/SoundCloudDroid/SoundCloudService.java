package org.urbanstew.SoundCloudDroid;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;


public class SoundCloudService extends android.app.Service
{
	public final static boolean useSandbox = false;
	
	public void onCreate()
	{
		super.onCreate();

    	mSoundCloud = newSoundCloudRequest();
	}

	public void onDestroy()
	{
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SoundCloudService.this);
    	preferences.edit()
    	.putString("oauth_access_token", mSoundCloud.getToken())
    	.putString("oauth_access_token_secret", mSoundCloud.getTokenSecret())
    	.commit();

		super.onDestroy();
	}
	
	public void onStart(Intent intent, int startId)
	{
		final Bundle extras = intent.getExtras() == null ? new Bundle() : intent.getExtras();

    	final Uri path = intent.getData();
    	Log.d(getClass().getName(), "Uploading file:" + path);
    	
		if(!extras.containsKey("title"))
			extras.putString("title", "Uploaded from SoundCloud Droid service");

    	ContentValues values = new ContentValues();
    	values.put(DB.Uploads.TITLE, intent.getExtras().getString("title"));
    	values.put(DB.Uploads.STATUS, "uploading");
    	values.put(DB.Uploads.PATH, path.toString());

    	// insert the values
    	final Uri upload = getContentResolver().insert(DB.Uploads.CONTENT_URI, values);

    	final SoundCloudRequest request = new SoundCloudRequest(mSoundCloud);

    	new Thread(new Runnable()
    	{
			public void run()
			{
				String newStatus;
	    		if(request.uploadFile(path, extras))
	    			newStatus = "uploaded";
	    		else
	    			newStatus = "error";
	    		ContentValues values = new ContentValues();
	        	values.put(DB.Uploads.STATUS, newStatus);
	    		getContentResolver().update(upload, values, null, null);
			}
    	}).start();
	}

	SoundCloudRequest newSoundCloudRequest()
	{
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        // WARNING: the following resources are not a part of the repository for security reasons
        // to build and test this app, you should register your build of the app with SoundCloud:
        //  http://soundcloud.com/settings/applications/new
        // and add your Consumer Key and Consumer Secret as string resources to the project.
        // (with names "consumer_key" and "s5rmEGv9Rw7iulickCZl", respectively)
        String consumerKey, consumerSecret;
        if (!useSandbox)
        {
        	consumerKey = getResources().getString(R.string.consumer_key);
        	consumerSecret  = getResources().getString(R.string.s5rmEGv9Rw7iulickCZl);
        }
        else
        {
        	consumerKey = getResources().getString(R.string.sandbox_consumer_key);
        	consumerSecret  = getResources().getString(R.string.sandbox_consumer_secret);        	
        }

    	SoundCloudRequest soundCloud = new SoundCloudRequest
    	(
    		consumerKey,
    		consumerSecret,
    		preferences.getString("oauth_access_token", ""),
    		preferences.getString("oauth_access_token_secret", "")
    	);
    	
    	soundCloud.setUsingSandbox(useSandbox);
    	
    	return soundCloud;
	}
	
	String getUserName()
	{    	
    	String response = mSoundCloud.retreiveMe();
    	
    	Log.d(getClass().toString(), "Me complete, response=" + response);
    	if(response.length()==0)
    		return response;

		try {

    			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    			Document dom = db.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));
    			
    			return dom.getElementsByTagName("username").item(0).getFirstChild().getNodeValue();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public IBinder onBind(Intent arg0)
	{
		return mBinder;
	}
	
    /**
     * A secondary interface to the service.
     */
    private final ISoundCloudService.Stub mBinder = new ISoundCloudService.Stub()
    {
		public String getUserName() throws RemoteException
		{
			return SoundCloudService.this.getUserName();
		}

		public int getState() throws RemoteException
		{
			return SoundCloudService.this.mSoundCloud.getState().ordinal();
		}

		public String getAuthorizeUrl() throws RemoteException
		{
			return SoundCloudService.this.mSoundCloud.getAuthorizeUrl();
		}

		public boolean obtainRequestToken() throws RemoteException
		{
			return SoundCloudService.this.mSoundCloud.obtainRequestToken();
		}

		public void obtainAccessToken() throws RemoteException
		{
			SoundCloudService.this.mSoundCloud.obtainAccessToken();
			
	        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SoundCloudService.this);
	    	preferences.edit()
	    	.putString("oauth_access_token", mSoundCloud.getToken())
	    	.putString("oauth_access_token_secret", mSoundCloud.getTokenSecret())
	    	.commit();
	    }
    };
    
    SoundCloudRequest mSoundCloud;
}
