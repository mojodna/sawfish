package net.mojodna.fireeagle.explicit;

import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

public class ExplicitUpdater extends OAuthActivity {
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
}
