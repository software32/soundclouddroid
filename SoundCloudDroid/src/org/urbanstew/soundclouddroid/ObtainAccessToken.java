package org.urbanstew.soundclouddroid;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

/**
 * ObtainAccessToken activity is used to execute the oauth
 * authorization protocol.
 * 
 * @author      Stjepan Rajko
 */
public class ObtainAccessToken extends SoundCloudActivity implements SoundCloudAuthorizationClient
{
    /**
     * Called when the Activity is created.
     * <p>
     * Initializes the SoundCloudRequest object and the user interface,
     * and then tries to obtain the request token from SoundCloud.
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.obtain_access_token);
                
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setWebViewClient(
        	new WebViewClient()
        	{
        		public boolean shouldOverrideUrlLoading(WebView view, String url)
        		{
					return false;
        		}
        	});
        
        Toast.makeText(ObtainAccessToken.this, "Please select \"Allow access\" when the SoundCloud authorization page loads.", Toast.LENGTH_LONG).show();

    	getSCApplication().authorize(this);
    }

    public void onDestroy()
    {
    	super.onDestroy();
    	// if the authorization is complete, canceling doesn't hurt
    	getSoundCloudAPI().cancelAuthorizeUsingUrl();
    	getSCApplication().cancel(this);
    }

	public void authorizationCompleted(final AuthorizationStatus status)
	{
		if(status == AuthorizationStatus.CANCELED)
			return;
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				Toast.makeText
				(
					ObtainAccessToken.this,
					"Authorization " + (status == AuthorizationStatus.SUCCESSFUL ? "successful." : "failed."),
					Toast.LENGTH_LONG
				).show();
			}	
		});
		finish();	
	}

	public void openAuthorizationURL(String url)
	{
		mWebView.loadUrl(url);		
	}
    
    // WebView used to open the SoundCloud authorization page
    WebView mWebView;

    // Button used by the user to indicate she has authorized SoundDroid 
    Button mAuthorizedButton;
}
