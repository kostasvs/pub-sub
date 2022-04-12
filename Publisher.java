public class Publisher {

	protected boolean isValid = false;
	protected String id;
	protected String brokerIp;
	protected String commandFile = "";
	protected int myPort;
	protected int brokerPort;

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
		this.commandFile = commandFile.trim();
		isValid = true;
	}

	public static void main(String[] args) {

		// init parameters
		var params = new Params(args);

		// create Publisher
		var sub = new Publisher(params.id, params.myPort, params.brokerIp,
				params.brokerPort, params.commandFile);
		if (!sub.isValid) {
			System.exit(1);
		}
	}
}
