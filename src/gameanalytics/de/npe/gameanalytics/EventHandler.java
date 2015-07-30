/**
 * (C) 2015 NPException
 */
package de.npe.gameanalytics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.gson.Gson;

import de.npe.gameanalytics.Analytics.KeyPair;
import de.npe.gameanalytics.events.GAErrorEvent;
import de.npe.gameanalytics.events.GAEvent;
import de.npe.gameanalytics.util.ACLock;


/**
 * @author NPException
 *
 */
final class EventHandler {
	private static boolean init = true;

	private static final Queue<GAEvent> immediateEvents = new ArrayDeque<>(32);
	private static Thread sendImmediateThread;
	private static ACLock immediateEvents_lock = new ACLock(true);
	private static Semaphore sendSemaphore = new Semaphore(0);

	private static ACLock getEventsForGame_lock = new ACLock(true);
	private static ACLock getCategoryEvents_lock = new ACLock(true);
	private static ACLock sendData_lock = new ACLock(true);
	private static ACLock errorSend_lock = new ACLock(true);

	/**
	 * Map containing all not yet sent events.<br>
	 * <br>
	 * Map: KeyPair -> Map: category -> event list
	 */
	private static final Map<KeyPair, Map<String, List<GAEvent>>> events = new HashMap<>(8);

	private static Map<String, List<GAEvent>> getEventsForGame(KeyPair keyPair) {
		try (ACLock acl = getEventsForGame_lock.lockAC()) {
			Map<String, List<GAEvent>> gameEvents = events.get(keyPair);
			if (gameEvents == null) {
				gameEvents = new HashMap<>();
				events.put(keyPair, gameEvents);
			}
			return gameEvents;
		}
	}

	private static List<GAEvent> getCategoryEvents(Map<String, List<GAEvent>> gameEvents, String category) {
		try (ACLock acl = getCategoryEvents_lock.lockAC()) {
			List<GAEvent> categoryEvents = gameEvents.get(category);
			if (categoryEvents == null) {
				categoryEvents = new ArrayList<>(16);
				gameEvents.put(category, categoryEvents);
			}
			return categoryEvents;
		}
	}

	static void add(GAEvent event) {
		try {
			Map<String, List<GAEvent>> gameEvents = getEventsForGame(event.keyPair);
			List<GAEvent> categoryEvents = getCategoryEvents(gameEvents, event.category());
			synchronized (categoryEvents) {
				categoryEvents.add(event);
			}
		} catch (Exception ex) {
			System.err.println("Failed to add GAEvent to event queue: " + event);
			ex.printStackTrace(System.err);
		}
		init();
	}

	static void queueImmediateSend(GAEvent event) {
		boolean added = false;
		try (ACLock acl = immediateEvents_lock.lockAC()) {
			added = immediateEvents.offer(event);
		}

		if (added) {
			sendSemaphore.release(); // increase free permits on semaphore by 1
		} else {
			System.err.println("Could not add event to immediate events queue: " + event);
		}
		init();
	}

	static void sendErrorNow(final GAErrorEvent event, boolean useThread) {
		if (useThread) {
			Thread errorSendThread = new Thread("GA-send-error-now") {
				@Override
				public void run() {
					try (ACLock acl = errorSend_lock.lockAC()) {
						RESTHelper.sendSingleEvent(event);
					}
				}
			};
			errorSendThread.start();
		} else {
			try (ACLock acl = errorSend_lock.lockAC()) {
				RESTHelper.sendSingleEvent(event);
			}
		}
	}

	private static void init() {
		if (!init)
			return;

		synchronized (EventHandler.class) {
			if (!init)
				return;
			init = false;
		}

		final int sleepTime = APIProps.PUSH_INTERVAL_SECONDS * 1000;

		Thread sendThread = new Thread("GA-DataSendThread") {
			@Override
			public void run() {
				while (true) {
					try {
						sleep(sleepTime);
						sendData();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		sendThread.setDaemon(true);
		sendThread.start();

		sendImmediateThread = new Thread("GA-DataSendImmediatelyThread") {
			@Override
			public void run() {
				while (true) {
					sendSemaphore.acquireUninterruptibly(); // try to aquire a permit. will only happen if something is in the queue
					GAEvent event;
					try (ACLock acl = immediateEvents_lock.lockAC()) {
						event = immediateEvents.poll();
					}
					if (event != null) {
						RESTHelper.sendSingleEvent(event);
					} else {
						System.err.println("Immediate event queue did not contain an event. Something released a permit without adding an event first.");
					}
				}
			}
		};
		sendImmediateThread.setDaemon(true);
		sendImmediateThread.start();
	}

	private static void sendData() {
		try (ACLock acl = sendData_lock.lockAC()) {
			Set<KeyPair> keyPairs = events.keySet();
			for (KeyPair keyPair : keyPairs) {
				Map<String, List<GAEvent>> gameEvents = getEventsForGame(keyPair);
				if (gameEvents.isEmpty()) {
					continue;
				}
				List<String> categories = new ArrayList<>(gameEvents.keySet());

				for (String category : categories) {
					// category already exists so we don't need to use the synchronized method.
					List<GAEvent> categoryEvents = gameEvents.get(category);
					List<GAEvent> categoryEventsCopy;

					synchronized (categoryEvents) {
						if (categoryEvents.isEmpty()) {
							continue;
						}
						categoryEventsCopy = new ArrayList<>(categoryEvents);
						categoryEvents.clear();
					}

					RESTHelper.sendData(keyPair, category, categoryEventsCopy);
				}
			}
		}
	}

	private static class RESTHelper {
		private RESTHelper() {}

		private static final Gson gson = new Gson();

		private static final String contentType = "application/json";
		private static final String accept = "application/json";

		static void sendSingleEvent(GAEvent event) {
			try {
				sendData(event.keyPair, event.category(), Arrays.asList(event));
			} catch (Exception e) {
				// System.err.println("Tried to send single event, but failed.");
			}
		}

		static void sendData(KeyPair keyPair, String category, List<GAEvent> events) {
			HttpPost request = createPostRequest(keyPair, category, events);

			try (CloseableHttpClient httpClient = HttpClients.createDefault();
					CloseableHttpResponse response = httpClient.execute(request)) {
				// String responseContent = readResponseContent(response);
				// System.out.println("Sent GA event of category \"" + category + "\", response: " + responseContent);
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}

		private static HttpPost createPostRequest(KeyPair keyPair, String category, List<GAEvent> events) {
			String uri = APIProps.GA_API_URL + APIProps.GA_API_VERSION + "/" + keyPair.gameKey + "/" + category;
			// Create POST request
			HttpPost request = new HttpPost(uri);

			String content = gson.toJson(events);
			byte[] authData = (content + keyPair.secretKey).getBytes();
			String hashedAuthData = DigestUtils.md5Hex(authData);
			request.setHeader("Authorization", hashedAuthData);
			request.setHeader("Accept", accept);

			try {
				// Prepare the request content
				StringEntity entity = new StringEntity(content);
				entity.setContentType(contentType);
				request.setEntity(entity);
			} catch (UnsupportedEncodingException e) {
				// should not happen, I think
			}

			return request;
		}

		/**
		 * This method stays here for a while for testing purposes
		 */
		@SuppressWarnings("unused")
		private static String readResponseContent(HttpResponse response) throws ClientProtocolException, IOException {
			// Read the whole body
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			// Read the info
			StringBuilder sb = new StringBuilder();
			String line = reader.readLine();
			while (line != null) {
				sb.append(line);
				line = reader.readLine();
			}

			return sb.toString();
		}
	}
}
