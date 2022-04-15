public class Params {

	private String id;
	private String brokerIp;
	private String commandFile;
	private int myPort;
	private int brokerPort;
	private int subscribersPort;

	public Params(String[] args) {

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

				case "s":
					subscribersPort = Utils.toInt(args[i + 1], subscribersPort);
					break;

				case "f":
					commandFile = args[i + 1];
					break;

				default:
					break;
			}
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getBrokerIp() {
		return brokerIp;
	}

	public void setBrokerIp(String brokerIp) {
		this.brokerIp = brokerIp;
	}

	public String getCommandFile() {
		return commandFile;
	}

	public void setCommandFile(String commandFile) {
		this.commandFile = commandFile;
	}

	public int getMyPort() {
		return myPort;
	}

	public void setMyPort(int myPort) {
		this.myPort = myPort;
	}

	public int getBrokerPort() {
		return brokerPort;
	}

	public void setBrokerPort(int brokerPort) {
		this.brokerPort = brokerPort;
	}

	public int getSubscribersPort() {
		return subscribersPort;
	}

	public void setSubscribersPort(int subscribersPort) {
		this.subscribersPort = subscribersPort;
	}
}
