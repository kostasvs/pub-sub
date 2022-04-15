import java.util.Scanner;

public class UserInput extends Thread {

	private boolean exited = false;
	private UserInputCallback callback;

	public UserInput(UserInputCallback callback) {

		if (callback == null) {
			Utils.logError("null UserInputCallback passed in constructor");
			return;
		}
		this.callback = callback;
	}

	@Override
	public void run() {

		try (var scanner = new Scanner(System.in)) {

			while (true) {

				// get line, quit on error
				String line = null;
				try {
					line = scanner.nextLine().trim();
					if (line.equalsIgnoreCase("exit")) {
						exited = true;
					} else {
						callback.handleLine(line);
					}
				} catch (Exception e) {
					Utils.logError(e.toString());
					exited = true;
				}

				// exit condition
				if (line == null || exited) {
					break;
				}
			}
		}

		callback.onClose();
		exited = true;
	}

	/**
	 * UserInputCallback
	 */
	public interface UserInputCallback {

		public void handleLine(String line);

		public void onClose();
	}

	public boolean hasExited() {
		return exited;
	}
}
