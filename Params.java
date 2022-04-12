public class Params {

	public String id;
	public String brokerIp;
	public String commandFile;
	public int myPort;
	public int brokerPort;
	public int subscribersPort;

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
}
