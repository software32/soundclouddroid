package org.urbanstew.SoundCloudDroid;

import org.urbanstew.soundcloudapi.SoundCloudAPI;
import org.urbanstew.util.AppDataAccess;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

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
public class SoundCloudDroid extends ServiceActivity
{
	public static float CURRENT_VERSION = 0.6f;
	/**
     * The method called when the Activity is created.
     * <p>
     * Initializes the user interface.
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.main);
        
        mAuthorized = (TextView) findViewById(R.id.authorization_status);

        mAuthorizeButton = (Button) findViewById(R.id.authorize_button);
        mAuthorizeButton
        	.setOnClickListener(new OnClickListener()
	        {
				public void onClick(View arg0)
				{
					authorize();
				}
	        });
        
        ((Button) findViewById(R.id.upload_button))
        	.setOnClickListener(new OnClickListener()
        	{
				public void onClick(View arg0)
				{
					startActivity(new Intent(getApplication(), UploadActivity.class));		
				}
        	});
        
        ((Button) findViewById(R.id.upload_status_button))
    	.setOnClickListener(new OnClickListener()
    	{
			public void onClick(View arg0)
			{
				startActivity(new Intent(getApplication(), UploadsActivity.class));					
			}
    	});
        
        final AppDataAccess appData = new AppDataAccess(this);
        if(appData.getVisitedVersion() == 0)
        {
        	new AlertDialog.Builder(this)
        	.setTitle("License")
        	.setMessage(getString(R.string.license))
        	.setPositiveButton
        	(
        		getString(android.R.string.ok),
        		new DialogInterface.OnClickListener()
        		{
					public void onClick(DialogInterface dialog, int which)
					{
	        			appData.setVisitedVersion(CURRENT_VERSION);
					}
        		}
        	).show();
        }
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
    	updateAuthorizationStatus();
    }
    
    /**
     * Sets up menu options.  Currently all have to do with defect / bug reports and discussion group.
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        
        mView = menu.add("View reported defects and feature requests").setIcon(android.R.drawable.ic_dialog_info);
        mReport = menu.add("Report defect or feature request").setIcon(android.R.drawable.ic_dialog_alert);
        mJoinGroup = menu.add("Join discussion group").setIcon(android.R.drawable.ic_dialog_email);
        //mSettingsMenuItem = menu.add("Preferences").setIcon(android.R.drawable.ic_menu_preferences);
        mManualMenuItem = menu.add("Read the manual").setIcon(android.R.drawable.ic_menu_manage);
        return true;
    }
    
    /**
     * Processes menu options.
     */
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	if(item == mView)
    	    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://code.google.com/p/soundclouddroid/issues/list")));    		
    	else if(item == mReport)
    	    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://code.google.com/p/soundclouddroid/issues/entry")));
    	else if(item == mJoinGroup)
    		startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://groups.google.com/group/soundcloud-droid/subscribe")));
    	else if(item == mSettingsMenuItem)
    		startActivity(new Intent(getApplication(), SettingsActivity.class));
    	else if(item == mManualMenuItem)
    		startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://urbanstew.org/soundclouddroid/manual.html")));
    	else
    		return false;
    	return true;
    }
    
    public void updateAuthorizationStatus()
    {
    	String text;
    	boolean buttonEnabled = false;
    	if(mSoundCloudService==null)
    		text = "connecting to SoundCloud service...";
    	else
    		try
    		{
    			if(mSoundCloudService.getState() == SoundCloudAPI.State.AUTHORIZED.ordinal())
    			{
    				String userName = mSoundCloudService.getUserName();
    				if(userName.length()>0)
    					text = "SoundCloud Droid is authorized as " + mSoundCloudService.getUserName() + ".";
    				else
    					text = "SoundCloud Droid is unable to verify the authorization.";
    				mAuthorizeButton.setText("Re-authorize");
    			}
    			else
    			{
    	        	text = "Please use the Authorize button to authorize SoundCloud Droid for SoundCloud access.";
    	        	mAuthorizeButton.setText("Authorize");
    			}
    			buttonEnabled = true;
    		} catch (RemoteException e)
			{
				text = "There was a problem accessing SoundCloud.";
				buttonEnabled = false;
			}
		mAuthorized.setText(text);
		mAuthorizeButton.setEnabled(buttonEnabled);
    }
    
    public void authorize()
    {
		Intent authorizeIntent = new Intent(SoundCloudDroid.this, ObtainAccessToken.class);
		startActivity(authorizeIntent);
    }
    
    // indicating whether SoundCloud Droid has been authorized
    // to access a user account
    TextView mAuthorized;
    
    Button mAuthorizeButton;
    
    MenuItem mView, mReport, mJoinGroup, mSettingsMenuItem, mManualMenuItem;

	protected void onServiceConnected()
	{
		updateAuthorizationStatus();
	}
}


