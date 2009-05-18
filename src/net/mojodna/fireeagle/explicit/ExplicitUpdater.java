package net.mojodna.fireeagle.explicit;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.impl.DefaultOAuthConsumer;
import oauth.signpost.impl.DefaultOAuthProvider;
import oauth.signpost.signature.SignatureMethod;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

// TODO create a menu with the option to clear preferences
public class ExplicitUpdater extends Activity {
	private static final String ACCESS_TOKEN = "access_token";
	private static final String ACCESS_TOKEN_SECRET = "access_token_secret";
	private static final String AUTHORIZED = "authorized";
	private static final String REQUEST_TOKEN = "request_token";
	private static final String REQUEST_TOKEN_SECRET = "request_token_secret";

	private OAuthConsumer oauthConsumer;
	private OAuthProvider oauthProvider;

	// an anonymous OnClickListener for initialization purposes
	private OnClickListener initializeFireEagleListener = new OnClickListener() {
		public void onClick(View v) {
			Log.i("initializeFireEagleListener", "Authorization initiated.");

			// TODO wrap this as an AsyncTask with a progress bar

			if (isAuthorized()) {
				Log.i("OAuth", "Already authorized.");
				return;
			}

			OAuthConsumer consumer = getOAuthConsumer();
			OAuthProvider provider = getOAuthProvider();

			try {
				// get a request token
				String authUrl = provider
						.retrieveRequestToken(getString(R.string.callback_url));

				Log.i("OAuth", "Request token: " + consumer.getToken());
				Log.i("OAuth", "Token secret: " + consumer.getTokenSecret());

				// write these to the preferences
				SharedPreferences prefs = getPreferences(MODE_PRIVATE);
				Editor editor = prefs.edit();
				editor.putString(REQUEST_TOKEN, consumer.getToken());
				editor.putString(REQUEST_TOKEN_SECRET, consumer
						.getTokenSecret());
				editor.commit();

				Log.i("OAuth", "Authorization URL: " + authUrl);

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

	private void clearPreferences() {
		Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.clear();
		editor.commit();
	}

	private OAuthConsumer getOAuthConsumer() {
		if (null == this.oauthConsumer) {
			this.oauthConsumer = new DefaultOAuthConsumer(
					getString(R.string.consumer_key),
					getString(R.string.consumer_secret),
					SignatureMethod.HMAC_SHA1);
		}

		return this.oauthConsumer;
	}

	private OAuthProvider getOAuthProvider() {
		if (null == this.oauthProvider) {
			this.oauthProvider = new DefaultOAuthProvider(getOAuthConsumer(),
					getString(R.string.request_token_url),
					getString(R.string.access_token_url),
					getString(R.string.authorization_url));
		}

		return this.oauthProvider;
	}

	private boolean isAuthorized() {
		return getPreferences(MODE_PRIVATE).getBoolean(AUTHORIZED, false);
	}

	private boolean isAuthorizedCallback(Intent intent) {
		return null != intent.getScheme()
				&& intent.getScheme().equals(getString(R.string.scheme))
				&& null != intent.getData()
				&& intent.getData().getHost().equals("authorized");
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i("callback", "onCreate");
		super.onCreate(savedInstanceState);

		// clearPreferences();

		if (null != getIntent().getAction()) {
			onNewIntent(getIntent());
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.i("callback", "onNewIntent");
		super.onNewIntent(intent);

		// TODO this functionality can be extracted out into a delegate
		Log.i("fireeagle", "Intent received.");
		Log.i("intent", "Action: " + intent.getAction());
		Log.i("intent", "Data: " + intent.getData());
		Log.i("intent", "Scheme: " + intent.getScheme());
		Log.i("intent", "Type: " + intent.getType());
		if (null != intent.getCategories()) {
			Log.i("intent", "Categories: " + intent.getCategories().toString());
		}

		if (isAuthorizedCallback(intent)) {
			// this is a callback
			Uri data = intent.getData();
			Log.i("OAuth", "Token: " + data.getQueryParameter("oauth_token"));

			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			String requestToken = prefs.getString(REQUEST_TOKEN, null);
			String requestTokenSecret = prefs.getString(REQUEST_TOKEN_SECRET,
					null);

			if (null != requestToken && null != requestTokenSecret) {
				// TODO extract into a method
				// complete the OAuth dance
				OAuthConsumer consumer = getOAuthConsumer();
				consumer.setTokenWithSecret(requestToken, requestTokenSecret);
				OAuthProvider provider = getOAuthProvider();

				try {
					provider.retrieveAccessToken();

					Log.i("OAuth", "Access token: " + consumer.getToken());
					Log
							.i("OAuth", "Token secret: "
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

					setContentView(R.layout.main);
				} catch (OAuthExpectationFailedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OAuthMessageSignerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OAuthNotAuthorizedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OAuthCommunicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	protected void onPause() {
		Log.i("callback", "onPause");
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		Log.i("callback", "onPostCreate");
		// TODO Auto-generated method stub
		super.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onPostResume() {
		Log.i("callback", "onPostResume");
		// TODO Auto-generated method stub
		super.onPostResume();
	}

	@Override
	protected void onRestart() {
		Log.i("callback", "onRestart");
		// TODO Auto-generated method stub
		super.onRestart();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.i("callback", "onRestoreInstanceState");
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		Log.i("callback", "onResume");
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i("callback", "onSaveInstanceState");
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStart() {
		Log.i("callback", "onStart");
		super.onStart();

		// this is here rather than in onCreate, as the view change resulting
		// from callbacks is reset when the app is explicitly paused and resumed
		if (isAuthorized()) {
			setContentView(R.layout.main);
		} else {
			setContentView(R.layout.splash);

			// wire up the button to the onClick listener
			Button button = (Button) findViewById(R.id.btnInitializeFireEagle);
			button.setOnClickListener(initializeFireEagleListener);
		}
	}

	@Override
	protected void onStop() {
		Log.i("callback", "onStop");
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onUserLeaveHint() {
		Log.i("callback", "onUserLeaveHint");
		// TODO Auto-generated method stub
		super.onUserLeaveHint();
	}
}