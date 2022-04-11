public class Subscriber {

	protected boolean isValid = false;
	protected String id;
	protected String brokerIp;
	protected String commandFile = "";
	protected int myPort;
	protected int brokerPort;

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
		this.commandFile = commandFile.trim();
		isValid = true;
	}

	public static void main(String[] args) {

		// init parameters
		String id = "";
		int myPort = -1;
		String brokerIp = "";
		int brokerPort = -1;
		String commandFile = "";

		for (int i = 0; i < args.length; i += 2) {
			// all arguments are pairs, so skip if last argument is odd
			// also, first argument of pair should start with dash (-)
			String arg = args[i];
			if (i >= args.length - 1 || !arg.startsWith("-")) {
				continue;
			}

			// first argument of pair is case-insensitive
			arg = arg.substring(1).toLowerCase();

			switch (arg) {
				case "i":
					id = args[i + 1];
					break;

				case "r":
					myPort = Utils.toInt(args[i + 1], myPort);
					break;

				case "h":
					brokerIp = args[i + 1];
					break;

				case "p":
					brokerPort = Utils.toInt(args[i + 1], brokerPort);
					break;

				case "f":
					commandFile = args[i + 1];
					break;

				default:
					break;
			}
		}

		// create subscriber
		var sub = new Subscriber(id, myPort, brokerIp, brokerPort, commandFile);
		if (!sub.isValid) {
			System.exit(1);
		}
	}
}
