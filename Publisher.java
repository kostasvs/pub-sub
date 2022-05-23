public class Publisher {

	public static final String CMD_ID = "pubid";
	public static final String CMD_PUB = "pub";

	private boolean isValid = false;
	private String id;
	private String brokerIp;
	private String commandFile = "";
	private int myPort;
	private int brokerPort;

	private ClientWrapper client;
	private UserInput userInput;

	private String pubAwaitingReply = null;

	public Publisher(String id, int myPort, String brokerIp, int brokerPort, String commandFile) {

		if (id == null || id.isBlank()) {
			Utils.logError("id not provided or invalid");
			return;
		}
		if (!Utils.isValidPort(myPort)) {
			Utils.logError("port not provided or invalid");
			return;
		}
		if (brokerIp == null || brokerIp.isBlank()) {
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

		// create user input handler
		var callback = new UserInputCallback();
		userInput = new UserInput(callback);
		userInput.start();
	}

	public static void main(String[] args) {

		// init parameters
		var params = new Params(args);

		// create Publisher
		var pub = new Publisher(params.getId(), params.getMyPort(), params.getBrokerIp(),
				params.getBrokerPort(), params.getCommandFile());
		if (!pub.isValid) {
			System.exit(1);
		}
	}

	/**
	 * UserInputCallback
	 */
	public class UserInputCallback implements UserInput.UserInputCallback {

		@Override
		public boolean handleLine(String line) {

			if (client == null || line == null || line.isBlank()) {
				return true;
			}
			if (!client.isConnected()) {
				Utils.logWarn("Client not currently connected to server");
				return true;
			}

			String[] parts = Utils.splitCommandPayload(line);
			String cmd = parts[0];
			String payload = parts[1];

			// currently only pub command is used
			if (!CMD_PUB.equals(cmd)) {
				Utils.logWarn("Unrecognized command: " + line);
				Utils.logWarn("Allowed publisher commands are: pub #topic message, exit");
				return true;
			}

			boolean sent = sendPublish(payload);

			// if sent successfully, return true to block further commands until OK received
			return !sent;
		}

		@Override
		public void onClose() {
			System.exit(0);
		}

		private boolean sendPublish(String payload) {

			String[] parts = Utils.splitTopicMessage(payload);
			String topic = parts[0];
			String msg = parts[1];
			if (!Utils.isValidTopic(topic)) {
				Utils.logWarn("invalid topic naming");
				return false;
			}
			if (msg.isEmpty()) {
				Utils.logWarn("publishing empty message");
			}

			String topicMsg = topic + " " + msg;
			pubAwaitingReply = topicMsg;
			client.sendLine(id + " " + CMD_PUB + " " + topicMsg);

			return true;
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

			// create command file handler
			if (!commandFile.isEmpty()) {
				new CommandFileHandler(commandFile, userInput).start();
			}
		}

		@Override
		public void handleReceivedLine(String line) {

			if (Broker.repliesToAdvanceQueue.contains(line)) {

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

		@Override
		public void handleConnectError() {
			Utils.logError("Could not connect to " + brokerIp + ":" + brokerPort);
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
			userInput.advanceQueue();
		}
	}

	public ClientWrapper getClient() {
		return client;
	}
}
