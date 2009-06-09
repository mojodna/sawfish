package net.mojodna.sawfish;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;

import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class UpdaterActivity extends OAuthActivity implements OnClickListener,
		OnKeyListener {
	private class UpdateLocationTask extends AsyncTask<String, Void, Void> {

		private String convertStreamToString(InputStream is) {
			/*
			 * To convert the InputStream to String we use the
			 * BufferedReader.readLine() method. We iterate until the
			 * BufferedReader return null which means there's no more data to
			 * read. Each line will appended to a StringBuilder and returned as
			 * String.
			 */
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is), 8192);
			StringBuilder sb = new StringBuilder();

			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return sb.toString();
		}

		@Override
		protected Void doInBackground(String... location) {
			try {
				HttpPost request = new HttpPost(
						"https://fireeagle.yahooapis.com/api/0.1/update");
				StringEntity body = new StringEntity("q="
						+ URLEncoder.encode(location[0], "UTF-8"));
				body.setContentType("application/x-www-form-urlencoded");
				request.setEntity(body);

				getOAuthConsumer().sign(request);

				Log.i("updater", "Sending update request to Fire Eagle...");

				HttpClient httpClient = new DefaultHttpClient();
				HttpResponse response = httpClient.execute(request);

				Log.i("updater", "Response: "
						+ response.getStatusLine().getStatusCode() + " "
						+ response.getStatusLine().getReasonPhrase());
				Log.i("updater", convertStreamToString(response.getEntity()
						.getContent()));
			} catch (OAuthMessageSignerException e) {
				Log.w("updater", e);
			} catch (ClientProtocolException e) {
				Log.w("updater", e);
			} catch (IOException e) {
				Log.w("updater", e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			EditText loc = (EditText) findViewById(R.id.txtLocation);
			loc.setText("");

			dismissDialog(DIALOG_UPDATING);

			Toast toast = Toast.makeText(UpdaterActivity.this,
					R.string.location_updated, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}

		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_UPDATING);
		}
	}

	private static final int DIALOG_UPDATING = 0;

	/**
	 * Clear preferences.
	 */
	private void clearPreferences() {
		Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.clear();
		editor.commit();
	}

	@Override
	public void onClick(View v) {
		Log.i("updater", v + " was clicked.");
		EditText location = (EditText) findViewById(R.id.txtLocation);
		submitLocation(location.getText().toString());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (null != getIntent().getAction()) {
			onNewIntent(getIntent());
		}

		if (isAuthorized()) {
			setContentView(R.layout.main);

			Button update = (Button) findViewById(R.id.btnUpdateLocation);
			update.setOnClickListener(this);

			EditText location = (EditText) findViewById(R.id.txtLocation);
			location.setOnKeyListener(this);
		} else {
			startAuthorization();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;

		switch (id) {
		case DIALOG_UPDATING:
			ProgressDialog updating = new ProgressDialog(this);
			updating.setMessage(getString(R.string.updating));

			dialog = updating;
			break;
		default:
			dialog = null;
		}

		return dialog;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// capture newlines and prevent them from displaying
		if (keyCode == KeyEvent.KEYCODE_ENTER) {

			// submit the location on key up (to avoid duplicate submissions)
			if (event.getAction() == KeyEvent.ACTION_UP) {
				submitLocation(((TextView) v).getText().toString());
			}
			return true;
		}

		return false;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.miReset:
			Log.d("updater", "clearing preferences..");

			clearPreferences();
			startAuthorization();

			return true;
		}
		return false;
	}

	/**
	 * Initiate the authorization process.
	 */
	protected void startAuthorization() {
		finish();

		startActivity(new Intent().setClass(getApplicationContext(),
				AuthorizationActivity.class));
	}

	/**
	 * Submit a location to Fire Eagle
	 * 
	 * @param location
	 *            Location to submit.
	 */
	protected void submitLocation(String location) {
		Log.i("updater", "Location is: " + location);
		new UpdateLocationTask().execute(location);
	}
}
