public class Subscriber {

	public static final String CMD_ID = "subid";
	public static final String CMD_SUB = "sub";
	public static final String CMD_UNSUB = "unsub";

	private boolean isValid = false;
	private String id;
	private String brokerIp;
	private String commandFile = "";
	private int myPort;
	private int brokerPort;

	private ClientWrapper client;
	private UserInput userInput;

	public Subscriber(String id, int myPort, String brokerIp, int brokerPort, String commandFile) {

		if (id == null || id.isBlank()) {
			Utils.logError("id not provided or invalid");
			return;
		}
		if (!Utils.isValidPort(myPort)) {
			Utils.log("port not provided, using auto-assigned port");
			myPort = 0;
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

		// create command file handler
		if (!this.commandFile.isEmpty()) {
			new CommandFileHandler(this.commandFile, userInput).start();
		}
	}

	public static void main(String[] args) {

		// init parameters
		var params = new Params(args);

		// create subscriber
		var sub = new Subscriber(params.getId(), params.getMyPort(), params.getBrokerIp(),
				params.getBrokerPort(), params.getCommandFile());
		if (!sub.isValid) {
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

			String[] parts = Utils.splitCommandPayload(line);
			String cmd = parts[0];
			String payload = parts[1];

			switch (cmd) {
				case CMD_SUB:
				case CMD_UNSUB:
					// subscribe/unsubscribe to topic
					boolean sent = sendCommand(cmd, payload);

					// if sent successfully, return true to block further commands until OK received
					return !sent;

				default:
					Utils.logWarn("unrecognized command in user input: " + line);
					break;
			}
			return true;
		}

		@Override
		public void onClose() {
			System.exit(0);
		}

		private boolean sendCommand(String cmd, String topic) {

			if (!Utils.isValidTopic(topic)) {
				Utils.logWarn("invalid topic naming");
				return false;
			}
			client.sendLine(id + " " + cmd + " " + topic);
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
		}

		@Override
		public void handleReceivedLine(String line) {

			if (Broker.repliesToAdvanceQueue.contains(line)) {
				Utils.printLine(line);
				userInput.advanceQueue();
				return;
			}

			String[] parts = Utils.splitCommandPayload(line);
			if (Publisher.CMD_PUB.equals(parts[0])) {
				handleTopicMessage(parts[1]);
				return;
			}

			Utils.log("Received line: " + line);
		}

		@Override
		public void handleDisconnected() {
			Utils.log("Disconnected from broker");
			System.exit(0);
		}

		private void handleTopicMessage(String input) {

			String[] parts = Utils.splitTopicMessage(input);

			if (!Utils.isValidTopic(parts[0])) {
				Utils.logWarn("received msg with invalid topic");
				return;
			}

			Utils.printLine("Received msg for topic " + parts[0] + ": " + parts[1]);
		}
	}

	public ClientWrapper getClient() {
		return client;
	}
}
