// ClientHandler.java
package safety.server;

import java.io.*;
import java.net.Socket;
import safety.firewall.*;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final String clientAddress;
    private final String connectionId;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isRunning = true;

    public ClientHandler(Socket socket, String connectionId) {
        this.clientSocket = socket;
        this.clientAddress = socket.getInetAddress().getHostAddress();
        this.connectionId = connectionId;
    }

    @Override
    public void run() {
        try {
            setupConnection();
            handleClientCommunication();
        } catch (IOException e) {
            handleError(e);
        } finally {
            cleanup();
        }
    }

    private void setupConnection() throws IOException {
        clientSocket.setSoTimeout(30000); // 30 second timeout
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out.println("CONNECTION_ACCEPTED");
        FirewallServer.gui.log("🤝 Established connection with client: " + clientAddress);
    }

    private void handleClientCommunication() throws IOException {
        String inputLine;
        int emptyLineCount = 0;

        while (isRunning && (inputLine = in.readLine()) != null) {
            if (inputLine.trim().isEmpty()) {
                if (++emptyLineCount > 5) break;
                continue;
            }
            emptyLineCount = 0;

            // Register header received for Slowloris detection
            FirewallServer.getSlowlorisProtector().registerHeaderReceived(clientAddress, connectionId);

            // Check for various attacks
            if (checkForAttacks(inputLine)) {
                break;
            }

            // Process normal request
            FirewallServer.gui.log("📨 Processing message from " + clientAddress + ": " + inputLine);
            String response = String.format("[%tT] Server received: %s", System.currentTimeMillis(), inputLine);
            out.println(response);
            out.flush();
        }
    }

    private boolean checkForAttacks(String inputLine) {
        // Check for HTTP Flood
        if (FirewallServer.getHttpFloodProtector().isHTTPFlood(clientAddress, inputLine)) {
            handleAttack("HTTP Flood");
            return true;
        }

        // Check for MITM
        if (FirewallServer.getMitmProtector().detectMITMAttempt(inputLine, clientAddress)) {
            handleAttack("MITM");
            return true;
        }

        // Check for Slowloris
        if (FirewallServer.getSlowlorisProtector().isSlowlorisAttack(clientAddress, connectionId)) {
            handleAttack("Slowloris");
            return true;
        }

        return false;
    }

    private void handleAttack(String attackType) {
        FirewallServer.gui.log("⚠️ " + attackType + " attack detected from: " + clientAddress);
        FirewallServer.addToBlacklist(clientAddress);
        out.println("BLOCKED: " + attackType + " Attack Detected");
        closeConnection();
    }

    private void handleError(IOException e) {
        if (!e.getMessage().contains("Socket closed") &&
                !e.getMessage().contains("Connection reset")) {
            FirewallServer.gui.log("❌ Error handling client " + clientAddress + ": " + e.getMessage());
        }
    }

    private void cleanup() {
        closeConnection();
        FirewallServer.removeHandler(connectionId);
        FirewallServer.getSlowlorisProtector().removeConnection(clientAddress, connectionId);
        FirewallServer.decrementConnections();
    }

    public void closeConnection() {
        isRunning = false;
        try {
            if (out != null) {
                out.println("CONNECTION_CLOSED");
                out.close();
            }
            if (in != null) in.close();
            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            FirewallServer.gui.log("Error closing connection: " + e.getMessage());
        }
    }

    public String getClientAddress() {
        return clientAddress;
    }
}