package de.npe.edusharing.cccount;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.rapidoid.log.Log;

import okhttp3.Response;

public class Config {
	private static final String REST_ENDPOINT = "edusharing.rest.endpoint";
	private static final String USERNAME = "edusharing.rest.username";
	private static final String PASSWORD = "edusharing.rest.password";
	private static final String SEARCH_PART = "edusharing.rest.search";

	private static final String SERVICE_PORT = "service.port";
	private static final String SERVICE_CONTEXT_PATH = "service.context.path";

	public final String baseUrl;
	public final String username;
	public final String password;
	public final String searchPart;

	public final int servicePort;
	public final String serviceContextPath;

	private Config(String baseUrl, String username, String password, String searchPart, int servicePort, String serviceContextPath) {
		this.baseUrl = baseUrl;
		this.username = username;
		this.password = password;
		this.searchPart = searchPart;
		this.servicePort = servicePort;
		this.serviceContextPath = serviceContextPath;
	}

	public static Config from(String file) {
		Log.info("Loading config from: " + file);
		final Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(file)) {
			props.load(fis);
		} catch (final IOException ioe) {
			Log.error("Could not load config", ioe);
			return null;
		}

		// attempt to verify the correctness of the properties file (at least partially)
		Log.info("Verifying configuration");

		final String baseUrl = props.getProperty(REST_ENDPOINT);
		try {
			new URL(baseUrl);
		} catch (final MalformedURLException mue) {
			Log.error("Value of [" + REST_ENDPOINT + "] is not a valid URL", mue);
			return null;
		}

		final String username = props.getProperty(USERNAME);
		if (username == null) {
			Log.error("No value for [" + USERNAME + "]");
			return null;
		}

		final String password = props.getProperty(PASSWORD);
		if (password == null) {
			Log.error("No value for [" + PASSWORD + "]");
			return null;
		}

		final String searchPart = props.getProperty(SEARCH_PART);
		if (searchPart == null) {
			Log.error("No value for [" + SEARCH_PART + "]");
			return null;
		}

		int port = -1;
		try {
			port = Integer.parseInt(props.getProperty(SERVICE_PORT));
		} catch (final Exception e) {
			Log.error("Could not parse int from [" + SERVICE_PORT + "]");
		}
		if (port < 0 || port > 65535) {
			Log.error("Invalid port number: " + port);
			return null;
		}

		String contextPath = props.getProperty(SERVICE_CONTEXT_PATH);
		if (contextPath == null) {
			Log.error("No value for [" + SERVICE_CONTEXT_PATH + "]");
			return null;
		}
		contextPath = contextPath.startsWith("/") ? contextPath : "/" + contextPath;

		final Config config = new Config(baseUrl, username, password, searchPart, port, contextPath);

		// test availability with loaded config
		final String testCallPath = "/_about";
		try (final Response response = Main.client.newCall(new RequestBuilder(config).get(testCallPath)).execute()) {
			final int responseCode = response.code();
			if (responseCode == 200) {
				Log.info("Successfully called " + testCallPath);
			} else {
				Log.error("Testing call to edu-sharing REST-API " + testCallPath + " failed. Response body: " + response.body().string());
				return null;
			}
		} catch (final IOException ioe) {
			Log.error("Could not execute call to edu-sharing REST-API", ioe);
			return null;
		}

		return config;
	}
}
