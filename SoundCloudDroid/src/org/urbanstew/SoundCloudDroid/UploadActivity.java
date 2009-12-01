package org.urbanstew.SoundCloudDroid;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class UploadActivity extends Activity
{
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.upload);
        
        mTitleEdit=(EditText)findViewById(R.id.title_edit);
        mTitleEdit.selectAll();
        
        mUploadButton = (Button) findViewById(R.id.upload_button);
        mUploadButton
	    	.setOnClickListener(new OnClickListener()
	    	{
				public void onClick(View arg0)
				{
					chooseFile();
				}
	    	});
        
        mFileUri = (TextView) findViewById(R.id.file_uri);
        if(getIntent().getAction().equals(Intent.ACTION_SEND))
        	setFileUri((Uri)getIntent().getExtras().get(Intent.EXTRA_STREAM));
	}
	
    /**
     * The method called when the upload button is pressed.
     * <p>
     * Invokes OIFileManager to select the file to be uplaoded, or
     * if OIFileManager is not installed it starts the browser
     * to download it.
     */
    public void chooseFile()
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

    /**
     * The method called when the upload button is pressed.
     * <p>
     * Uploads the chosen file.
     */
    public void uploadFile()
    {
    	Intent soundCloudUpload = new Intent(this, SoundCloudService.class);
    	soundCloudUpload.setData(mFile);
    	soundCloudUpload.putExtra("title", mTitleEdit.getText().toString());
		startService(soundCloudUpload);
		finish();    	
    }
    
    /**
     * The method called when the file to be uploaded is selected.
     */
    protected void onActivityResult(int requestCode,
            int resultCode, Intent data)
    {
    	if(data == null)
    		return;
    	
    	mFile = data.getData();
        mFileUri.setText(mFile.toString());

    	mUploadButton
	    	.setOnClickListener(new OnClickListener()
	    	{
	    		public void onClick(View arg0)
	    		{
	    			uploadFile();
	    		}
	    	});
    	mUploadButton.setText("Upload File");
    }
	
    protected void setFileUri(Uri uri)
    {
    	mFile = uri;
    	if(uri != null)
        mFileUri.setText("Chosen file: " + mFile.toString());

    }
	EditText mTitleEdit;
	Button mUploadButton;
	TextView mFileUri;
	Uri mFile; 
}
