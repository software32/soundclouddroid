package org.urbanstew.SoundCloudDroid;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
        
        mAuthorized = (TextView) this.findViewById(R.id.authorization_status);

        mAuthorizeButton = (Button) this.findViewById(R.id.authorize_button);
        mAuthorizeButton
        	.setOnClickListener(new OnClickListener()
	        {
				public void onClick(View arg0)
				{
					authorize();
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
    	else
    		return false;
    	return true;
    }
    
    public void updateAuthorizationStatus()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(preferences.contains("oauth_access_token") && preferences.contains("oauth_access_token_secret"))
        {
        	mAuthorized.setText("authorized as " + getUserName());
        	mAuthorizeButton.setText("Re-authorize");
        }
        else
        {
        	mAuthorized.setText("unauthorized");
        	mAuthorizeButton.setText("Authorize");
        }
    }
    
    public void authorize()
    {
		Intent authorizeIntent = new Intent(SoundCloudDroid.this, ObtainAccessToken.class);
		startActivity(authorizeIntent);
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
    		Toast.makeText(this, "When OIFileManager is finished downloading, please select it to install it, and then try uploading again from SoundCloud Droid.", Toast.LENGTH_LONG).show();
    		Intent downloadOIFM = new Intent("android.intent.action.VIEW");
    		downloadOIFM.setData(Uri.parse("http://openintents.googlecode.com/files/FileManager-1.0.0.apk"));
    		startActivity(downloadOIFM);
    	}
    }
    
    String getUserName()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    	String consumerKey = getResources().getString(R.string.consumer_key);
        String consumerSecret = getResources().getString(R.string.s5rmEGv9Rw7iulickCZl);

    	SoundCloudRequest request = new SoundCloudRequest
    	(
    		consumerKey,
    		consumerSecret,
    		preferences.getString("oauth_access_token", ""),
    		preferences.getString("oauth_access_token_secret", "")
    	);
    	
    	String response = request.retreiveMe();
    	
    	Log.d(getClass().toString(), "Me complete, response=" + response);

		try {

    			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    			Document dom = db.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));
    			
    			return dom.getElementsByTagName("username").item(0).getFirstChild().getNodeValue();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return "";
    }
    
    /**
     * The method called when the file to be uploaded is selected.
     */
    protected void onActivityResult(int requestCode,
            int resultCode, Intent data)
    {
    	if(data == null)
    		return;
    	
    	Intent soundCloudUpload = new Intent(SoundCloudDroid.this, SoundCloudService.class);
    	soundCloudUpload.setData(data.getData());
		startService(soundCloudUpload);
    }
    
    // indicating whether SoundCloud Droid has been authorized
    // to access a user account
    TextView mAuthorized;
    
    Button mAuthorizeButton;
    
    MenuItem mView, mReport, mJoinGroup;
}


