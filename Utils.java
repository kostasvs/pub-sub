public class Utils {
	private Utils() {
	}

	public static int toInt(String str, int defVal) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return defVal;
		}
	}

	public static String[] splitCommandPayload(String input) {

		String[] parts = { "", "" };
		if (input != null) {
			input = input.trim();
			int sepPos = input.indexOf(" ");
			if (sepPos != -1) {
				parts[0] = input.substring(0, sepPos).trim();
				parts[1] = input.substring(sepPos + 1).trim();
			}
			else {
				parts[0] = input;
			}
		}
		return parts;
	}

	public static String[] splitIdCommandPayload(String input) {

		String[] parts = { "", "", "" };
		if (input != null) {
			input = input.trim();
			int sepPos = input.indexOf(" ");
			if (sepPos != -1) {
				parts[0] = input.substring(0, sepPos).trim();
				var commandPayload = splitCommandPayload(input.substring(sepPos + 1));
				parts[1] = commandPayload[0];
				parts[2] = commandPayload[1];
			}
			else {
				parts[0] = input;
			}
		}
		return parts;
	}

	public static String[] splitTopicMessage(String input) {

		// currently has identical implementation with splitCommandPayload, so use that instead
		return splitCommandPayload(input);
	}

	public static void printLine(String str) {
		System.out.println(str == null ? "null" : str);
	}

	public static void log(String str) {
		System.out.println("[log] " + (str == null ? "null" : str));
	}

	public static void logWarn(String str) {
		System.out.println("[warn] " + (str == null ? "null" : str));
	}

	public static void logError(String str) {
		System.err.println("[error] " + (str == null ? "null" : str));
	}

	public static boolean isValidPort(int port) {
		return port > 0 && port <= 65535;
	}

	public static boolean isValidTopic(String topic) {
		return topic != null && !topic.isBlank();
	}

	public static boolean isValidTopicStrict(String topic) {
		return topic != null && topic.length() > 1 && topic.startsWith("#");
	}
}