public class Broker {

	protected boolean isValid = false;
	protected int publishersPort;
	protected int subscribersPort;

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
	}

	public static void main(String[] args) {

		// init parameters
		var params = new Params(args);

		// create Broker
		var sub = new Broker(params.myPort, params.subscribersPort);
		if (!sub.isValid) {
			System.exit(1);
		}
	}
}
