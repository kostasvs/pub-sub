import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class UserInput extends Thread {

	private boolean exited = false;
	private UserInputCallback callback;

	private boolean heldUp = false;
	private List<String> queuedLines = new ArrayList<>();

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
					line = scanner.nextLine();
					injectLine(line);

				} catch (NoSuchElementException | IllegalStateException e) {
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

		/**
		 * Called when a new line was entered by the user and no other lines are waiting
		 * to be processed.
		 * If returns true, the line is discarded afterwards.
		 * If returns false, the line is pushed into queue and no handleLine() will be
		 * called again until the queue empties.
		 * 
		 * @param line The new line that was entered.
		 * @return Whether the line was processed and can be discarded.
		 */
		public boolean handleLine(String line);

		public void onClose();
	}

	/**
	 * Injects this line as if it was input by the user.
	 * 
	 * @param line Line to inject.
	 */
	public void injectLine(String line) {

		if (line == null) {
			return;
		}

		if (line.equalsIgnoreCase("exit")) {
			exited = true;
		} else {
			if (heldUp) {
				queuedLines.add(line);
			}
			else if (!callback.handleLine(line)) {
				heldUp = true;
			}
		}
	}

	public void advanceQueue() {

		if (!heldUp) {
			return;
		}

		while (!queuedLines.isEmpty()) {

			String nextLine = queuedLines.get(0);
			queuedLines.remove(0);
			if (!callback.handleLine(nextLine)) {
				return;
			}
		}

		heldUp = false;
	}

	public boolean isHeldUp() {
		return heldUp;
	}

	public boolean hasExited() {
		return exited;
	}
}
