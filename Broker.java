import java.net.Socket;

public class Broker {

	private boolean isValid = false;
	private int publishersPort;
	private int subscribersPort;

	private ServerWrapper serverWrapper;

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

		// create user input handler
		var callback = broker.new UserInputCallback();
		new UserInput(callback).start();
	}

	/**
	 * UserInputCallback
	 */
	public class UserInputCallback implements UserInput.UserInputCallback {

		@Override
		public void handleLine(String line) {
			for (var socket : serverWrapper.getClientSockets()) {
				serverWrapper.sendLine(socket, line);
			}
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
			Utils.log("Message from " + socket + ": " + line);
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
	}
}
