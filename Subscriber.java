public class Subscriber {

	protected boolean isValid = false;
	protected String id;
	protected String brokerIp;
	protected String commandFile = "";
	protected int myPort;
	protected int brokerPort;

	protected ClientWrapper client;

	public Subscriber(String id, int myPort, String brokerIp, int brokerPort, String commandFile) {

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
		client = new ClientWrapper(brokerIp, brokerPort, new BrokerHandler());
		client.start();
	}

	public static void main(String[] args) {

		// init parameters
		var params = new Params(args);

		// create subscriber
		var sub = new Subscriber(params.id, params.myPort, params.brokerIp,
				params.brokerPort, params.commandFile);
		if (!sub.isValid) {
			System.exit(1);
			return;
		}

		// create user input handler
		var callback = sub.new UserInputCallback();
		new UserInput(callback).start();
	}

	/**
	 * UserInputCallback
	 */
	public class UserInputCallback implements UserInput.UserInputCallback {

		@Override
		public void handleLine(String line) {
			if (client != null) {
				client.sendLine(line);
			}
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
