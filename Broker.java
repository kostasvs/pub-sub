import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class Broker {

	public static final String REPLY_OK = "OK";
	public static final String REPLY_BLANK_ID = "ID CANNOT BE BLANK";
	public static final String REPLY_BAD_TOPIC = "BAD TOPIC NAME";

	public static final List<String> repliesToAdvanceQueue = Arrays.asList(
			REPLY_OK,
			REPLY_BLANK_ID,
			REPLY_BAD_TOPIC);

	private boolean isValid = false;
	private int publishersPort;
	private int subscribersPort;

	private ServerWrapper serverWrapper;
	private UserInput userInput;

	private Map<Socket, String> subscriberIds = new HashMap<>();
	private Map<Socket, String> publisherIds = new HashMap<>();
	private Map<String, HashSet<String>> subIdsPerTopic = new HashMap<>();

	public Broker(int publishersPort, int subscribersPort) {

		if (!Utils.isValidPort(publishersPort)) {
			Utils.logError("publishers port not provided or invalid");
			return;
		}
		if (!Utils.isValidPort(subscribersPort)) {
			Utils.logError("subscribers port not provided or invalid");
			return;
		}

		this.publishersPort = publishersPort;
		this.subscribersPort = subscribersPort;
		isValid = true;

		serverWrapper = new ServerWrapper(subscribersPort, new SubscriberHandler());
		serverWrapper.start();

		// create user input handler
		var callback = new UserInputCallback();
		userInput = new UserInput(callback);
		userInput.start();
	}

	public static void main(String[] args) {

		// init parameters
		var params = new Params(args);

		// create Broker
		Utils.log("Broker with publishers port " + params.getBrokerPort() +
				" and subscribers port " + params.getSubscribersPort());
		var broker = new Broker(params.getBrokerPort(), params.getSubscribersPort());
		if (!broker.isValid) {
			System.exit(1);
		}
	}

	/**
	 * UserInputCallback
	 */
	public class UserInputCallback implements UserInput.UserInputCallback {

		@Override
		public boolean handleLine(String line) {
			return true;
		}

		@Override
		public void onClose() {
			System.exit(0);
		}
	}

	/**
	 * SubscriberHandler
	 */
	public class SubscriberHandler implements ServerWrapper.SocketHandler {

		@Override
		public void handleAcceptedClient(Socket socket) {
			Utils.log("Subscriber " + socket + " connected");
		}

		@Override
		public void handleReceivedLine(Socket socket, String line) {

			String[] parts = Utils.splitIdCommandPayload(line);
			String fromId = parts[0];
			String cmd = parts[1];
			String payload = parts[2];

			// check given id
			if (fromId.isEmpty()) {
				Utils.logWarn("blank id sent by " + socket);
				serverWrapper.sendLine(socket, REPLY_BLANK_ID);
				return;
			}
			if (!isValidIdForCommand(cmd, fromId, socket)) {
				return;
			}

			switch (cmd) {
				case Subscriber.CMD_ID:
				case Publisher.CMD_ID:
					// Subscriber/Publisher registration
					boolean isSub = cmd.equals(Subscriber.CMD_ID);
					(isSub ? subscriberIds : publisherIds).put(socket, fromId);

					Utils.log("Registered " + (isSub ? "sub" : "pub") + " " + socket + " with id " + fromId);
					break;

				case Subscriber.CMD_SUB:
					// Subscriber topic sub
					if (subToTopic(payload, fromId, socket)) {

						Utils.log(fromId + " subbed to topic " + payload);
						serverWrapper.sendLine(socket, REPLY_OK);
					}
					break;

				case Subscriber.CMD_UNSUB:
					// Subscriber topic unsub
					if (unsubFromTopic(payload, fromId, socket)) {

						Utils.log(fromId + " unsubbed from topic " + payload);
						serverWrapper.sendLine(socket, REPLY_OK);
					}
					break;

				case Publisher.CMD_PUB:
					// Publish message in topic
					parts = Utils.splitTopicMessage(payload);
					if (publishMessage(parts[0], parts[1], socket)) {

						Utils.log(fromId + " published message \"" + parts[1] + "\" in topic " + parts[0]);
						serverWrapper.sendLine(socket, REPLY_OK);
					}
					break;

				default:
					// warn for unexpected message
					Utils.logWarn("Unhandled message from " + socket + ": " + line);
					break;
			}
		}

		@Override
		public void handleDisconnectedClient(Socket socket) {
			Utils.log("Subscriber " + socket + " disconnected");
		}

		@Override
		public void handleServerReady(int port) {
			if (port == subscribersPort) {
				Utils.log("Started listening for subscribers at port " + port);
			} else if (port == publishersPort) {
				Utils.log("Started listening for publishers at port " + port);
			} else {
				Utils.log("Started listening at port " + port);
			}
		}

		/**
		 * Subscribe the subscriber with given id and socket to the given topic.
		 * 
		 * @param topic
		 * @param fromId
		 * @param socket
		 * @return whether successful (even if the subscriber was already subbed to that
		 *         topic)
		 */
		private boolean subToTopic(String topic, String fromId, Socket socket) {

			if (!Utils.isValidTopic(topic)) {
				Utils.logWarn("invalid topic naming sent by subscriber " + socket + ": " + topic);
				serverWrapper.sendLine(socket, REPLY_BAD_TOPIC);
				return false;
			}

			var topicSubIds = subIdsPerTopic.computeIfAbsent(topic, k -> new HashSet<>());
			topicSubIds.add(fromId);
			return true;
		}

		/**
		 * Unsubscribe the subscriber with given id and socket from the given topic.
		 * 
		 * @param topic
		 * @param fromId
		 * @param socket
		 * @return whether successful (even if the subscriber was not subbed to that
		 *         topic)
		 */
		private boolean unsubFromTopic(String topic, String fromId, Socket socket) {

			if (!Utils.isValidTopic(topic)) {
				Utils.logWarn("invalid topic naming sent by subscriber " + socket + ": " + topic);
				serverWrapper.sendLine(socket, REPLY_BAD_TOPIC);
				return false;
			}

			var topicSubIds = subIdsPerTopic.get(topic);
			if (topicSubIds != null) {
				topicSubIds.remove(fromId);
			}
			return true;
		}

		/**
		 * Publish given message, received from given socket, in given topic.
		 * 
		 * @param topic
		 * @param msg
		 * @param socket
		 * @return whether successful
		 */
		private boolean publishMessage(String topic, String msg, Socket socket) {

			if (!Utils.isValidTopic(topic)) {
				Utils.logWarn("invalid topic naming sent by publisher " + socket + ": " + topic);
				serverWrapper.sendLine(socket, REPLY_BAD_TOPIC);
				return false;
			}

			var topicSubIds = subIdsPerTopic.get(topic);
			if (topicSubIds != null) {
				var compositeMsg = Publisher.CMD_PUB + " " + topic + " " + msg;
				for (var sub : subscriberIds.entrySet()) {
					if (!topicSubIds.contains(sub.getValue())) {
						continue;
					}
					serverWrapper.sendLine(sub.getKey(), compositeMsg);
				}
			}
			return true;
		}

		private boolean isValidIdForCommand(String cmd, String fromId, Socket socket) {

			if (Subscriber.CMD_ID.equals(cmd) || Publisher.CMD_ID.equals(cmd)) {
				return true;
			}

			boolean isPubCmd = Publisher.CMD_PUB.equals(cmd);
			var regId = (isPubCmd ? publisherIds : subscriberIds).get(socket);

			if (regId == null) {
				Utils.logWarn("got command " + cmd + " by unregistered " + socket);
				return false;
			}
			if (!regId.equals(fromId)) {
				Utils.logWarn(
						"inconsistent id " + fromId + " given by " + (isPubCmd ? "pub" : "sub") + " with id " + regId);
				return false;
			}
			return true;
		}
	}
}
