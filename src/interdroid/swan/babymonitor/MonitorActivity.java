package interdroid.swan.babymonitor;

import interdroid.swan.ExpressionManager;
import interdroid.swan.SwanException;
import interdroid.swan.swansong.ExpressionFactory;
import interdroid.swan.swansong.ExpressionParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.RingtonePreference;

public class MonitorActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static final int REQUEST_IMAGE = 1234;

	private BroadcastReceiver mNameReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			List<String> names = intent.getStringArrayListExtra("names");
			((ListPreference) findPreference("swan_location")).setEntries(names
					.toArray(new String[names.size()]));
			((ListPreference) findPreference("swan_location"))
					.setEntryValues(names.toArray(new String[names.size()]));
		}

	};

	@Override
	protected void onResume() {
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		registerReceiver(mNameReceiver, new IntentFilter(
				"interdroid.swan.NAMES"));
		sendBroadcast(new Intent("interdroid.swan.GET_NAMES"));
		setupPrefs(getPreferenceScreen());
		super.onResume();
	}

	@Override
	protected void onPause() {
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		unregisterReceiver(mNameReceiver);
		super.onPause();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.default_preferences);

		((CheckBoxPreference) findPreference("start"))
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						if ((Boolean) newValue) {
							// start monitoring
							startMonitoring();
						} else {
							stopMonitoring();
							// stop monitoring
						}
						return true;
					}
				});

		findPreference("image").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						Intent pickIntent = new Intent(
								Intent.ACTION_GET_CONTENT,
								android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
						pickIntent.setType("image/*");
						startActivityForResult(pickIntent, REQUEST_IMAGE);
						return true;
					}
				});
	}

	private void setupPrefs(PreferenceGroup group) {
		for (int i = 0; i < group.getPreferenceCount(); i++) {
			if (group.getPreference(i) instanceof PreferenceGroup) {
				setupPrefs((PreferenceGroup) group.getPreference(i));
			} else {
				Object value = group.getPreference(i).getSharedPreferences()
						.getAll().get(group.getPreference(i).getKey());
				if (value != null) {
					onSharedPreferenceChanged(group.getPreference(i)
							.getSharedPreferences(), group.getPreference(i)
							.getKey());
				}
				if (group.getPreference(i) instanceof RingtonePreference) {
					// ugly hack:
					// ringtone preferences do not call
					// onsharedpreferencechanged by default, so forward it in
					// the onpreferencechangelistener, but delay, since this is
					// invoked before the change is made.
					group.getPreference(i).setOnPreferenceChangeListener(
							new OnPreferenceChangeListener() {

								@Override
								public boolean onPreferenceChange(
										final Preference preference,
										Object newValue) {
									new Thread() {
										public void run() {
											try {
												sleep(100);
											} catch (InterruptedException e) {
											}
											onSharedPreferenceChanged(
													preference
															.getSharedPreferences(),
													preference.getKey());
										}
									}.start();

									return true;
								}
							});
				}
			}
		}
	}

	private void startMonitoring() {
		stopMonitoring();
		try {
			SharedPreferences prefs = getPreferenceScreen()
					.getSharedPreferences();
			// String babyLocation = prefs.getString("swan_location", "self");
			String babyLocation = "self";
			double threshold = Double.parseDouble(prefs.getString("threshold",
					"10"));
			String awake = "(" + babyLocation + "@sound:db?audio_source="
					+ MediaRecorder.AudioSource.MIC
					+ "&sample_rate=44100&channel_config="
					+ AudioFormat.CHANNEL_IN_MONO + "&audio_format="
					+ AudioFormat.ENCODING_PCM_16BIT
					+ "&sample_interval=2000 {MEAN,5000} - " + babyLocation
					+ "@sound:db?audio_source=" + MediaRecorder.AudioSource.MIC
					+ "&sample_rate=44100&channel_config="
					+ AudioFormat.CHANNEL_IN_MONO + "&audio_format="
					+ AudioFormat.ENCODING_PCM_16BIT
					+ "&sample_interval=2000 {MEAN,60000}) > " + threshold;
			System.out.println("awake: " + awake);

			String lowBattery = babyLocation + "@battery:level < 10";

			ExpressionManager.registerExpression(this, "awake",
					ExpressionFactory.parse(awake), null);
			ExpressionManager.registerExpression(this, "low",
					ExpressionFactory.parse(lowBattery), null);
		} catch (SwanException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExpressionParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void stopMonitoring() {
		ExpressionManager.unregisterExpression(this, "awake");
		ExpressionManager.unregisterExpression(this, "low");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQUEST_IMAGE:
				getPreferenceScreen().getSharedPreferences().edit()
						.putString("image", data.getDataString()).commit();

				break;

			default:
				break;
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (findPreference(key) instanceof RingtonePreference) {
			String value = sharedPreferences.getString(key, null);
			if (value != null) {
				Ringtone ringtone = RingtoneManager.getRingtone(this,
						Uri.parse(value));
				findPreference(key).setSummary(ringtone.getTitle(this));
			}
		} else if (findPreference(key).getKey().equals("image")) {

		} else {
			findPreference(key).setSummary(
					"" + sharedPreferences.getAll().get(key));
		}
	}

}
