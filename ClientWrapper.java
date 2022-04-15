import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientWrapper extends Thread {

	protected final String ip;
	protected final int port;
	protected final SocketHandler handler;

	protected Socket socket;
	protected DataOutputStream outputStream;

	public ClientWrapper(String ip, int port, ClientWrapper.SocketHandler handler) {
		this.ip = ip;
		this.port = port;
		this.handler = handler;
	}

	@Override
	public void run() {

		try (var s = new Socket(ip, port)) {

			socket = s;
			outputStream = new DataOutputStream(s.getOutputStream());
			handler.handleConnected();

			handleInputFromServer(socket);

			handler.handleDisconnected();

		} catch (Exception e) {
			Utils.logError("Could not connect to " + ip + ":" + port);
		}
	}

	private void handleInputFromServer(Socket socket) {

		try (var in = new DataInputStream(socket.getInputStream())) {

			String line = null;
			while (isAlive()) {

				line = in.readUTF().trim();
				if (line.equalsIgnoreCase("exit")) {
					break;
				}
				handler.handleReceivedLine(line);
			}
		} catch (Exception e) {
			Utils.log("Client disconnected: " + e.getMessage());
		}
	}

	public void sendLine(String line) {

		try {
			outputStream.writeUTF(line);
		} catch (Exception e) {
			Utils.logError("Could not send line, reason: " + e.getMessage());
		}
	}

	public interface SocketHandler {

		void handleConnected();

		void handleReceivedLine(String line);

		void handleDisconnected();
	}
}
