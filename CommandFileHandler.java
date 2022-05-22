import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class CommandFileHandler extends Thread {

	private boolean exited = false;
	private boolean isValid = false;
	private UserInput userInput = null;
	private final List<CommandInfo> commands = new ArrayList<>();

	public CommandFileHandler(String filename, UserInput userInput) {

		if (userInput == null) {
			Utils.logError("null UserInput passed in constructor");
			return;
		}
		if (filename == null || filename.isBlank()) {
			Utils.logError("null or empty filename passed in constructor");
			return;
		}

		// load lines from file into list
		File sourceFile = new File(filename);
		try (Scanner reader = new Scanner(sourceFile)) {

			while (reader.hasNextLine()) {
				var cmd = new CommandInfo(reader.nextLine());
				if (!cmd.isValid) {
					Utils.logWarn("Invalid command: " + filename);
					continue;
				}
				commands.add(cmd);
			}

		} catch (FileNotFoundException e) {
			Utils.logError("Command file not found: " + filename);
			return;
		} catch (Exception e) {
			Utils.logError("Unhandler exception during command file read: " + e.getMessage());
			return;
		}

		Utils.log(commands.size() + " commands loaded from file");
		this.userInput = userInput;
		isValid = true;
	}

	@Override
	public void run() {

		if (!isValid) {
			return;
		}

		for (CommandInfo commandInfo : commands) {

			if (!commandInfo.isValid) {
				continue;
			}

			Utils.log("Command file: Waiting " + commandInfo.delay + "s before executing command: "
					+ commandInfo.command);
			try {
				Thread.sleep(commandInfo.delay * 1000L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			Utils.log("Command file: Executing command: " + commandInfo.command);
			userInput.injectLine(commandInfo.command);
		}

		exited = true;
	}

	public boolean hasExited() {
		return exited;
	}

	private class CommandInfo {

		public int delay = 0;
		public String command = "";
		public boolean isValid = false;

		public CommandInfo(String fromString) {

			var parts = Utils.splitCommandPayload(fromString);
			if (parts == null || parts[0] == null || parts[0].isBlank() || parts[1] == null || parts[1].isBlank()) {
				Utils.logWarn("empty delay and/or command");
				return;
			}

			try {
				delay = Math.max(0, Integer.parseInt(parts[0]));
			} catch (NumberFormatException e) {
				Utils.logWarn("invalid delay");
				return;
			}
			command = parts[1];

			isValid = true;
		}
	}
}
