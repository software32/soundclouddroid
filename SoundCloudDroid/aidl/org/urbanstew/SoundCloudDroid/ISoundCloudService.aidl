// My AIDL file, named SomeClass.aidl
// Note that standard comment syntax is respected.
// Comments before the import or package statements are not bubbled up
// to the generated interface, but comments above interface/method/field
// declarations are added to the generated interface.

// Include your fully-qualified package statement.
package org.urbanstew.SoundCloudDroid;


// Declare the interface.
interface ISoundCloudService
{
	String getUserName();
	int getState();
	boolean obtainRequestToken();
	String getAuthorizeUrl();
	void obtainAccessToken();
}