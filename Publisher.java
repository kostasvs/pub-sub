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
		new UserInput(callback);
	}

	/**
	 * UserInputCallback
	 */
	public class UserInputCallback implements UserInput.UserInputCallback {

		@Override
		public void handleLine(String line) {
			// TODO handle line
		}

		@Override
		public void onClose() {
			System.exit(0);
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
			Utils.log("Received line: " + line);
		}

		@Override
		public void handleDisconnected() {
			Utils.log("Disconnected from broker");
		}
	}

	public ClientWrapper getClient() {
		return client;
	}
}
