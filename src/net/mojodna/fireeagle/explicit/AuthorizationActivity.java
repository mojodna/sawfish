package net.mojodna.fireeagle.explicit;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class AuthorizationActivity extends OAuthActivity {
	// an anonymous OnClickListener for initialization purposes
	private OnClickListener initializeFireEagleListener = new OnClickListener() {
		public void onClick(View v) {
			Log.i("initializeFireEagleListener", "Authorization initiated.");

			// TODO wrap this as an AsyncTask with a progress bar

			if (isAuthorized()) {
				Log.d("authorization", "Already authorized.");
				onAuthorizationCompleted();
				return;
			}

			OAuthConsumer consumer = getOAuthConsumer();
			OAuthProvider provider = getOAuthProvider();

			try {
				// get a request token
				String authUrl = provider
						.retrieveRequestToken(getString(R.string.callback_url));

				Log.d("authorization", "Request token: " + consumer.getToken());
				Log.d("authorization", "Token secret: "
						+ consumer.getTokenSecret());

				// write these to the preferences
				SharedPreferences prefs = getPreferences(MODE_PRIVATE);
				Editor editor = prefs.edit();
				editor.putString(REQUEST_TOKEN, consumer.getToken());
				editor.putString(REQUEST_TOKEN_SECRET, consumer
						.getTokenSecret());
				editor.commit();

				Log.d("authorization", "Authorization URL: " + authUrl);

				// open a browser window to handle authorization
				startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri
						.parse(authUrl)));

			} catch (OAuthExpectationFailedException e) {
				// TODO give OAuth exceptions a common parent
				e.printStackTrace();
			} catch (OAuthMessageSignerException e) {
				e.printStackTrace();
			} catch (OAuthNotAuthorizedException e) {
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				e.printStackTrace();
			}
		}
	};

	/**
	 * The authorization has been completed.
	 */
	private void onAuthorizationCompleted() {
		// our work here is done; pass control back to the initiating activity
		finish();

		// TODO figure out a way to avoid hardcoding the Activity to return to
		startActivity(new Intent().setClass(this, ExplicitUpdater.class));
	}

	/**
	 * Is this an authorized callback?
	 * 
	 * @param intent
	 *            Intent under examination.
	 * @return whether this is intended as a post-authorization callback.
	 */
	private boolean isAuthorizedCallback(Intent intent) {
		return null != intent.getScheme()
				&& intent.getScheme().equals(getString(R.string.scheme))
				&& null != intent.getData()
				&& intent.getData().getHost().equals("authorized");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (null != getIntent().getAction()) {
			onNewIntent(getIntent());
		}

		setContentView(R.layout.splash);

		// wire up the button to the onClick listener
		Button button = (Button) findViewById(R.id.btnInitializeFireEagle);
		button.setOnClickListener(initializeFireEagleListener);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (isAuthorizedCallback(intent)) {
			// this is a callback
			Uri data = intent.getData();
			Log.d("authorization", "Token: "
					+ data.getQueryParameter("oauth_token"));

			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			String requestToken = prefs.getString(REQUEST_TOKEN, null);
			String requestTokenSecret = prefs.getString(REQUEST_TOKEN_SECRET,
					null);

			// TODO compare the `oauth_token` query parameter with the stored
			// request token

			if (null != requestToken && null != requestTokenSecret) {
				// TODO extract into an AsyncTask
				// complete the OAuth dance
				OAuthConsumer consumer = getOAuthConsumer();
				consumer.setTokenWithSecret(requestToken, requestTokenSecret);
				OAuthProvider provider = getOAuthProvider();

				try {
					provider.retrieveAccessToken();

					Log.d("authorization", "Access token: "
							+ consumer.getToken());
					Log.d("authorization", "Token secret: "
							+ consumer.getTokenSecret());

					// replace the preferences
					Editor editor = prefs.edit();
					editor.remove(REQUEST_TOKEN);
					editor.remove(REQUEST_TOKEN_SECRET);
					editor.putString(ACCESS_TOKEN, consumer.getToken());
					editor.putString(ACCESS_TOKEN_SECRET, consumer
							.getTokenSecret());
					editor.putBoolean(AUTHORIZED, true);
					editor.commit();

					onAuthorizationCompleted();
				} catch (OAuthExpectationFailedException e) {
					e.printStackTrace();
				} catch (OAuthMessageSignerException e) {
					e.printStackTrace();
				} catch (OAuthNotAuthorizedException e) {
					e.printStackTrace();
				} catch (OAuthCommunicationException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
