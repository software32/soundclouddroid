package org.urbanstew.SoundCloudBase;

import java.util.concurrent.Semaphore;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
public class ObtainAccessToken extends SoundCloudBaseActivity implements SoundCloudAuthorizationClient
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
        			if(url.startsWith("http://soundcloud.urbanstew.org/"))
        			{
        				mVerificationCode = Uri.parse(url).getQueryParameter("oauth_verifier");
        				Log.d(ObtainAccessToken.class.getName(), "Verification code is: " + mVerificationCode);
        				mVerificationCodeAvailable.release();
        				return true;
        			}
					return false;
        		}
        	});
        
        Toast.makeText(ObtainAccessToken.this, "Please select \"Allow access\" when the SoundCloud authorization page loads.", Toast.LENGTH_LONG).show();

        mVerificationCodeAvailable = new Semaphore(0);
    	getSCApplicationBase().authorizeWithoutCallback(this);
    }

    public void onDestroy()
    {
    	super.onDestroy();
    	// if the authorization is complete, canceling doesn't hurt
    	getSCApplicationBase().cancel(this);
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
					"Authorization "
						+ (status == AuthorizationStatus.SUCCESSFUL ? "successful." : "failed.")
						+ (mAuthorizationException != null ? mAuthorizationException.toString() : ""),
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
    
	public String getVerificationCode()
	{
		try
		{
			mVerificationCodeAvailable.acquire();
		} catch (InterruptedException e)
		{
			return null;
		}
		return mVerificationCode;
	}
	
	public void exceptionOccurred(Exception e)
	{
		mAuthorizationException = e;
	}

    // WebView used to open the SoundCloud authorization page
    WebView mWebView;

    // Button used by the user to indicate she has authorized SoundDroid 
    Button mAuthorizedButton;
    
    Semaphore mVerificationCodeAvailable;
    String mVerificationCode;
    Exception mAuthorizationException = null;
}
