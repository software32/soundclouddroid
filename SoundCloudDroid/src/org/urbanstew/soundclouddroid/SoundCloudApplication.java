package org.urbanstew.soundclouddroid;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.urbanstew.soundclouddroid.SoundCloudAuthorizationClient.AuthorizationStatus;
import org.urbanstew.soundcloudapi.ProgressFileBody;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class SoundCloudApplication extends Application
{
	public final static boolean useSandbox = false;
	
	public void onCreate()
	{
		super.onCreate();

    	mSoundCloud = newSoundCloudRequest();
    	mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
	
	public void uploadFile(Uri uri, final Bundle extras, final SoundCloudRequestClient client)
	{
		Log.d(getClass().getName(), "Uploading file data:" + uri.getPath());
		
    	final File file = new File(uri.getPath());
    	final ProgressFileBody fileBody = new ProgressFileBody(file);
    	
    	Log.d(getClass().getName(), "Uploading file:" + file.getAbsolutePath());

		if(!extras.containsKey("title"))
			extras.putString("title", "Uploaded from SoundCloud Droid service");

		boolean authorized = (getSoundCloudAPI().getState() == SoundCloudAPI.State.AUTHORIZED);
		
    	ContentValues values = new ContentValues();
    	final String title = extras.getString("title");
    	values.put(DB.Uploads.TITLE, title);
    	values.put(DB.Uploads.STATUS, authorized ? "uploading" : "unauthorized");
    	values.put(DB.Uploads.PATH, file.getAbsolutePath());
    	if(extras.containsKey("sharing"))
    		values.put(DB.Uploads.SHARING, extras.getString("sharing"));
    	if(extras.containsKey("description"))
    		values.put(DB.Uploads.DESCRIPTION, extras.getString("description"));
    	if(extras.containsKey("genre"))
    		values.put(DB.Uploads.GENRE, extras.getString("genre"));    	
    	if(extras.containsKey("track_type"))
    		values.put(DB.Uploads.TRACK_TYPE, extras.getString("track_type"));

    	// insert the values
    	final Uri upload = getContentResolver().insert(DB.Uploads.CONTENT_URI, values);
    	if(!authorized)
    	{
    		Toast.makeText(this, R.string.unauthorized_upload, Toast.LENGTH_LONG).show();
    		return;
    	}
    	
    	final int notificationId = Integer.parseInt(upload.getLastPathSegment()) * 2;

    	final SoundCloudAPI request = new SoundCloudAPI(mSoundCloud);
    	
    	//NotificationManager 
        Notification notification = new Notification(android.R.drawable.stat_sys_upload,"Uploading " + title + "...",System.currentTimeMillis());        
         
        RemoteViews remoteView = new RemoteViews(this.getPackageName(), R.layout.progress);
        remoteView.setCharSequence(R.id.progressText, "setText", title + " - SoundCloud");
        notification.contentView = remoteView;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        
        Intent notificationIntent = new Intent(getApplicationContext(), UploadsActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.contentIntent = contentIntent;    
               
        mNotificationManager.notify(notificationId, notification); 

		final ProgressRunnable progress = new ProgressRunnable
		(
			remoteView,
			notification,
			fileBody,
			notificationId
		);
		
		mHandler.postDelayed
		(
			progress,
			1000
		);
    	
    	Thread thread = new Thread(new Runnable()
    	{
			public void run()
			{
				List<NameValuePair> params = new java.util.ArrayList<NameValuePair>();

				for (String key : extras.keySet())
					params.add(new BasicNameValuePair("track["+key+"]", extras.getString(key)));
				boolean success = false;
	    		try
				{
					if(request.upload(fileBody, params).getStatusLine().getStatusCode() == HttpStatus.SC_CREATED)
						success = true;
				} catch (Exception e)
				{
				}
				progress.finish();
				
	    		ContentValues values = new ContentValues();
	        	values.put(DB.Uploads.STATUS, success ? "uploaded" : "failed");
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
	    		mNotificationManager.notify(notificationId+1, notification);
	    		
				complete(client, Thread.currentThread());
			}
    	});
    	
    	launch(client, thread);
	}

	public void processRequest(final String request, final SoundCloudRequestClient client)
	{
    	Thread thread = new Thread(new Runnable()
    	{
			public void run()
			{
		    	HttpResponse response;
				try
				{
					response = mSoundCloud.get(request);
					client.requestCompleted(response);
				} catch (Exception e)
				{
					client.requestFailed(e);
				}
				complete(client, Thread.currentThread());
			}
    	});
    	
    	launch(client, thread);
	}

	public void authorize(final SoundCloudAuthorizationClient client)
	{
		Thread thread = new Thread(new Runnable()
		{
			public void run()
			{
				AuthorizationStatus status = AuthorizationStatus.FAILED;

				try
				{
					// Find a free port
					int port=58088;
/*					for(port=58088; port<59099; port++)
					{
						ServerSocket socket = null;
						try
						{
							socket = new ServerSocket(port);
						    break;
						} catch (IOException e) {
						} finally {
						    if (socket != null) socket.close(); 
						}
					}*/
					if(port<59099)
					{
						if(mSoundCloud.authorizeUsingUrl("http://127.0.0.1:" + port + "/","Thank you for authorizing",client))
						{
							status = AuthorizationStatus.SUCCESSFUL;
							storeAuthorization();
						}
						else
							status = AuthorizationStatus.CANCELED;
					} // else failed
				} catch (Exception e)
				{
					// failed
				} finally
				{
					final AuthorizationStatus finalStatus = status;
					client.authorizationCompleted(finalStatus);
					complete(client, Thread.currentThread());
				}
			}
		});
		
		launch(client, thread);
	}

	private void launch(Object object, Thread thread)
	{
		synchronized(mThreads)
		{
			Set<Thread> threads;
			if(mThreads.containsKey(object))
				threads = mThreads.get(object);
			else
				threads = new HashSet<Thread>();
			threads.add(thread);
		}
		thread.start();
		Log.d(SoundCloudApplication.class.getSimpleName(), "Starting SoundCloudService");
		startService(new Intent(this, SoundCloudService.class));
	}
	
	private void complete(Object object, Thread thread)
	{
		synchronized(mThreads)
		{
			if(mThreads.containsKey(object))
			{
				Set<Thread> threads = mThreads.get(object);
				threads.remove(thread);
				if(threads.size()==0)
					mThreads.remove(object);
			}
			if(mThreads.size()==0)
			{
				Log.d(SoundCloudApplication.class.getSimpleName(), "Stopping SoundCloudService");
				stopService(new Intent(this, SoundCloudService.class));
			}
		}
	}
	
	public void cancel(Object object)
	{
		synchronized(mThreads)
		{
			if(mThreads.containsKey(object))
			{
				for(Thread thread : mThreads.get(object))
				{
					thread.interrupt();
					complete(object, thread);
				}
			}
		}
	}

	private void storeAuthorization()
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    	preferences.edit()
    	.putString("oauth_access_token", mSoundCloud.getToken())
    	.putString("oauth_access_token_secret", mSoundCloud.getTokenSecret())
    	.commit();
	}
    
    SoundCloudAPI mSoundCloud;
	NotificationManager mNotificationManager;
    
    Handler mHandler = new Handler();
    
    class ProgressRunnable implements Runnable
    { 
    	ProgressRunnable(RemoteViews remoteView, Notification notification, ProgressFileBody fileBody, int notificationId)
    	{
    		mRemoteView = remoteView;
    		mNotification = notification;
    		mFileBody = fileBody;
    		mContinue = true;
    		mId = notificationId;
    	}
    	
    	public void run()
		{
    		if(!mContinue)
    	    	return;

    		int percent = (int) (mFileBody.getBytesTransferred() * 100 / mFileBody.getContentLength());
    		update(percent);
            
            mHandler.postDelayed
	    	(
	    		this,
	    		1000
	    	);
		}
    	
    	public void finish()
    	{
    		mContinue = false;
    		update(100);
    		mNotificationManager.cancel(mId);
    	}
    	
    	private void update(int percent)
    	{
            mRemoteView.setProgressBar(R.id.progressBar, 100, percent, false);
            mRemoteView.setCharSequence(R.id.progressPercentage, "setText", percent + "%");
            mNotificationManager.notify(mId, mNotification);
    	}
    	
    	RemoteViews mRemoteView;
    	Notification mNotification;
    	ProgressFileBody mFileBody;
    	boolean mContinue;
    	int mId;
    }

	public SoundCloudAPI getSoundCloudAPI()
	{
		return mSoundCloud;
	};
	
	Map<Object, Set<Thread>> mThreads = new HashMap<Object, Set<Thread>>();
}