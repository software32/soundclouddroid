package org.urbanstew.SoundCloudDroid;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
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
import android.util.Log;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpMethodParams;


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
	
	void signRequest(String resourceUrl, Form parameters)
	{
		// construct the Signature Base String
		try
		{
			String signatureBase = "POST&" +
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
		//
		Log.d(getClass().getName(), "Uploading file " + uri.getPath());
		
		// get the oauth parameters
		Form parameters = parametersForm();
		
		// Add the two required parameters
//		parameters.add("track[asset_data]", "RIFF");
//		parameters.add("track[description]", "file uploaded from SoundCloud Droid");
//		parameters.add("track[sharing]", "private");
		parameters.add("track[title]", "Upload");
		
		// execute the request (currently returns Bad Request (400) - Bad Request)
		//Representation r = executeRequest("http://api.soundcloud.com/tracks", parameters);
		
		signRequest("http://api.soundcloud.com/tracks", parameters);
		
		File targetFile = new File(uri.getPath());
		try
		{
			PostMethod filePost = new PostMethod("http://api.soundcloud.com/tracks");
			Part[] parts = new Part[parameters.size() + 1];
			int i=0;
			for(Parameter p : parameters)
			{
				parts[i++] = new StringPart(p.getName(), p.getValue());
				Log.d(getClass().getName(), "Parameter: " + p.getName() + "=" + p.getValue());
			}
			parts[i] = new FilePart("track[asset_data]", targetFile);

			
			filePost.setRequestEntity(
                    new MultipartRequestEntity(parts, filePost.getParams())
                    );
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			new MultipartRequestEntity(parts, filePost.getParams()).writeRequest(out);
			System.out.println(out.toString("UTF-8"));
			
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
            int status = client.executeMethod(filePost);
            if (status == HttpStatus.SC_OK) {
                Log.d(getClass().toString(), "Upload complete, response=" + filePost.getResponseBodyAsString()
                );
            } else {
                Log.d(getClass().toString(),
                    "Upload failed, response=" + HttpStatus.getStatusText(status)
                );
            }

		} catch (FileNotFoundException e1)
		{
			e1.printStackTrace();
		} catch (HttpException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


/*
		  
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
		}*/
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
