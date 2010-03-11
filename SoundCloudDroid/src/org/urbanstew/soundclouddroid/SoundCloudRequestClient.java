package org.urbanstew.soundclouddroid;

import org.apache.http.HttpResponse;

public interface SoundCloudRequestClient
{
	public void requestCompleted(HttpResponse response);
	public void requestFailed(Exception e);
}
