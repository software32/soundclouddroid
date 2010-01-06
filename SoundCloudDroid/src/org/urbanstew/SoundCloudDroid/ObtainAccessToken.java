package org.urbanstew.SoundCloudDroid;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

/**
 * ObtainAccessToken activity is used to execute the oauth
 * authorization protocol.
 * 
 * @author      Stjepan Rajko
 */
public class ObtainAccessToken extends ServiceActivity
{
    /**
     * Called when the Activity is created.
     * <p>
     * Initializes the SoundCloudRequest object and the user interface,
     * and then tries to obtain the request token from SoundCloud.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.obtain_access_token);
                
        mWebView = (WebView) findViewById(R.id.webview);
        mAuthorizedButton = (Button) findViewById(R.id.authorized_button);
        mAuthorizedButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				userHasAuthorized();
			}	
        });
    }
    
    /**
     * Called when the user presses the button indicating she has authorized
     * SoundCloud Droid for access to the user account.
     * <p>
     * It obtains the access token using the request token, and stores it
     * in the app's preferences.
     */
    private void userHasAuthorized()
    {
    	try
		{
			mSoundCloudService.obtainAccessToken(null);
	    	finish();
		} catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    // WebView used to open the SoundCloud authorization page
    WebView mWebView;

    // Button used by the user to indicate she has authorized SoundDroid 
    Button mAuthorizedButton;

	@Override
	protected void onServiceConnected()
	{
        try
		{
        	String url = mSoundCloudService.obtainRequestToken();
			if (url != null)
			{
				mWebView.loadUrl(url);
				return;
			}
		} catch (RemoteException e)
		{
			e.printStackTrace();
		}
		Toast.makeText(this, "There was a problem obtaining an OAuth Request Token from SoundCloud", Toast.LENGTH_LONG).show();
	}
    

}
