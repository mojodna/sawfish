package net.mojodna.sawfish;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.signature.SignatureMethod;
import android.app.Activity;
import android.content.SharedPreferences;

public abstract class OAuthActivity extends Activity {
	protected static final String ACCESS_TOKEN = "access_token";
	protected static final String ACCESS_TOKEN_SECRET = "access_token_secret";
	protected static final String AUTHORIZED = "authorized";
	protected static final String REQUEST_TOKEN = "request_token";
	protected static final String REQUEST_TOKEN_SECRET = "request_token_secret";

	private OAuthConsumer oauthConsumer;
	private OAuthProvider oauthProvider;

	protected OAuthConsumer getOAuthConsumer() {
		if (null == oauthConsumer) {
			oauthConsumer = new DefaultOAuthConsumer(
					getString(R.string.consumer_key),
					getString(R.string.consumer_secret),
					SignatureMethod.HMAC_SHA1);
			oauthConsumer.setTokenWithSecret(getAccessToken(),
					getAccessTokenSecret());
		}

		return oauthConsumer;
	}

	protected OAuthProvider getOAuthProvider() {
		if (null == oauthProvider) {
			oauthProvider = new DefaultOAuthProvider(getOAuthConsumer(),
					getString(R.string.request_token_url),
					getString(R.string.access_token_url),
					getString(R.string.authorization_url));
		}

		return oauthProvider;
	}

	protected String getAccessToken() {
		return getPreferences(MODE_PRIVATE).getString(ACCESS_TOKEN, null);
	}

	protected String getAccessTokenSecret() {
		return getPreferences(MODE_PRIVATE)
				.getString(ACCESS_TOKEN_SECRET, null);
	}

	@Override
	public SharedPreferences getPreferences(int mode) {
		return getSharedPreferences(getPackageName(), mode);
	}

	protected boolean isAuthorized() {
		return getPreferences(MODE_PRIVATE).getBoolean(AUTHORIZED, false);
	}

}
