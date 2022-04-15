public class Utils {
	private Utils() {}

	public static int toInt(String str, int defVal) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return defVal;
		}
	}

	public static void log(String str) {
		System.out.println("[log] " + (str == null ? "null" : str));
	}
	
	public static void logError(String str) {
		System.err.println("[error] " + (str == null ? "null" : str));
	}
	
	public static boolean isValidPort(int port) {
		return port > 0 && port <= 65535;
	}
}