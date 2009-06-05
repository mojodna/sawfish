package net.mojodna.fireeagle.explicit;

import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ExplicitUpdater extends OAuthActivity implements OnClickListener,
		OnKeyListener {
	/**
	 * Clear preferences.
	 */
	private void clearPreferences() {
		Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.clear();
		editor.commit();
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
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i("updater", "clearing preferences..");

		clearPreferences();
		startAuthorization();

		return false;
	}

	/**
	 * Initiate the authorization process.
	 */
	protected void startAuthorization() {
		startActivity(new Intent().setClass(this, AuthorizationActivity.class));
	}

	/**
	 * Submit a location to Fire Eagle
	 * 
	 * @param location
	 *            Location to submit.
	 */
	protected void submitLocation(String location) {
		Log.i("updater", "Location is: " + location);
	}

	@Override
	public void onClick(View v) {
		Log.i("updater", v + " was clicked.");
		EditText location = (EditText) findViewById(R.id.txtLocation);
		submitLocation(location.getText().toString());
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
}
