package net.mojodna.fireeagle.explicit;

public class OAuthToken {
	private String token;
	private String secret;

	public OAuthToken(String token, String secret) {
		this.token = token;
		this.secret = secret;
	}

	public String getToken() {
		return token;
	}

	public String getSecret() {
		return secret;
	}
}
