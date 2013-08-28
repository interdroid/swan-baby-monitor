package interdroid.swan.babymonitor;

import interdroid.swan.ExpressionManager;
import interdroid.swan.swansong.TriState;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;

public class MonitorReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getData().getAuthority().equals("low")) {
			// display or remove low battery notification
			TriState tristate = TriState.valueOf(intent
					.getStringExtra(ExpressionManager.EXTRA_NEW_TRISTATE));
			if (tristate == TriState.TRUE) {
				long timestamp = intent.getLongExtra(
						ExpressionManager.EXTRA_NEW_TRISTATE_TIMESTAMP, 0);

				NotificationManager nm = (NotificationManager) context
						.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification.Builder builder = new Notification.Builder(context)
						.setSmallIcon(R.drawable.ic_launcher)
						.setContentText("Baby Monitor").setAutoCancel(true)
						.setSound(getSound(context)).setWhen(timestamp)
						.setContentTitle("Battery Low!");

				nm.notify(234, builder.build());
			}

		} else if (intent.getData().getAuthority().equals("awake")) {
			// update awake notification
			TriState tristate = TriState.valueOf(intent
					.getStringExtra(ExpressionManager.EXTRA_NEW_TRISTATE));
			if (tristate == TriState.TRUE) {
				long timestamp = intent.getLongExtra(
						ExpressionManager.EXTRA_NEW_TRISTATE_TIMESTAMP, 0);

				NotificationManager nm = (NotificationManager) context
						.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification.Builder builder = new Notification.Builder(context)
						.setSmallIcon(R.drawable.ic_launcher)
						.setLargeIcon(getLargeImage(context))
						.setContentText("Baby Monitor").setAutoCancel(true)
						.setSound(getSound(context)).setWhen(timestamp)
						.setContentTitle("I'm awake!");

				nm.notify(123, builder.build());
			}

		}

	}

	private Bitmap getLargeImage(Context context) {
		String uriString = PreferenceManager.getDefaultSharedPreferences(
				context).getString("image", null);
		if (uriString != null) {
			Uri uri = Uri.parse(uriString);
			InputStream in;
			try {
				in = context.getContentResolver().openInputStream(uri);
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 8;
				Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);

				in.close();
				return bitmap;
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
		}
		System.out.println("uriString is null");
		return null;
	}

	private Uri getSound(Context context) {
		String uriString = PreferenceManager.getDefaultSharedPreferences(
				context).getString("awake", null);
		if (uriString != null) {
			return Uri.parse(uriString);
		}
		System.out.println("uriString is null");
		return null;
	}

}
