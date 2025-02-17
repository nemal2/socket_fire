package safety.server;

import safety.firewall.DDoSProtector;
import safety.firewall.HTTPFloodProtector;
import safety.firewall.MITMProtector;
import safety.gui.ServerGUI;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class FirewallServer {
    private static final int PORT = 5000;
    private static final Set<String> blacklist = new HashSet<>();
    public static ServerGUI gui;
    private static int activeConnections = 0;
    private static final DDoSProtector ddosProtector = new DDoSProtector();
    private static final MITMProtector mitmProtector = new MITMProtector();
    private static final HTTPFloodProtector httpFloodProtector = new HTTPFloodProtector();



    public static void main(String[] args) {
        // Start the GUI
        gui = new ServerGUI();
        gui.setVisible(true);

        try {
            // Create SSL Server Socket
            System.setProperty("javax.net.ssl.keyStore", "server.keystore");
            System.setProperty("javax.net.ssl.keyStorePassword", "password");
            SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(PORT);

            gui.log("Firewall Server is running on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientAddress = clientSocket.getInetAddress().getHostAddress();

                // First check if the IP is already blocked
                if (blacklist.contains(clientAddress)){
                    gui.log("Rejected connection from blocked IP: " + clientAddress);
                    clientSocket.close();
                    updateConnections(false, true);
                    continue;
                }

                // Unified attack detection approach
                boolean isDDoS = ddosProtector.isDDoSAttack(clientAddress);
                boolean isHTTPFlood = httpFloodProtector.isHTTPFlood(clientAddress, null);

                if (isDDoS || isHTTPFlood) {
                    String attackType = isDDoS ? "DDoS" : "HTTP Flood";
                    gui.log("Rejected connection from " + attackType + " attacker: " + clientAddress);
                    clientSocket.close();
                    updateConnections(false, true);
                    addToBlacklist(clientAddress);
                    continue;
                }

                // Check for DDoS attack - this must be checked BEFORE HTTP flood
                if (ddosProtector.isDDoSAttack(clientAddress)) {
                    gui.log("Rejected connection from DDoS attacker: " + clientAddress);
                    clientSocket.close();
                    updateConnections(false, true);
                    addToBlacklist(clientAddress);
                    continue;
                }

                // If we reach here, accept the connection
                updateConnections(true, false);
                gui.log("Accepted connection from: " + clientAddress);
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            gui.log("Server error: " + e.getMessage());
        }
    }

    private static synchronized void updateConnections(boolean isNewConnection, boolean isBlocked) {
        if (isNewConnection) {
            activeConnections++;
        } else if (activeConnections > 0) {
            activeConnections--;
        }
        if (isBlocked) {
            gui.incrementBlockedClients();
        }
        gui.updateConnectionStatus(activeConnections);
    }

    public static void addToBlacklist(String ip) {
        blacklist.add(ip);
        gui.log("Added to blacklist: " + ip);
        updateConnections(false, true);
    }

    public static void decrementConnections() {
        updateConnections(false, false);
    }

    public static DDoSProtector getDdosProtector() {
        return ddosProtector;
    }

    public static HTTPFloodProtector getHttpFloodProtector() {
        return httpFloodProtector;
    }
}