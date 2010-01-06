package org.urbanstew.SoundCloudDroid;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.AdapterView.OnItemSelectedListener;

public class UploadActivity extends Activity
{
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.upload);
        
    	mUploadIntent = new Intent(this, SoundCloudService.class);

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
        
        mTextAttribute = (EditText) findViewById(R.id.text_attribute);
        mSpinnerAttribute = (Spinner) findViewById(R.id.spinner_attribute);
        mAnimator = (ViewAnimator) findViewById(R.id.animator);
        	
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.attributes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mExtraAttribute = (Spinner) findViewById(R.id.extra_attributes);
        mExtraAttribute.setAdapter(adapter);
        mExtraAttribute.setOnItemSelectedListener(new OnItemSelectedListener()
        {
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long id)
			{
				commitSelectedAttribute();
				switch(position)
				{
				case 0:
					selectedSpinnerAttribute("sharing", R.array.sharing_options);
					break;
				case 1:
					selectedTextAttribute("description");
					break;
				case 2:
					selectedTextAttribute("genre");
					break;
				case 3:
					selectedSpinnerAttribute("track_type", R.array.track_type_options);
				}
				mLastExtraAttributePosition = position;
			}

			public void onNothingSelected(AdapterView<?> arg0)
			{
				commitSelectedAttribute();
			}
        });
        
        mFileUri = (TextView) findViewById(R.id.file_uri);
        if(getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SEND) && getIntent().getExtras().containsKey(Intent.EXTRA_STREAM))
        	setFileUri((Uri)getIntent().getExtras().get(Intent.EXTRA_STREAM));
	}
	
	void commitSelectedAttribute()
	{
		switch(mLastExtraAttributePosition)
		{
		case 0:
			mUploadIntent.putExtra("sharing", (CharSequence)mSpinnerAttribute.getSelectedItem());
			break;
		case 1:
			mUploadIntent.putExtra("description", mTextAttribute.getText().toString());
			break;
		case 2:
			mUploadIntent.putExtra("genre", mTextAttribute.getText().toString());
			break;
		case 3:
			mUploadIntent.putExtra("track_type", (CharSequence)mSpinnerAttribute.getSelectedItem());
			break;					
		}
		mLastExtraAttributePosition = Spinner.INVALID_POSITION;
	}
	void selectedTextAttribute(String parameter)
	{
		if(mUploadIntent.hasExtra(parameter))
			mTextAttribute.setText(mUploadIntent.getStringExtra(parameter));
		else
			mTextAttribute.setText("");
		mAnimator.setDisplayedChild(1); // text
	}
	
	void selectedSpinnerAttribute(String parameter, int options)
	{
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerAttribute.setAdapter(adapter);
		mAnimator.setDisplayedChild(0); // spinner
		if(mUploadIntent.hasExtra(parameter))
			for(int i=0; i<adapter.getCount(); i++)
				if(mUploadIntent.getStringExtra(parameter).equals(adapter.getItem(i)))
				{
					mSpinnerAttribute.setSelection(i);
					break;
				}
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
    	commitSelectedAttribute();
    	mUploadIntent.setData(mFile);
    	mUploadIntent.putExtra("title", mTitleEdit.getText().toString());
		startService(mUploadIntent);
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
    	
    	setFileUri(data.getData());
    }
	
    protected void setFileUri(Uri uri)
    {
    	mFile = uri;
    	if(uri != null)
    	{
    		mFileUri.setText("Chosen file: " + mFile.toString());
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
    }
    
    Intent mUploadIntent;
    
    int mLastExtraAttributePosition = Spinner.INVALID_POSITION;
	EditText mTitleEdit, mTextAttribute;
	Button mUploadButton;
	TextView mFileUri;
	Spinner mExtraAttribute, mSpinnerAttribute;
	ViewAnimator mAnimator;
	Uri mFile; 
}
