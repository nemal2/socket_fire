package safety.server;

import java.io.*;
import java.net.Socket;
import safety.firewall.MITMProtector;
import safety.firewall.HTTPFloodProtector;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final String clientAddress;
    private final MITMProtector mitmProtector;
    private static final HTTPFloodProtector httpFloodProtector = new HTTPFloodProtector();
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.clientAddress = socket.getInetAddress().getHostAddress();
        this.mitmProtector = new MITMProtector();
    }

    @Override
    public void run() {
        try {
            clientSocket.setSoTimeout(30000); // Set a timeout for client inactivity
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            out.println("CONNECTION_ACCEPTED");
            FirewallServer.gui.log("Established connection with client: " + clientAddress);

            String inputLine;
            int emptyLineCount = 0;
            boolean attackDetected = false;

            while ((inputLine = in.readLine()) != null) {
                if (inputLine.trim().isEmpty()) {
                    emptyLineCount++;
                    if (emptyLineCount > 5) break; // Close connection if too many empty lines
                    continue;
                }

                FirewallServer.gui.log("Processing message from " + clientAddress + ": " + inputLine);

                // Check for HTTP Flood
                if (httpFloodProtector.isHTTPFlood(clientAddress, inputLine)) {
                    FirewallServer.gui.log("⚠️ HTTP FLOOD ATTACK DETECTED from: " + clientAddress);
                    FirewallServer.gui.updateHTTPFloodStatus(true, clientAddress);
                    FirewallServer.addToBlacklist(clientAddress);
                    out.println("BLOCKED: HTTP Flood Attack Detected");
                    attackDetected = true;
                    break;
                }

                // Check for MITM attack
                if (mitmProtector.detectMITMAttempt(inputLine, clientAddress)) {
                    FirewallServer.gui.log("⚠️ MITM ATTACK DETECTED from: " + clientAddress);
                    FirewallServer.gui.updateMITMStatus(true, clientAddress);
                    FirewallServer.addToBlacklist(clientAddress);
                    out.println("BLOCKED: MITM Attack Detected");
                    attackDetected = true;
                    break;
                }

                // Process normal request
                String response = String.format("[%tT] Server received: %s", System.currentTimeMillis(), inputLine);
                out.println(response);
                out.flush();
            }

            if (attackDetected) {
                closeConnection("Attack detected");
            } else {
                closeConnection("Connection closed normally");
            }

        } catch (IOException e) {
            if (!e.getMessage().contains("Socket closed") && !e.getMessage().contains("Connection reset")) {
                FirewallServer.gui.log("Error handling client " + clientAddress + ": " + e.getMessage());
            }
            closeConnection("Connection terminated: " + e.getMessage());
        }
    }

    private void closeConnection(String reason) {
        try {
            if (out != null) {
                out.println("CONNECTION_CLOSED: " + reason);
                out.flush();
                out.close();
            }
            if (in != null) in.close();
            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
            FirewallServer.decrementConnections();
            FirewallServer.gui.log("Closed connection from " + clientAddress + ": " + reason);
        } catch (IOException e) {
            FirewallServer.gui.log("Error while closing connection: " + e.getMessage());
        }
    }
}