package org.urbanstew.soundclouddroid;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.urbanstew.SoundCloudBase.ObtainAccessToken;
import org.urbanstew.SoundCloudBase.SoundCloudRequestClient;
import org.urbanstew.soundcloudapi.SoundCloudAPI;
import org.urbanstew.util.AppDataAccess;
import org.w3c.dom.Document;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
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
public class SoundCloudDroid extends SoundCloudActivity implements SoundCloudRequestClient
{
	public static float CURRENT_VERSION = 1.0f;
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

        ((ImageButton) findViewById(R.id.authorize_button))
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
        
        ((Button) findViewById(R.id.view_tracks_button))
    	.setOnClickListener(new OnClickListener()
    	{
			public void onClick(View arg0)
			{
				startActivity(new Intent(getApplication(), ViewTracksActivity.class));					
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
        
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    	if(preferences.getBoolean("check_old_version", true))
    	{
	        try
	        {
	        	getPackageManager().getPackageInfo("org.urbanstew.SoundCloudDroid", 0);
	        	
	        	new AlertDialog.Builder(this)
	        	.setTitle("Package Name Changed")
	        	.setMessage("For reasons beyond our control, we had to change the package name for version 1.0 of SoundCloud Droid.\n\nThis means you have to manually uninstall your old version of SoundCloud Droid. Select OK to do so.")
	        	.setPositiveButton
	        	(
	        		getString(android.R.string.ok),
	        		new DialogInterface.OnClickListener()
	        		{
						public void onClick(DialogInterface dialog, int which)
						{
						    Intent intent = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", "org.urbanstew.SoundCloudDroid", null)); 
						    startActivity(intent);
		    		    	preferences.edit().putBoolean("check_old_version", false).commit();
						}
	        		}
	        	).show();
	        } catch(PackageManager.NameNotFoundException e)
	        {
		    	preferences.edit().putBoolean("check_old_version", false).commit();
	        }
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
    
    public void onDestroy()
    {
    	super.onDestroy();
    	getSCApplication().cancel(this);
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
//    	else if(item == mSettingsMenuItem)
//    		startActivity(new Intent(getApplication(), SettingsActivity.class));
    	else if(item == mManualMenuItem)
    		startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://urbanstew.org/soundclouddroid/manual.html")));
    	else
    		return false;
    	return true;
    }
    
    public void updateAuthorizationStatus()
    {
    	int text;

		if(getSoundCloudAPI().getState() == SoundCloudAPI.State.AUTHORIZED)
		{
			if(mUserName != null)
			{
				setUserName(mUserName);
				return;
			}
			getSCApplication().processRequest("me", this);
			text = R.string.verifying_connection;
		}
		else
		{
			text = R.string.please_connect;
		}
		
		mAuthorized.setText(text);
	}
    
    public void authorize()
    {
		Intent authorizeIntent = new Intent(SoundCloudDroid.this, ObtainAccessToken.class);
		startActivity(authorizeIntent);
    }
    
	protected void onServiceConnected()
	{
		updateAuthorizationStatus();
	}

	public void setUserName(String userName)
	{
		mUserName = userName;
		
		String text;
		
		if(userName != null)
			text = getString(R.string.connected_as) + " " + userName + ".";
		else
			text = getString(R.string.unable_to_verify_connection);
		mAuthorized.setText(text);
	}

	public void requestCompleted(HttpResponse response)
	{
		String userName = null;

    	if(response.getStatusLine().getStatusCode() == 200)
			try {
	
	    			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	
	    			Document dom = db.parse(response.getEntity().getContent());
	    			
	    			userName = dom.getElementsByTagName("username").item(0).getFirstChild().getNodeValue();
			}catch(Exception e) {
				e.printStackTrace();
			}
		
		final String finalUserName = userName;
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				setUserName(finalUserName);
			}			
		});
	}

	public void requestFailed(Exception e)
	{
		mAuthorized.setText(R.string.unable_to_verify_connection);
	}
	
    // indicating whether SoundCloud Droid has been authorized
    // to access a user account
    TextView mAuthorized;

    MenuItem mView, mReport, mJoinGroup, mSettingsMenuItem, mManualMenuItem;
    
    static String mUserName = null;

}


