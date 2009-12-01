package org.urbanstew.SoundCloudDroid;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
import org.restlet.data.Parameter;
import org.restlet.engine.util.Base64;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;


/**
 * SoundCloudRequest handles communication with the SoundCloud API and
 * oauth authorization.
 * 
 * @author      Stjepan Rajko
 */
public class SoundCloudRequest
{
    enum State
    {
    	UNAUTHORIZED,
    	REQUEST_TOKEN_OBTAINED,
    	AUTHORIZED
    };

    /**
     * Constructor for the case when neither the request or access token have
     * been obtained.
     */
	SoundCloudRequest(String consumerKey, String consumerSecret)
	{
		this(consumerKey, consumerSecret, "", "");
	}

    /**
     * Constructor for the case when the access token has been obtained.
     */
	SoundCloudRequest(String consumerKey, String consumerSecret, String token, String tokenSecret)
	{
		mClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);

		mConsumerKey = consumerKey;
		mConsumerSecret = consumerSecret;
		mToken = token;
		mTokenSecret = tokenSecret;

		mState =
			(mToken.length()==0 || mTokenSecret.length()==0) ?
			State.UNAUTHORIZED :
			State.AUTHORIZED;
		
		setUsingSandbox(false);
	}
	
    /**
     * Constructor from another SoundCloudRequest.
     */
	SoundCloudRequest(SoundCloudRequest soundCloudRequest)
	{
		this(soundCloudRequest.mConsumerKey, soundCloudRequest.mConsumerSecret, soundCloudRequest.mToken, soundCloudRequest.mTokenSecret);

		mState = soundCloudRequest.mState;
		mSoundCloudURL = soundCloudRequest.mSoundCloudURL;
		mSoundCloudApiURL = soundCloudRequest.mSoundCloudApiURL;
	}
	
	void setUsingSandbox(boolean use)
	{
		if(!use)
		{
			mSoundCloudURL = "http://soundcloud.com/";
			mSoundCloudApiURL = "http://api.soundcloud.com/";
		}
		else
		{
			mSoundCloudURL = "http://sandbox-soundcloud.com/";
			mSoundCloudApiURL = "http://api.sandbox-soundcloud.com/";
		}
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
    		signRequest(resourceUrl, parameters, "POST");

			Representation r = resource.post(parameters.getWebRepresentation());
			boolean success = resource.getStatus().isSuccess();
			Log.d(this.getClass().toString(), resource.getStatus().toString());
			if(success)
				return r;
			return null;	    	
		} catch (ResourceException e)
		{
			e.printStackTrace();
		} 
		return null;
	}
	
	void signRequest(String resourceUrl, Form parameters, String method)
	{
		// construct the Signature Base String
		try
		{
			String signatureBase = method + "&" +
			URLEncoder.encode(resourceUrl, "UTF-8") +
			"&" +
			// is this double encode causing a bug in some cases?
			URLEncoder.encode(parameters.encode(), "UTF-8");
		
			Log.d(this.getClass().toString(), "Signature Base String: " + signatureBase);
    	
			String signature = sign(signatureBase, mConsumerSecret + "&" + mTokenSecret);
       	
			Log.d(this.getClass().toString(), "Signature: " + signature);
			parameters.add("oauth_signature", signature);
		} catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    /**
     * Obtains the request token from Sound Cloud
     * @return true on success.
     */
	boolean obtainRequestToken()
	{
		mToken = mTokenSecret = "";
		mState = State.UNAUTHORIZED;
		    	
    	Representation r = executeRequest(mSoundCloudApiURL + "oauth/request_token", parametersForm());
    	
		if(r != null)
		{
			try
			{
				Form token_info = new Form(r.getText());
				
				mToken = token_info.getFirstValue("oauth_token");				
				mTokenSecret = token_info.getFirstValue("oauth_token_secret");
				
				mState = State.REQUEST_TOKEN_OBTAINED;
				
				return true;
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return false;
	}
	
    /**
     * Returns the SoundCloud Authorize URL.
     */
	public String getAuthorizeUrl()
	{
		return mSoundCloudURL + "oauth/authorize?oauth_token=" + mToken;
	}
	
    /**
     * Swaps the authorized request token for an access token.
     */
	public void obtainAccessToken()
	{
		Form parameters = parametersForm();
		Representation r = executeRequest(mSoundCloudApiURL + "oauth/access_token", parameters);
    	
		if(r != null)
		{
			try
			{
				Form token_info = new Form(r.getText());

				mToken = token_info.getFirstValue("oauth_token");
				mTokenSecret = token_info.getFirstValue("oauth_token_secret");
				
				mState = State.AUTHORIZED;
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	String authorizationHeader(String url, String method)
	{
		Form parameters = parametersForm();
		
		String authorizationHeader = "OAuth ";
		try
		{
			for(Parameter p : parameters)
			{
				authorizationHeader += URLEncoder.encode(p.getName(), "UTF-8") + "=\""+ URLEncoder.encode(p.getValue(), "UTF-8") + "\"";
				authorizationHeader += ", ";
			}
			signRequest(url, parameters, method);
			authorizationHeader += URLEncoder.encode(parameters.get(parameters.size()-1).getName(), "UTF-8") + "=\""+ URLEncoder.encode(parameters.get(parameters.size()-1).getValue(), "UTF-8") + "\"";
			Log.d(getClass().getName(), "Authorization Header: " + authorizationHeader);
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		return authorizationHeader;
	}
    /**
     * Uploads the file specified by the URI.
     * 
     */
	boolean uploadFile(Uri uri, Bundle properties)
	{
		File targetFile = new File(uri.getPath());

		try
		{
			PostMethod filePost = new PostMethod(mSoundCloudApiURL + "tracks");
		
			filePost.addRequestHeader(new Header("Authorization", authorizationHeader(mSoundCloudApiURL + "tracks", "POST")));
			
			Part[] parts = new Part[properties.size() + 1];
			
			int i=0;
			for (String key : properties.keySet())
				parts[i++] = new StringPartMod("track[" + key + "]", properties.getString(key));
			
			parts[i] = 	new FilePart("track[asset_data]", targetFile);

			MultipartRequestEntity mre = new MultipartRequestEntity(parts, filePost.getParams());

			filePost.setRequestEntity(mre);
			
            int status = mClient.executeMethod(filePost);
            if (status == HttpStatus.SC_CREATED) {
                Log.d(getClass().toString(), "Upload complete, response=" + filePost.getResponseBodyAsString());
                return true;
            } else {
                Log.d(getClass().toString(),
                    "Upload failed, response=" + HttpStatus.getStatusText(status)
                );
            }
		} catch (HttpException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return false;
	}
	

	String retreiveMe()
	{
		GetMethod meGet;
		try
		{
			meGet = new GetMethod(mSoundCloudApiURL + "me");

			meGet.addRequestHeader(new Header("Authorization", authorizationHeader(mSoundCloudApiURL + "me", "GET")));

			int status = mClient.executeMethod(meGet);
	        if (status == HttpStatus.SC_OK)
	        {
	        	String response = meGet.getResponseBodyAsString();
	            return response;
	        }
	        else
			{
				Log.d(getClass().toString(), "Me failed, response="
						+ HttpStatus.getStatusText(status));
			}
		} catch (HttpException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return new String();
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
	
	public State getState()
	{
		return mState;
	}
	
	String mSoundCloudURL;
	String mSoundCloudApiURL;
	
    // Consumer key and secret
    String mConsumerKey, mConsumerSecret;
    
    // Either the Request Token and Secret or the Access Token and Secret
    String mToken, mTokenSecret;
    
    State mState;
    
    HttpClient mClient = new HttpClient();
}

class StringPartMod extends StringPart
{

	public StringPartMod(String name, String value)
	{
		super(name, value);
	}
	
	protected void sendContentTypeHeader(OutputStream arg0)
	{
	}
	
	protected void sendTransferEncodingHeader(OutputStream arg0)
	{
		
	}
	
}