package org.urbanstew.SoundCloudBase;

import org.urbanstew.soundcloudapi.AuthorizationURLOpener;

public interface SoundCloudAuthorizationClient extends AuthorizationURLOpener
{
	enum AuthorizationStatus
	{
		SUCCESSFUL,
		CANCELED,
		FAILED		
	}
	void authorizationCompleted(AuthorizationStatus status);
}
