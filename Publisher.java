import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Publisher {

	public static final String CMD_ID = "pubid";
	public static final String CMD_PUB = "pub";

	private static final List<String> repliesToAdvanceQueue = Arrays.asList(
		Broker.REPLY_OK,
		Broker.REPLY_BLANK_ID,
		Broker.REPLY_BAD_TOPIC
	);

	private boolean isValid = false;
	private String id;
	private String brokerIp;
	private String commandFile = "";
	private int myPort;
	private int brokerPort;

	private ClientWrapper client;

	private String pubAwaitingReply;
	private List<String> pendingPubs = new ArrayList<>();

	public Publisher(String id, int myPort, String brokerIp, int brokerPort, String commandFile) {

		if (id == null || id.trim().isEmpty()) {
			Utils.logError("id not provided or invalid");
			return;
		}
		if (!Utils.isValidPort(myPort)) {
			Utils.logError("port not provided or invalid");
			return;
		}
		if (brokerIp == null || brokerIp.trim().isEmpty()) {
			Utils.logError("broker IP address not provided or invalid");
			return;
		}
		if (!Utils.isValidPort(brokerPort)) {
			Utils.logError("broker port not provided or invalid");
			return;
		}

		this.id = id.trim();
		this.myPort = myPort;
		this.brokerIp = brokerIp.trim();
		this.brokerPort = brokerPort;
		if (commandFile != null && !commandFile.isBlank()) {
			this.commandFile = commandFile.trim();
		}
		isValid = true;

		// connect to broker
		client = new ClientWrapper(this.brokerIp, this.brokerPort, new BrokerHandler(), this.myPort);
		client.start();
	}

	public static void main(String[] args) {

		// init parameters
		var params = new Params(args);

		// create Publisher
		var pub = new Publisher(params.getId(), params.getMyPort(), params.getBrokerIp(),
				params.getBrokerPort(), params.getCommandFile());
		if (!pub.isValid) {
			System.exit(1);
			return;
		}

		// create user input handler
		var callback = pub.new UserInputCallback();
		new UserInput(callback).start();
	}

	/**
	 * UserInputCallback
	 */
	public class UserInputCallback implements UserInput.UserInputCallback {

		@Override
		public void handleLine(String line) {

			if (client == null || line == null || line.isBlank()) {
				return;
			}

			String[] parts = Utils.splitCommandPayload(line);
			String cmd = parts[0];
			String payload = parts[1];

			// currently only pub command is used
			if (!CMD_PUB.equals(cmd)) {
				Utils.logWarn("unrecognized command in user input: " + line);
				return;
			}

			sendPublish(payload, true);
		}

		@Override
		public void onClose() {
			System.exit(0);
		}
	}

	/**
	 * Send a command to the broker to publish the given topic-message packet.
	 * 
	 * @param payload topic-message string (separated by space, topic should be
	 *                single keyword)
	 * @param queued  whether to respect queue of previous publishes pending
	 *                completion
	 */
	private void sendPublish(String payload, boolean queued) {

		String[] parts = Utils.splitTopicMessage(payload);
		String topic = parts[0];
		String msg = parts[1];
		if (!Utils.isValidTopic(topic)) {
			Utils.logWarn("invalid topic naming");
			return;
		}
		if (msg.isEmpty()) {
			Utils.logWarn("publishing empty message");
		}

		String topicMsg = topic + " " + msg;
		if (!queued || pubAwaitingReply == null) {
			pendingPubs.remove(topicMsg);
			pubAwaitingReply = topicMsg;
			client.sendLine(id + " " + CMD_PUB + " " + topicMsg);
		} else {
			pendingPubs.add(topicMsg);
		}
	}

	/**
	 * BrokerHandler
	 */
	public class BrokerHandler implements ClientWrapper.SocketHandler {

		@Override
		public void handleConnected() {
			Utils.log("Connected to broker");
			client.sendLine(id + " " + CMD_ID);
		}

		@Override
		public void handleReceivedLine(String line) {

			if (repliesToAdvanceQueue.contains(line)) {

				handleQueueAdvanceReply(line);
				return;
			}

			Utils.log("Received line: " + line);
		}

		@Override
		public void handleDisconnected() {
			Utils.log("Disconnected from broker");
			System.exit(0);
		}

		private void handleQueueAdvanceReply(String line) {

			if (line.equals(Broker.REPLY_OK)) {
				if (pubAwaitingReply != null) {
					String[] parts = Utils.splitTopicMessage(pubAwaitingReply);
					Utils.printLine("Published msg for topic " + parts[0] + ": " + parts[1]);
				}
			} else {
				Utils.printLine(line);
			}

			pubAwaitingReply = null;
			if (!pendingPubs.isEmpty()) {
				sendPublish(pendingPubs.get(0), true);
			}
		}
	}

	public ClientWrapper getClient() {
		return client;
	}
}
