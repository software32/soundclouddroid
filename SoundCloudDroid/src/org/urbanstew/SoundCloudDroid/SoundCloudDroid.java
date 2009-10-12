package org.urbanstew.SoundCloudDroid;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

/**
 * SoundCloudDroid is the main SoundCloud Droid activity.
 * <p>
 * It shows
 * whether SoundCloud Droid has been authorized to access a user
 * account, can initiate the authorization process, and can upload
 * a file to SoundCloud.
 * 
 * @author      Stjepan Rajko
 */
public class SoundCloudDroid extends Activity
{

    /**
     * The method called when the Activity is created.
     * <p>
     * Initializes the user interface.
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mAuthorized = (CheckBox) this.findViewById(R.id.access_token_status);
    	                
        ((Button) this.findViewById(R.id.authorize_button))
        	.setOnClickListener(new OnClickListener()
	        {
				public void onClick(View arg0)
				{
					Intent authorizeIntent = new Intent(SoundCloudDroid.this, ObtainAccessToken.class);
					startActivity(authorizeIntent);
				}
	        });
        
        ((Button) this.findViewById(R.id.upload_button))
        	.setOnClickListener(new OnClickListener()
        	{
				public void onClick(View arg0)
				{
	        		uploadFile();					
				}
        	});
    }
        
    /**
     * The method called when the Activity is resumed.
     * <p>
     * Updates the UI to reflect whether SoundCloud Droid has been
     * authorized to access a user account.
     */
    public void onResume()
    {
    	super.onResume();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mAuthorized.setChecked(preferences.contains("oauth_access_token") && preferences.contains("oauth_access_token_secret"));
    }

    /**
     * The method called when the upload button is pressed.
     * <p>
     * Invokes OIFileManager to select the file to be uplaoded, or
     * if OIFileManager is not installed it starts the browser
     * to download it. 
     */
    public void uploadFile()
    {
    	Intent intent = new Intent("org.openintents.action.PICK_FILE");
    	intent.setData(Uri.parse("file:///sdcard/"));
    	intent.putExtra("org.openintents.extra.TITLE", "Please select a file");
    	try
    	{
    		startActivityForResult(intent, 1);
    	} catch (ActivityNotFoundException e)
    	{
    		Intent downloadOIFM = new Intent("android.intent.action.VIEW");
    		downloadOIFM.setData(Uri.parse("http://openintents.googlecode.com/files/FileManager-1.0.0.apk"));
    		startActivity(downloadOIFM);
    	}
    }
    
    /**
     * The method called when the file to be uploaded is selected.
     * <p>
     * Invokes OIFileManager to select the file to be uplaoded, or
     * if OIFileManager is not installed it starts the browser
     * to download it. 
     */
    protected void onActivityResult(int requestCode,
            int resultCode, Intent data)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    	Log.d(this.getClass().toString(), "Uploading file:" + data.getData());
    	
        // WARNING: the following resources are not a part of the repository for security reasons
        // to build and test this app, you should register your build of the app with SoundCloud:
        //  http://soundcloud.com/settings/applications/new
        // and add your Consumer Key and Consumer Secret as string resources to the project.
        // (with names "consumer_key" and "s5rmEGv9Rw7iulickCZl", respectively)
    	String consumerKey = getResources().getString(R.string.consumer_key);
        String consumerSecret = getResources().getString(R.string.s5rmEGv9Rw7iulickCZl);

    	SoundCloudRequest request = new SoundCloudRequest
    	(
    		consumerKey,
    		consumerSecret,
    		preferences.getString("oauth_access_token", ""),
    		preferences.getString("oauth_access_token_secret", "")
    	);
    	
    	request.uploadFile(data.getData());
    }
    
    // checkbox indicating whether SoundCloud Droid has been authorized
    // to access a user account
    CheckBox mAuthorized;
}


