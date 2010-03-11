package org.urbanstew.soundclouddroid;

import org.urbanstew.soundcloudapi.SoundCloudAPI;

import android.app.Activity;

public class SoundCloudActivity extends Activity
{
	public SoundCloudAPI getSoundCloudAPI()
	{
		return ((SoundCloudApplication)getApplication()).getSoundCloudAPI();
	}

	public SoundCloudApplication getSCApplication()
	{
		return (SoundCloudApplication)getApplication();
	}
}
