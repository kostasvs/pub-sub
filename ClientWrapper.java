import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ClientWrapper extends Thread {

	private final String ip;
	private final int port;
	private final int localPort;
	private final SocketHandler handler;

	private DataOutputStream outputStream;

	public ClientWrapper(String ip, int port, ClientWrapper.SocketHandler handler) {
		this.ip = ip;
		this.port = port;
		this.handler = handler;
		this.localPort = 0;
	}

	public ClientWrapper(String ip, int port, ClientWrapper.SocketHandler handler, int localPort) {
		this.ip = ip;
		this.port = port;
		this.handler = handler;
		this.localPort = localPort;
	}

	@Override
	public void run() {

		try (var socket = new Socket(ip, port, InetAddress.getLocalHost(), localPort)) {

			outputStream = new DataOutputStream(socket.getOutputStream());
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
