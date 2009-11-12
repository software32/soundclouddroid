package org.urbanstew.SoundCloudDroid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

public abstract class ServiceActivity extends Activity
{
	/**
     * The method called when the Activity is created.
     * <p>
     * Binds to the SoundCloud service.
     */
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	bindService(new Intent(ISoundCloudService.class.getName()),
            mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void onDestroy()
    {
    	unbindService(mServiceConnection);
    	super.onDestroy();
    }
    
    /**
     * Class for interacting with the secondary interface of the service.
     */
    private ServiceConnection mServiceConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
        	mSoundCloudService = ISoundCloudService.Stub.asInterface(service);
        	ServiceActivity.this.onServiceConnected();
        }

        public void onServiceDisconnected(ComponentName className)
        {
        	mSoundCloudService = null;
        }
    };

    ISoundCloudService mSoundCloudService = null;

	protected abstract void onServiceConnected();
}
