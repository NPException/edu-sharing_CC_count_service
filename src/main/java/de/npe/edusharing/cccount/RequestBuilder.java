package de.npe.edusharing.cccount;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class RequestBuilder {
	private static final MediaType mediaTypeJson = MediaType.parse("application/json; charset=utf-8");

	private final String baseUrl;

	private final String username, password;

	public RequestBuilder(Config config) {
		String url = config.baseUrl;
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		baseUrl = url;
		username = config.username;
		password = config.password;
	}

	public Request post(String restPart, String jsonBody) {
		return base(restPart)
				.addHeader("Content-Type", "application/json")
				.post(RequestBody.create(mediaTypeJson, jsonBody))
				.build();
	}

	public Request get(String restPart) {
		return base(restPart)
				.get()
				.build();
	}

	private Request.Builder base(String restPart) {
		// ensure slash prefix
		restPart = restPart.startsWith("/") ? restPart : "/" + restPart;
		return new Request.Builder()
				.url(baseUrl + restPart)
				.addHeader("Authorization", Credentials.basic(username, password))
				.addHeader("Accept", "application/json");
	}
}
