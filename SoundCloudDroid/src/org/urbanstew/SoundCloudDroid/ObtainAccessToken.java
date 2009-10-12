package org.urbanstew.SoundCloudDroid;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;

/**
 * ObtainAccessToken activity is used to execute the oauth
 * authorization protocol.
 * 
 * @author      Stjepan Rajko
 */
public class ObtainAccessToken extends Activity
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
        
        // WARNING: the following resources are not a part of the repository for security reasons
        // to build and test this app, you should register your build of the app with SoundCloud:
        //  http://soundcloud.com/settings/applications/new
        // and add your Consumer Key and Consumer Secret as string resources to the project.
        // (with names "consumer_key" and "s5rmEGv9Rw7iulickCZl", respectively)
        String consumerKey = getResources().getString(R.string.consumer_key);
        String consumerSecret = getResources().getString(R.string.s5rmEGv9Rw7iulickCZl);

        mRequest = new SoundCloudRequest(consumerKey, consumerSecret);
        
        mWebView = (WebView) findViewById(R.id.webview);
        mAuthorizedButton = (Button) findViewById(R.id.authorized_button);
        mAuthorizedButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				userHasAuthorized();
			}	
        });
        
        mRequest.obtainRequestToken(new Runnable(){

			public void run()
			{
	        	mWebView.loadUrl(mRequest.getAuthorizeUrl());
				
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
    	mRequest.obtainAccessToken();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    	preferences.edit()
    	.putString("oauth_access_token", mRequest.getToken())
    	.putString("oauth_access_token_secret", mRequest.getTokenSecret())
    	.commit();

    	finish();
	}
    

    SoundCloudRequest mRequest;

    // WebView used to open the SoundCloud authorization page
    WebView mWebView;

    // Button used by the user to indicate she has authorized SoundDroid 
    Button mAuthorizedButton;
    

}
