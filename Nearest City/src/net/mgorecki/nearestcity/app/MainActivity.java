package net.mgorecki.nearestcity.app;

import dalvik.system.VMRuntime;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnInitListener {

	static EditText lonEdit;
	static EditText latEdit;
	static EditText nearestEdit;

	private TextToSpeech talker;

	private static final String ACTIVITY = "MainActivity";
	private static final String LISTENER = "GeoListener";
	private static final double UPDATE_EVERY_METERS = 5;

	private static final int HELLO_ID = 1;

	private static String lastToast = null;
	private static String lastNotification = null;
	private static double lastNotificationDistance = Double.MAX_VALUE;
	private static String lastTold = null;

	LocationListener locationListener = new LocationListener() {

		public void onLocationChanged(Location location) {
			displayLocation(location);
			findCity(location);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onProviderDisabled(String provider) {
		}

	};

	private void displayLocation(Location location) {
		lonEdit.setText(String.valueOf(location.getLongitude()));
		latEdit.setText(String.valueOf(location.getLatitude()));
	}

	private void findCity() {
		float lon = Float.parseFloat(lonEdit.getText().toString());
		float lat = Float.parseFloat(latEdit.getText().toString());

		Location location = new Location(LocationManager.GPS_PROVIDER);
		location.setLatitude(lat);
		location.setLongitude(lon);
	}

	private void displayNotification(String text, double distance) {

		boolean newPlace = !text.equals(MainActivity.lastNotification);
		boolean distanceUpdate = Math.abs(distance - MainActivity.lastNotificationDistance) > UPDATE_EVERY_METERS;

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
			MainActivity.lastNotification = text;
			MainActivity.lastNotificationDistance = distance;
		} else {
			Log.d(LISTENER, "Skipping notification. " + text + " already displayesd");
		}
	}

	private void talk(String text) {
		if (!text.equals(MainActivity.lastTold)) {
			talker.speak(text, TextToSpeech.QUEUE_FLUSH, null);
			MainActivity.lastTold = text;
		}
	}

	private void displayToast(String text) {
		if (!text.equals(MainActivity.lastToast)) {

			LayoutInflater inflater = getLayoutInflater();
			View layout = inflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_layout_root));

			TextView textView = (TextView) layout.findViewById(R.id.text);
			textView.setText("Nearest city: " + text);

			Toast toast = new Toast(getApplicationContext());
			toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			toast.setDuration(Toast.LENGTH_LONG);
			toast.setView(layout);
			toast.show();

			MainActivity.lastToast = text;

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

		String cityName = city.getName() + "," + city.getState();
		double distance = nearest.getDistance();

		Log.d(ACTIVITY, "got: " + city);

		nearestEdit.setText(cityName);
		displayToast(cityName);
		displayNotification(cityName, distance);
		displayLocation(location);
		talk(cityName);
	}

	public void quitClicked(View target) {

	}

	public void turnOffClicked(View target) {
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(locationListener);

	}

	public void getLocation(View target) {
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, locationListener);
		nearestEdit.setText("Waiting for location");
	}

	public void buttonClicked(View target) {
		findCity();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);

		lonEdit = (EditText) findViewById(R.id.lonEditText);
		latEdit = (EditText) findViewById(R.id.latEditText);
		nearestEdit = (EditText) findViewById(R.id.nearestEditText);

		talker = new TextToSpeech(this, this);
		
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if(location!=null){
			displayLocation(location);
		}
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
