package org.urbanstew.SoundCloudBase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.urbanstew.SoundCloudBase.SoundCloudAuthorizationClient.AuthorizationStatus;
import org.urbanstew.soundcloudapi.CountingOutputStream;
import org.urbanstew.soundcloudapi.SoundCloudAPI;
import org.urbanstew.soundclouddroid.R;
import org.urbanstew.soundclouddroid.ViewTracksActivity;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class SoundCloudApplicationBase extends Application
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
	
	public enum RequestType
	{
		GET,
		GET_STREAM,
		DELETE
	}
	
	public Thread processRequest(final String request, final SoundCloudRequestClient client)
	{
		return processRequest(request, client, RequestType.GET);
	}

	public Thread processRequest(final String request, final SoundCloudRequestClient client, final RequestType type)
	{
    	Thread thread = new Thread(new Runnable()
    	{
			public void run()
			{
		    	HttpResponse response = null;
				try
				{
					switch(type)
					{
					case GET:
						response = mSoundCloud.get(request);
						break;
					case GET_STREAM:
						response = mSoundCloud.getStream(request);
						break;			
					case DELETE:
						response = mSoundCloud.delete(request);
						break;
					}
					client.requestCompleted(response);
				} catch (Exception e)
				{
					client.requestFailed(e);
				}
				complete(client, Thread.currentThread());
			}
    	});
    	
    	launch(client, thread);
    	return thread;
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

	public String downloadStream(final String url, final String title)
	{
		final String filename = title.replaceAll(" ", "_").replaceAll("[^a-zA-Z0-9_]", "") + ".mp3";
		
		final Notification notification = new Notification(android.R.drawable.stat_sys_download,"Downloading " + title + "...",System.currentTimeMillis());
		
		final RemoteViews remoteView = new RemoteViews(this.getPackageName(), R.layout.progress);
        remoteView.setCharSequence(R.id.progressText, "setText", title + " - SoundCloud");
        remoteView.setImageViewResource(R.id.progress_icon,android.R.drawable.stat_sys_download);
        notification.contentView = remoteView;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        Intent notificationIntent = new Intent(getApplicationContext(), ViewTracksActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.contentIntent = contentIntent;  
                
		final int notificationId = mDownloadNotificationId++;		
		
		final File newFile = new File(Environment.getExternalStorageDirectory() + "/SoundCloudDroid/" + filename);
		boolean fileCreated;

		new File(Environment.getExternalStorageDirectory() + "/SoundCloudDroid").mkdirs();
		newFile.delete();
		try
		{
			fileCreated = newFile.createNewFile();
			Log.d("file created: ", "" + fileCreated);
			final CountingOutputStream out = new CountingOutputStream (new FileOutputStream(newFile));
		
			Thread thread = new Thread(new Runnable()
	    	{
				public void run()
				{
			    	HttpResponse response = null;
			    	ProgressUpdater progress = null;
			    	boolean success = false;
					try
					{
						response = mSoundCloud.getStream(url);
	
						progress = new ProgressUpdater
						(
							remoteView,
							notification,
							new DownloadProgressable(out, response.getEntity().getContentLength()),
							notificationId
						);
	
						mHandler.postDelayed
						(
							progress,
							1000
						);
						
						Log.d("received stream", response.getStatusLine().getReasonPhrase());
	
						if(response.getStatusLine().getStatusCode()== 200)
						{
							try
							{
								response.getEntity().writeTo(out);
								success = true;
							} catch (IOException e)
							{
								e.printStackTrace();
							}
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
					
					if(progress != null)
					{
						progress.finish();
			    		String notificationString = title + " download " + (success ? "completed" : "failed");
			    		Notification notification = new Notification
			    		(
			    			success ? android.R.drawable.stat_sys_download_done : android.R.drawable.stat_notify_error,
			    			notificationString,
			    			System.currentTimeMillis()
			    		);
			    		Intent intent = new Intent(Intent.ACTION_VIEW);
			    		intent.setDataAndType(Uri.parse("file://" + newFile.getAbsolutePath()), "audio/mp3");
			    		notification.setLatestEventInfo
			    		(
			    			getApplicationContext(),
			    			"SoundCloud Droid",
			    			notificationString,
			    			PendingIntent.getActivity(getApplicationContext(), 0, intent, 0)
			    		);
			    		notification.flags |= Notification.FLAG_AUTO_CANCEL;
			    		mNotificationManager.notify(mDownloadNotificationId++, notification);

					}
				}
	    	});
			thread.start();
		} catch (IOException e1)
		{
		}
		return newFile.getAbsolutePath();
	}
	protected void launch(Object object, Thread thread)
	{
		synchronized(mThreads)
		{
			Set<Thread> threads;
			if(mThreads.containsKey(object))
				threads = mThreads.get(object);
			else
				threads = new HashSet<Thread>();
			threads.add(thread);

			thread.start();
			Log.d(SoundCloudApplicationBase.class.getSimpleName(), "Starting SoundCloudService");
			startService(new Intent(this, SoundCloudService.class));
		}
	}
	
	protected void complete(Object object, Thread thread)
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
				Log.d(SoundCloudApplicationBase.class.getSimpleName(), "Stopping SoundCloudService");
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
    
    protected SoundCloudAPI mSoundCloud;
	protected NotificationManager mNotificationManager;
    
	public SoundCloudAPI getSoundCloudAPI()
	{
		return mSoundCloud;
	};
	
	int mDownloadNotificationId = Integer.MAX_VALUE / 2;
	Map<Object, Set<Thread>> mThreads = new HashMap<Object, Set<Thread>>();
	protected Handler mHandler = new Handler();
	
	protected class ProgressUpdater implements Runnable
    {
		public ProgressUpdater(RemoteViews remoteView, Notification notification, Progressable progressable, int notificationId)
    	{
    		mRemoteView = remoteView;
    		mNotification = notification;
    		mProgressable = progressable;
    		mContinue = true;
    		mId = notificationId;
    	}
    	
    	public void run()
		{
    		if(!mContinue)
    	    	return;

    		int percent = mProgressable.getProgress();
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
    	Progressable mProgressable;
    	boolean mContinue;
    	int mId;
    }
}

class DownloadProgressable implements Progressable
{
	DownloadProgressable(CountingOutputStream out, long contentLength)
	{
		mOutputStream = out;
		mContentLength = contentLength;
	}
	
	public int getProgress()
	{
		return (int) (mOutputStream.getCount() * 100 / mContentLength);
	}	
	
	CountingOutputStream mOutputStream;
	long mContentLength;
}