package net.mgorecki.nearestcity.app;

import net.mgorecki.nearestcity.NearestGeoPoint;
import android.app.Application;
import android.location.Location;

public class NearestApplication extends Application {

	//state of the application:
	NearestGeoPoint lastNGP = null;
	boolean isListenerOn = false;
	Location lastLocation = null;
	String lastToast = null;
	String lastNotification = null;
	double lastNotificationDistance = Double.MAX_VALUE;
	String lastTold = null;

	@Override
	public void onCreate() {
		super.onCreate();
	}
}
