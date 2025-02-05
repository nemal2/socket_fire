package safety.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private static final Map<String, Integer> requestCounts = new HashMap<>(); // Track requests per client
    private static final int REQUEST_LIMIT = 10; // Max requests per client

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        FirewallServer.gui.log("Accepted connection from: " + clientAddress);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                FirewallServer.gui.log("Received from " + clientAddress + ": " + inputLine);

                // Detect malicious input
                if (isMalicious(inputLine)) {
                    FirewallServer.gui.log("Malicious input detected from " + clientAddress);
                    FirewallServer.addToBlacklist(clientAddress);
                    out.println("You are blocked!");
                    clientSocket.close();
                    FirewallServer.decrementConnections();
                    return;
                }

                // Rate limiting
                if (isRateLimited(clientAddress)) {
                    FirewallServer.gui.log("Rate limit exceeded by " + clientAddress);
                    FirewallServer.addToBlacklist(clientAddress);
                    out.println("You are blocked for exceeding the rate limit!");
                    clientSocket.close();
                    FirewallServer.decrementConnections();
                    return;
                }

                // Respond to the client
                out.println("Echo: " + inputLine);
            }
        } catch (IOException e) {
            FirewallServer.gui.log("Error handling client " + clientAddress + ": " + e.getMessage());
        } finally {
            FirewallServer.decrementConnections();
            try {
                clientSocket.close();
            } catch (IOException e) {
                FirewallServer.gui.log("Error closing socket for " + clientAddress + ": " + e.getMessage());
            }
            FirewallServer.logActiveConnections();
        }
    }

    private boolean isMalicious(String input) {
        // Detect long inputs or specific attack patterns
        return input.length() > 1000 || input.contains("attack") || input.contains("DROP TABLE") || input.contains("SELECT * FROM");
    }

    private boolean isRateLimited(String ip) {
        requestCounts.putIfAbsent(ip, 0);
        requestCounts.put(ip, requestCounts.get(ip) + 1);
        return requestCounts.get(ip) > REQUEST_LIMIT;
    }
}