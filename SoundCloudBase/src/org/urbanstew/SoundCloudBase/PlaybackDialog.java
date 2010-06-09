package org.urbanstew.SoundCloudBase;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class PlaybackDialog
{
	PlaybackDialog(Activity activity)
	{
		mActivity = activity;
		
		mPlayTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		// construct playback dialog
        LayoutInflater factory = LayoutInflater.from(activity);
        View playbackView = factory.inflate(R.layout.alert_playback_dialog, null);
        mPlaybackDialog = new AlertDialog.Builder(activity)
            .setView(playbackView)
            .setPositiveButton
            (
            	"Close",
            	new DialogInterface.OnClickListener()
            	{
            		public void onClick(DialogInterface dialog, int whichButton)
            		{
            			releasePlayer();
                	}
            	}
            )
            .setOnCancelListener(new DialogInterface.OnCancelListener()
            {
				public void onCancel(DialogInterface dialog)
				{
					releasePlayer();
				}
            })
            .create();
        mPlaybackDialog.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mPlayPauseButton = (ImageButton)playbackView.findViewById(R.id.playback_pause);
        mPlaybackCurrentTime = (TextView)playbackView.findViewById(R.id.playback_current_time);
        mPlaybackFileSize = (TextView)playbackView.findViewById(R.id.playback_file_size);
        mPlaybackDuration  = (TextView)playbackView.findViewById(R.id.playback_length);
        mSeekBar = (SeekBar)playbackView.findViewById(R.id.playback_seek);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			public void onProgressChanged(SeekBar bar, int progress, boolean fromUser)
			{
				if(fromUser && mPlayer != null)
						mPlayer.seekTo(mPlayer.getDuration() * progress / 1024);				
			}

			public void onStartTrackingTouch(SeekBar arg0)
			{
				if(mPlayer.isPlaying())
				{
					mPlayer.pause();
			    	setPlayPauseButton(android.R.drawable.ic_media_play);
				}
			}

			public void onStopTrackingTouch(SeekBar bar)
			{
				mPlayer.start();
		    	setPlayPauseButton(android.R.drawable.ic_media_pause);
			}
        
        });
        playbackView.findViewById(R.id.playback_pause).setOnClickListener
        (
        	new OnClickListener()
        	{
				public void onClick(View view)
				{
					if(mPlayer.isPlaying())
					{
						mPlayer.pause();
				    	setPlayPauseButton(android.R.drawable.ic_media_play);
					}
					else
					{
						mPlayer.start();
				    	setPlayPauseButton(android.R.drawable.ic_media_pause);
					}
				}
        	}
        );
/*        playbackView.findViewById(R.id.playback_next).setOnClickListener
        (
        	new OnClickListener()
        	{
				public void onClick(View view)
				{
					if(mAnnotationsCursor.getCount() > mPlayingPosition + 1)
						playItem(mPlayingPosition + 1);
				}
        	}
        );
        playbackView.findViewById(R.id.playback_previous).setOnClickListener
        (
        	new OnClickListener()
        	{
				public void onClick(View view)
				{
					if((mAnnotationsCursor.getCount() > mPlayingPosition - 1) && mPlayingPosition > 0)
						playItem(mPlayingPosition - 1);
				}
        	}
        );*/
	}
	
	public void onPause()
	{
		mCurrentTimeTask.cancel();
	}
	
	public void onDestroy()
	{
		mTimer.cancel();
		if(mPlaybackDialog.isShowing())
			mPlaybackDialog.dismiss();
		releasePlayer();
	}
	
	public void onResume()
	{
    	mCurrentTimeTask = new TimerTask()
    	{
    		public void run()
    		{
    			mActivity.runOnUiThread(new Runnable()
    			{
    				public void run()
    				{
    					if(mPlayer != null && mPlayer.isPlaying())
    					{
    						updateProgressDisplay();
    					}
    				}
    			});                                
    		}
    	};
		mTimer.scheduleAtFixedRate(
				mCurrentTimeTask,
				0,
				100);
	}
	
	public void releasePlayer()
	{
		if(mPlayer!=null)
		{
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}
	}
	
    void updateProgressDisplay()
    {
    	String timeText;

    	if(mPlayerIsPrepared)
    	{
			timeText = mPlayTimeFormatter.format(mPlayer.getCurrentPosition());	
			mSeekBar.setProgress((1024 * mPlayer.getCurrentPosition()) / mPlayer.getDuration());
    	}
    	else
    	{
    		timeText = mPlayTimeFormatter.format(0);
    		mSeekBar.setProgress(0);
    	}
		mPlaybackCurrentTime.setText(timeText);

    }
    
	void setPlayPauseButton(int id)
	{
		mPlayPauseButton.setImageDrawable(mActivity.getResources().getDrawable(id));		
	}
	
    public void displayPlaybackDialog(String streamUrl)
    {
    	if(mPlayer != null)
    		mPlayer.release();
    	
	    mPlayer = new MediaPlayer();
	    mPlayerIsPrepared = false;
	    try
		{
			mPlayer.setDataSource(streamUrl); // throws
			
	  		setPlayPauseButton(android.R.drawable.ic_media_pause);
	  		mPlayPauseButton.setEnabled(false);
	   		mPlaybackDialog.show();
	   		mPlaybackDuration.setText("opening");
			mSeekBar.setEnabled(false);
			updateProgressDisplay();
			
	    	mPlayer.prepareAsync();
	    	mPlayer.setOnPreparedListener(new OnPreparedListener()
		   	{
				public void onPrepared(MediaPlayer mp)
				{
					mPlayerIsPrepared = true;
					mPlaybackDuration.setText(mPlayTimeFormatter.format(mPlayer.getDuration()));
			   		updateProgressDisplay();
					mp.start();
					mSeekBar.setEnabled(true);
			  		mPlayPauseButton.setEnabled(true);
				}
		   	});
	    	mPlayer.setOnErrorListener(new OnErrorListener()
	    	{
				public boolean onError(MediaPlayer mp, int what, int extra)
				{
					if(what==MediaPlayer.MEDIA_ERROR_SERVER_DIED)
						mPlaybackDuration.setText("server died");
					else
						mPlaybackDuration.setText("error");
					return false;
				}
	    	});

		} catch (IllegalArgumentException e)
		{
			mPlayer.release();
			e.printStackTrace();
		} catch (IllegalStateException e)
		{
			// this should not happen
			mPlayer.release();
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			mPlayer.release();
			e.printStackTrace();
		}
    }

	Activity mActivity;
	
    MediaPlayer mPlayer = null;
	boolean mPlayerIsPrepared = false;

	// Playback dialog views
    SeekBar mSeekBar;
    TextView mPlaybackCurrentTime, mPlaybackDuration, mPlaybackFileSize;

    ImageButton mPlayPauseButton;
    
    SimpleDateFormat mPlayTimeFormatter = new SimpleDateFormat("HH:mm:ss");

	private AlertDialog mPlaybackDialog;

	private TimerTask mCurrentTimeTask;

	private Timer mTimer = new Timer();
}
