package net.mojodna.sawfish;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class AuthorizationActivity extends OAuthActivity implements
		OnClickListener {
	private class RequestTokenRetrievalTask extends
			AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			try {
				// get a request token
				String authUrl = getOAuthProvider().retrieveRequestToken(
						getString(R.string.callback_url));

				Log.d("authorization", "Request token: "
						+ getOAuthConsumer().getToken());
				Log.d("authorization", "Token secret: "
						+ getOAuthConsumer().getTokenSecret());

				// write these to the preferences
				SharedPreferences prefs = getPreferences(MODE_PRIVATE);
				Editor editor = prefs.edit();
				editor.putString(REQUEST_TOKEN, getOAuthConsumer().getToken());
				editor.putString(REQUEST_TOKEN_SECRET, getOAuthConsumer()
						.getTokenSecret());
				editor.commit();

				Log.d("authorization", "Authorization URL: " + authUrl);

				return authUrl;
			} catch (OAuthExpectationFailedException e) {
				// TODO give OAuth exceptions a common parent
				Log.w("authorization", e);
			} catch (OAuthMessageSignerException e) {
				Log.w("authorization", e);
			} catch (OAuthNotAuthorizedException e) {
				Log.w("authorization", e);
			} catch (OAuthCommunicationException e) {
				Log.w("authorization", e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(String authUrl) {
			dismissDialog(DIALOG_INITIATING_AUTHORIZATION);

			if (null != authUrl) {
				Toast toast = Toast.makeText(AuthorizationActivity.this,
						R.string.authorization_redirect, Toast.LENGTH_LONG);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();

				// open a browser window to handle authorization
				startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri
						.parse(authUrl)));

				finish();
			} else {
				Toast toast = Toast.makeText(AuthorizationActivity.this,
						R.string.authorization_failed, Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
			}
		}

		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_INITIATING_AUTHORIZATION);
		}
	}

	private class TokenExchangeTask extends AsyncTask<Object, Void, OAuthToken> {

		@Override
		protected OAuthToken doInBackground(Object... params) {
			// complete the OAuth dance
			OAuthConsumer consumer = getOAuthConsumer();
			OAuthToken token = (OAuthToken) params[0];
			consumer.setTokenWithSecret(token.getToken(), token.getSecret());
			OAuthProvider provider = getOAuthProvider();

			try {
				provider.retrieveAccessToken((String) params[1]);

				Log.d("authorization", "Access token: " + consumer.getToken());
				Log.d("authorization", "Token secret: "
						+ consumer.getTokenSecret());

				return new OAuthToken(consumer.getToken(), consumer
						.getTokenSecret());
			} catch (OAuthExpectationFailedException e) {
				e.printStackTrace();
			} catch (OAuthMessageSignerException e) {
				e.printStackTrace();
			} catch (OAuthNotAuthorizedException e) {
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(OAuthToken token) {
			// replace the preferences
			Editor editor = getPreferences(MODE_PRIVATE).edit();
			editor.remove(REQUEST_TOKEN);
			editor.remove(REQUEST_TOKEN_SECRET);
			editor.putString(ACCESS_TOKEN, token.getToken());
			editor.putString(ACCESS_TOKEN_SECRET, token.getSecret());
			editor.putBoolean(AUTHORIZED, true);
			editor.commit();

			onAuthorizationCompleted();

			dismissDialog(DIALOG_COMPLETING_AUTHORIZATION);

			Toast toast = Toast.makeText(AuthorizationActivity.this,
					R.string.authorization_completed, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}

		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_COMPLETING_AUTHORIZATION);
		}
	}

	private static final int DIALOG_COMPLETING_AUTHORIZATION = 1;

	private static final int DIALOG_INITIATING_AUTHORIZATION = 0;

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

	/**
	 * The authorization has been completed.
	 */
	private void onAuthorizationCompleted() {
		// our work here is done; pass control back to the initiating activity
		finish();

		// TODO figure out a way to avoid hardcoding the Activity to return to
		startActivity(new Intent().setClass(getApplicationContext(),
				UpdaterActivity.class));
	}

	public void onClick(View v) {
		Log.i("authorization", "Authorization initiated.");

		if (isAuthorized()) {
			Log.d("authorization", "Already authorized.");
			onAuthorizationCompleted();
			return;
		}

		new RequestTokenRetrievalTask().execute();
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
		button.setOnClickListener(this);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;

		switch (id) {
		case DIALOG_INITIATING_AUTHORIZATION:
			ProgressDialog initiating = new ProgressDialog(this);
			initiating.setMessage(getString(R.string.initiating_authorization));

			dialog = initiating;
			break;
		case DIALOG_COMPLETING_AUTHORIZATION:
			ProgressDialog completing = new ProgressDialog(this);
			completing.setMessage(getString(R.string.completing_authorization));

			dialog = completing;
			break;
		default:
			dialog = null;
		}

		return dialog;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (isAuthorizedCallback(intent)) {
			// this is a callback
			Uri data = intent.getData();
			Log.d("authorization", "Token: "
					+ data.getQueryParameter(OAuth.OAUTH_TOKEN));

			SharedPreferences prefs = getPreferences(MODE_PRIVATE);
			String requestToken = prefs.getString(REQUEST_TOKEN, null);
			String requestTokenSecret = prefs.getString(REQUEST_TOKEN_SECRET,
					null);

			// TODO compare the `oauth_token` query parameter with the stored
			// request token

			if (null != requestToken && null != requestTokenSecret) {
				new TokenExchangeTask().execute(new OAuthToken(requestToken,
						requestTokenSecret), data
						.getQueryParameter(OAuth.OAUTH_VERIFIER));
			}
		}
	}
}
