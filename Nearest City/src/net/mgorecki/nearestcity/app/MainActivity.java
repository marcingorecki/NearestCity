package net.mgorecki.nearestcity.app;

import net.mgorecki.nearestcity.GeoPoint;
import net.mgorecki.nearestcity.NearestGeoPoint;
import net.mgorecki.nearestcity.service.GetNearestService;
import net.mgorecki.nearestcity.service.distance.DistanceFormatter;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnInitListener {

	TextView lonEdit;
	TextView latEdit;
	TextView nearestLabel;
	Button onOffButton;
	TextView distance;

	private TextToSpeech talker;

	private static final String ACTIVITY = "MainActivity";
	private static final String LISTENER = "GeoListener";
	private static final double UPDATE_EVERY_METERS = 5;

	private static final int HELLO_ID = 1;

	LocationListener locationListener = new LocationListener() {

		public void onLocationChanged(Location location) {
			findCity(location);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onProviderDisabled(String provider) {
		}

	};

	private String pointName(NearestGeoPoint point) {
		return point.getPoint().getName() + ", " + point.getPoint().getState();
	}

	private void displayLocation(NearestGeoPoint nearest, Location currentLocation) {
		lonEdit.setText(String.valueOf(currentLocation.getLongitude()));
		latEdit.setText(String.valueOf(currentLocation.getLatitude()));
		if(nearest!=null){
			nearestLabel.setText(pointName(nearest));
			distance.setText(DistanceFormatter.prettyMilesFromMeters(nearest.getDistance()));
		}
	}

	private void displayNotification(String text, double distance) {

		boolean newPlace = !text.equals(getMyApplication().lastNotification);
		boolean distanceUpdate = Math.abs(distance - getMyApplication().lastNotificationDistance) > UPDATE_EVERY_METERS;

		if (newPlace || distanceUpdate) {
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

			int icon = R.drawable.ic_launcher;
			CharSequence tickerText = text;
			long when = System.currentTimeMillis();

			Notification notification = new Notification(icon, tickerText, when);

			Context context = getApplicationContext();
			CharSequence contentTitle = "Nearest city " + DistanceFormatter.prettyMilesFromMeters(distance);
			CharSequence contentText = text;
			Intent notificationIntent = new Intent(this, MainActivity.class);
			notificationIntent.setAction(Intent.ACTION_MAIN);
			notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

			mNotificationManager.notify(HELLO_ID, notification);
			getMyApplication().lastNotification = text;
			getMyApplication().lastNotificationDistance = distance;
		} else {
			Log.d(LISTENER, "Skipping notification. " + text + " already displayesd");
		}
	}

	private void talk(String text) {
		if (!text.equals(getMyApplication().lastTold)) {
			talker.speak(text, TextToSpeech.QUEUE_FLUSH, null);
			getMyApplication().lastTold = text;
		}
	}

	private void displayToast(String text) {
		if (!text.equals(getMyApplication().lastToast)) {

			LayoutInflater inflater = getLayoutInflater();
			View layout = inflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_layout_root));

			TextView textView = (TextView) layout.findViewById(R.id.text);
			textView.setText("Nearest city: " + text);

			Toast toast = new Toast(getApplicationContext());
			toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			toast.setDuration(Toast.LENGTH_LONG);
			toast.setView(layout);
			toast.show();

			getMyApplication().lastToast = text;

		} else {
			Log.d(LISTENER, "Skipping toast. " + text + " already displayesd");
		}
	}

	private void findCity(Location location) {

		double lat = location.getLatitude();
		double lon = location.getLongitude();

		Log.d(ACTIVITY, "searching for " + lat + " " + lon);

		GetNearestService gn = new GetNearestService();
		NearestGeoPoint nearest = gn.getNearest(lat, lon);
		GeoPoint city = nearest.getPoint();

		String cityName = pointName(nearest);
		double distance = nearest.getDistance();

		Log.d(ACTIVITY, "got: " + city);

		// update application state
		getMyApplication().lastNGP = nearest;
		getMyApplication().lastLocation = location;

		displayToast(cityName);
		displayNotification(cityName, distance);
		refreshDisplay();
		talk(cityName);
	}

	private void refreshDisplay() {

		// udpate references to components in the view
		lonEdit = (TextView) findViewById(R.id.lonEditText);
		latEdit = (TextView) findViewById(R.id.latEditText);
		nearestLabel = (TextView) findViewById(R.id.nearestCity);
		onOffButton = (Button) findViewById(R.id.getLocation);
		distance = (TextView) findViewById(R.id.distance);

		// get latest location and nearest geo point
		NearestGeoPoint nearest = getMyApplication().lastNGP;
		Location currentLocation = getMyApplication().lastLocation;
		if (currentLocation == null) {
			LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			currentLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
		}
		//update the view
		displayLocation(nearest, currentLocation);
		updateButtonLabel();
	}

	private void updateButtonLabel() {
		Log.d(ACTIVITY, "Updating button state");
		if (getMyApplication().isListenerOn) {
			String msg = getResources().getString(R.string.turnOff);
			onOffButton.setText(msg);
		} else {
			String msg = getResources().getString(R.string.turnOn);
			onOffButton.setText(msg);
		}
	}

	private NearestApplication getMyApplication() {
		return (NearestApplication) getApplication();
	}

	public void getLocation(View target) {
		if (!getMyApplication().isListenerOn) {
			nearestLabel.setText("Waiting for location");
			LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, locationListener);
			getMyApplication().isListenerOn = true;
		} else {
			LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			locationManager.removeUpdates(locationListener);
			getMyApplication().isListenerOn = false;
		}
		updateButtonLabel();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(ACTIVITY, "Orientation changed, redrawing");
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.activity_main);
		refreshDisplay();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		talker = new TextToSpeech(this, this);

		refreshDisplay();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onInit(int arg0) {
		// TODO Auto-generated method stub

	}

}
