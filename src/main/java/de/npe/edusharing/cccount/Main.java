package de.npe.edusharing.cccount;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.rapidoid.http.Req;
import org.rapidoid.log.Log;
import org.rapidoid.setup.On;
import org.rapidoid.u.U;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Main {
	private static Gson gson = new Gson();
	private static Request REQUEST;

	private static AtomicLong requestCount = new AtomicLong();

	public static OkHttpClient client = new OkHttpClient();

	public static void main(String[] args) {
		Log.info("Initializing Service");

		// initialize configuration
		final Config config = Config.from("application.properties");
		if (config == null) {
			// config invalid. exit program
			System.exit(-1);
			return;
		}

		// initialize the search request
		try {
			final File bodyFile = new File("searchbody.json");
			final String bodyJson = new String(Files.readAllBytes(bodyFile.toPath()), StandardCharsets.UTF_8);
			REQUEST = new RequestBuilder(config).post(config.searchPart, bodyJson);
		} catch (final IOException ioe) {
			Log.error("Could not read searchbody from file", ioe);
			System.exit(-1);
			return;
		}

		// initialize service
		On.port(config.servicePort);
		On.get(config.serviceContextPath).json(Main::onCountRequest);
	}

	private static Map<String, Long> onCountRequest(Req req) {
		final long requestId = requestCount.incrementAndGet();
		final String ccType = req.param("type", "cc");
		Log.info("Request [" + requestId + "] from " + req.clientIpAddress() + " for CC-Type: " + ccType);

		final long start = System.currentTimeMillis();
		final Map<String, Long> result = U.map("count", queryCCCount(ccType));
		final long time = System.currentTimeMillis() - start;
		Log.info("Request [" + requestId + "] took " + time + "ms to process");

		return result;
	}

	private static long queryCCCount(String ccType) {
		Objects.requireNonNull(ccType, "ccType was null!");
		try {
			final Response response = client.newCall(REQUEST).execute();
			final int responseCode = response.code();
			final String responseBody = response.body().string();
			if (responseCode == 200) {
				@SuppressWarnings("unchecked")
				final List<Map<String, Object>> facettes = (List<Map<String, Object>>) gson.fromJson(responseBody, Map.class).get("facettes");
				@SuppressWarnings("unchecked")
				final List<Map<String, Object>> ccFacet = (List<Map<String, Object>>) facettes.get(0).get("values");

				return ccFacet.stream()
						.filter(m -> ccType.equals(m.get("value")))
						.map(m -> String.valueOf(m.get("count")))
						.map(Double::valueOf)
						.findFirst().orElse(0.0).longValue();
			} else {
				Log.error("Request returned " + responseCode + ": " + responseBody);
			}
		} catch (final Exception e) {
			Log.error("Request failed", e);
		}
		return -1;
	}
}
