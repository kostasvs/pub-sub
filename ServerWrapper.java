import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServerWrapper extends Thread {

	protected final int port;
	protected final SocketHandler handler;

	protected final Map<Socket, DataOutputStream> socketOutputs = new HashMap<>();

	public ServerWrapper(int port, SocketHandler handler) {
		this.port = port;
		this.handler = handler;
	}

	@Override
	public void run() {

		try (var server = new ServerSocket(port)) {

			handler.handleServerReady(port);
			while (isAlive()) {

				var socket = server.accept();
				socketOutputs.put(socket, new DataOutputStream(socket.getOutputStream()));
				new ServerClientHandler(socket, handler, this).start();
				handler.handleAcceptedClient(socket);
			}
		} catch (IOException e) {
			Utils.log("Server at port " + port + " exited: " + e.getMessage());
		}
	}

	public void sendLine(Socket socket, String line) {

		try {
			socketOutputs.get(socket).writeUTF(line);
		} catch (Exception e) {
			Utils.logError("Could not send line to socket " + socket +
					", reason: " + e.getMessage());
		}
	}

	public Set<Socket> getClientSockets() {
		return socketOutputs.keySet();
	}

	public void cleanupClient(Socket socket) {

		socketOutputs.remove(socket);
	}

	public interface SocketHandler {

		void handleServerReady(int port);

		void handleAcceptedClient(Socket socket);

		void handleReceivedLine(Socket socket, String line);

		void handleDisconnectedClient(Socket socket);
	}
}