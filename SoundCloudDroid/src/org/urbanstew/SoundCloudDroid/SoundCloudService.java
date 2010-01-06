package org.urbanstew.SoundCloudDroid;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.urbanstew.soundcloudapi.SoundCloudAPI;
import org.w3c.dom.Document;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

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

		Log.d(getClass().getName(), "Uploading file data:" + intent.getData().getPath());
    	final File file = new File(intent.getData().getPath());
    	Log.d(getClass().getName(), "Uploading file:" + file.getAbsolutePath());
    	
		if(!extras.containsKey("title"))
			extras.putString("title", "Uploaded from SoundCloud Droid service");

    	ContentValues values = new ContentValues();
    	final String title = intent.getExtras().getString("title");
    	values.put(DB.Uploads.TITLE, title);
    	values.put(DB.Uploads.STATUS, "uploading");
    	values.put(DB.Uploads.PATH, file.getAbsolutePath());

    	// insert the values
    	final Uri upload = getContentResolver().insert(DB.Uploads.CONTENT_URI, values);

    	final SoundCloudAPI request = new SoundCloudAPI(mSoundCloud);

    	new Thread(new Runnable()
    	{
			public void run()
			{
				List<NameValuePair> params = new java.util.ArrayList<NameValuePair>();

				for (String key : extras.keySet())
					params.add(new BasicNameValuePair("track["+key+"]", extras.getString(key)));
				boolean success = false;
				NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	    		try
				{
					if(request.upload(file, params).getStatusLine().getStatusCode() == HttpStatus.SC_CREATED)
						success = true;
				} catch (Exception e)
				{
				}
	    		ContentValues values = new ContentValues();
	        	values.put(DB.Uploads.STATUS, success ? "uploaded" : "error");
	    		getContentResolver().update(upload, values, null, null);
	    		
	    		String notificationString = title + " upload " + (success ? "completed" : "failed");
	    		Notification notification = new Notification
	    		(
	    			success ? android.R.drawable.stat_sys_upload_done : android.R.drawable.stat_notify_error,
	    			notificationString,
	    			System.currentTimeMillis()
	    		);			
	    		notification.setLatestEventInfo
	    		(
	    			getApplicationContext(),
	    			"SoundCloud Droid",
	    			notificationString,
	    			PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), UploadsActivity.class), 0)
	    		);
	    		notification.flags |= Notification.FLAG_AUTO_CANCEL;
	    		nm.notify(0, notification);
			}
    	}).start();
	}

	SoundCloudAPI newSoundCloudRequest()
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

    	SoundCloudAPI soundCloud = new SoundCloudAPI
    	(
    		consumerKey,
    		consumerSecret,
    		preferences.getString("oauth_access_token", ""),
    		preferences.getString("oauth_access_token_secret", ""),
    		(useSandbox ? SoundCloudAPI.USE_SANDBOX : SoundCloudAPI.USE_PRODUCTION).with(SoundCloudAPI.OAuthVersion.V1_0)
    	);
    	    	
    	return soundCloud;
	}
	
	String getUserName()
	{    	
    	HttpResponse response;
		try
		{
			response = mSoundCloud.get("me");
		} catch (Exception e)
		{
			return "";
		}
    	
    	Log.d(getClass().toString(), "Me complete, response=" + response);
    	if(response.getStatusLine().getStatusCode() != 200)
    		return "";

		try {

    			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    			Document dom = db.parse(response.getEntity().getContent());
    			
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

		public String obtainRequestToken() throws RemoteException
		{
			try
			{
				return SoundCloudService.this.mSoundCloud.obtainRequestToken();
			} catch (Exception e)
			{
				return null;
			}
		}

		public void obtainAccessToken(String verificationCode) throws RemoteException
		{
			try
			{
				SoundCloudService.this.mSoundCloud.obtainAccessToken(verificationCode);
		        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SoundCloudService.this);
		    	preferences.edit()
		    	.putString("oauth_access_token", mSoundCloud.getToken())
		    	.putString("oauth_access_token_secret", mSoundCloud.getTokenSecret())
		    	.commit();
			} catch (Exception e)
			{
			}
			
	    }
    };
    
    SoundCloudAPI mSoundCloud;
}
