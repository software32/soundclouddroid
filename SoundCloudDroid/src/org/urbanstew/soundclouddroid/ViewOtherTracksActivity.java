package org.urbanstew.soundclouddroid;

import android.os.Bundle;

public class ViewOtherTracksActivity extends org.urbanstew.SoundCloudBase.ViewTracksActivity
{
	public void onCreate(Bundle savedInstanceState) {
		
		super.setQueryAndClass("me/favorites", 1);
		super.onCreate(savedInstanceState);
	}
}
