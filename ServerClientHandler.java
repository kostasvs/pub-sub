import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerClientHandler extends Thread {

	protected Socket socket;
	protected ServerWrapper.SocketHandler handler;
	protected ServerWrapper parentServer;

	public ServerClientHandler(Socket socket, ServerWrapper.SocketHandler handler, ServerWrapper parentServer) {
		this.socket = socket;
		this.handler = handler;
		this.parentServer = parentServer;
	}

	@Override
	public void run() {

		try (var in = new DataInputStream(new BufferedInputStream(
				socket.getInputStream()))) {

			String line = null;
			while (isAlive()) {

				line = in.readUTF().trim();
				if (line.equalsIgnoreCase("exit")) {
					break;
				}
				handler.handleReceivedLine(socket, line);
			}
		} catch (IOException e) {
			Utils.log("Socket " + socket + " exited: " + e.getMessage());
		}

		if (parentServer != null) parentServer.cleanupClient(socket);
		handler.handleDisconnectedClient(socket);
	}
}
