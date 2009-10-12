package org.urbanstew.SoundCloudDroid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.restlet.data.Form;
import org.restlet.engine.util.Base64;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import android.net.Uri;
import android.util.Log;

/**
 * SoundCloudRequest handles communication with the SoundCloud API and
 * oauth authorization.
 * 
 * @author      Stjepan Rajko
 */
public class SoundCloudRequest
{
    /**
     * Constructor for the case when neither the request or access token have
     * been obtained.
     */
	SoundCloudRequest(String consumerKey, String consumerSecret)
	{
		mConsumerKey = consumerKey;
		mConsumerSecret = consumerSecret;
		mToken = "";
		mTokenSecret = "";
	}

    /**
     * Constructor for the case when either the request or access token have
     * been obtained.
     */
	SoundCloudRequest(String consumerKey, String consumerSecret, String token, String tokenSecret)
	{
		mConsumerKey = consumerKey;
		mConsumerSecret = consumerSecret;
		mToken = token;
		mTokenSecret = tokenSecret;
	}
	
    /**
     * Signs data using they provided key and HMAC-SHA1.
     */
	static String sign(String data, String key)
	{
		try
		{
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(key.getBytes(), "HmacSHA1"));
	        return Base64.encode(mac.doFinal(data.getBytes()), false);	
		} catch (NoSuchAlgorithmException e1)
		{
			e1.printStackTrace();
		}
		catch (InvalidKeyException e)
		{
			e.printStackTrace();
		}
		return new String();
	}
	
    /**
     * POSTs the specified parameters to the specified URL.
     */
	Representation executeRequest(String resourceUrl, Form parameters)
	{
	    ClientResource resource = new ClientResource(resourceUrl);
    	try
		{
    		// construct the Signature Base String
			String signatureBase =
				"POST&" +
				URLEncoder.encode(resourceUrl, "UTF-8") +
				"&" +
				// is this double encode causing a bug in some cases?
				URLEncoder.encode(parameters.encode(), "UTF-8");
			
	       	Log.d(this.getClass().toString(), "Signature Base String: " + signatureBase);
	    	
			String signature = sign(signatureBase, mConsumerSecret + "&" + mTokenSecret);
	       	
			Log.d(this.getClass().toString(), "Signature: " + signature);
	    	parameters.add("oauth_signature", signature);

	    	Log.d(this.getClass().toString(), "Making POST request: " + parameters.getWebRepresentation());
	    	Log.d(this.getClass().toString(), "POST request contents: " + parameters.getWebRepresentation().getText());
			Representation r = resource.post(parameters.getWebRepresentation());
			boolean success = resource.getStatus().isSuccess();
			Log.d(this.getClass().toString(), resource.getStatus().toString());
			if(success)
				return r;
			return null;	    	
		} catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (ResourceException e)
		{
			e.printStackTrace();
		} 
		return null;
	}
	
    /**
     * Obtains the request token from Sound Cloud, and calls the specified
     * Runnable on success.
     */
	void obtainRequestToken(Runnable onSuccess)
	{
    	Form parameters = parametersForm();
    	
    	Representation r = executeRequest("http://api.soundcloud.com/oauth/request_token", parameters);
    	
		if(r != null)
		{
			try
			{
				Form token_info = new Form(r.getText());
				
				mToken = token_info.getFirstValue("oauth_token");				
				mTokenSecret = token_info.getFirstValue("oauth_token_secret");
				
				onSuccess.run();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
    /**
     * Returns the SoundCloud Authorize URL.
     */
	public String getAuthorizeUrl()
	{
		return "http://soundcloud.com/oauth/authorize?oauth_token=" + mToken;
	}
	
    /**
     * Swaps the authorized request token for an access token.
     */
	public void obtainAccessToken()
	{
		Form parameters = parametersForm();
		Representation r = executeRequest("http://api.soundcloud.com/oauth/access_token", parameters);
    	
		if(r != null)
		{
			try
			{
				Form token_info = new Form(r.getText());

				mToken = token_info.getFirstValue("oauth_token");
				mTokenSecret = token_info.getFirstValue("oauth_token_secret");
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}		
	}

    /**
     * Uploads the file specified by the URI.
     * 
     * @TODO: Currently uses hard-coded file content
     * @BUG: Doesn't work!
     */
	void uploadFile(Uri uri)
	{
		// get the oauth parameters
		Form parameters = parametersForm();
		
		// Add the two required parameters
		parameters.add("track[asset_data]", "RIFF");
//		parameters.add("track[description]", "file uploaded from SoundCloud Droid");
//		parameters.add("track[sharing]", "private");
		parameters.add("track[title]", "Upload");
		
		// execute the request (currently returns Bad Request (400) - Bad Request)
		Representation r = executeRequest("http://api.soundcloud.com/tracks", parameters);
		
		if(r != null)
		{
			try
			{
				Log.d(this.getClass().toString(), r.getText());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

    /**
     * Returns a form containing the standard oauth parameters.
     */
    private Form parametersForm(long timestamp, String nonc)
    {
    	Form parameters = new Form();
    	parameters.add("oauth_consumer_key", mConsumerKey);
    	parameters.add("oauth_nonce", nonc);
    	parameters.add("oauth_signature_method", "HMAC-SHA1");
    	parameters.add("oauth_timestamp", String.valueOf(timestamp));
    	parameters.add("oauth_token", mToken);
    	parameters.add("oauth_version", "1.0");
    	return parameters;
    }

    /**
     * Returns a form containing the standard oauth parameters.
     */
    private Form parametersForm()
    {
    	return parametersForm
    	(
    		generateTimestamp(),
    		generateNonc()
    	);
    }
    
    /**
     * Returns the timestamp corresponding to now.
     */
    private long generateTimestamp()
    {
    	return (new Date()).getTime() / 1000;
    }
    
    /**
     * Returns a new NONC.
     */
    private String generateNonc()
    {
    	return new BigInteger(100, new SecureRandom()).toString(32);
    }
    
    /**
     * Returns the Request or Access Token.
     */
    public String getToken()
	{
		return mToken;
	}

    /**
     * Returns the Request or Access Token Secret.
     */
	public String getTokenSecret()
	{
		return mTokenSecret;
	}
	
    // Consumer key and secret
    String mConsumerKey, mConsumerSecret;
    
    // Either the Request Token and Secret or the Access Token and Secret
    String mToken, mTokenSecret;
        
}
