package safety.server;

import safety.firewall.*;
import safety.gui.ServerGUI;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class FirewallServer {
    private static final int PORT = 5000;
    private static final Set<String> blacklist = new HashSet<>();
    public static ServerGUI gui;
    private static int activeConnections = 0;
    private static final Map<String, ClientHandler> activeHandlers = new ConcurrentHashMap<>();

    // Firewall protectors
    private static final DDoSProtector ddosProtector = new DDoSProtector();
    private static final MITMProtector mitmProtector = new MITMProtector();
    private static final HTTPFloodProtector httpFloodProtector = new HTTPFloodProtector();
    private static final PingOfDeathProtector podProtector = new PingOfDeathProtector();
    private static final SlowlorisProtector slowlorisProtector = new SlowlorisProtector();
    private static UDPHandler udpHandler;

    public static void main(String[] args) {
        // Initialize GUI
        gui = new ServerGUI();
        gui.setVisible(true);

        // Start UDP Handler
        try {
            udpHandler = new UDPHandler(PORT);
            udpHandler.start();
            gui.log("UDP Handler started on port " + PORT);
        } catch (IOException e) {
            gui.log("Failed to start UDP Handler: " + e.getMessage());
        }

        // Start main server
        try {
            // SSL Configuration
            System.setProperty("javax.net.ssl.keyStore", "server.keystore");
            System.setProperty("javax.net.ssl.keyStorePassword", "password");
            SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(PORT);

            gui.log("🚀 Firewall Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientAddress = clientSocket.getInetAddress().getHostAddress();
                String connectionId = UUID.randomUUID().toString();

                if (handleNewConnection(clientSocket, clientAddress, connectionId)) {
                    // Start client handler if connection is accepted
                    ClientHandler handler = new ClientHandler(clientSocket, connectionId);
                    activeHandlers.put(connectionId, handler);
                    handler.start();
                }
            }
        } catch (IOException e) {
            gui.log("❌ Server error: " + e.getMessage());
        }
    }

    private static boolean handleNewConnection(Socket clientSocket, String clientAddress, String connectionId) throws IOException {
        // Check blacklist
        if (blacklist.contains(clientAddress)) {
            gui.log("🚫 Rejected connection from blocked IP: " + clientAddress);
            clientSocket.close();
            updateConnections(false, true);
            return false;
        }

        // Check for various attacks
        if (checkForAttacks(clientAddress, connectionId)) {
            gui.log("🚫 Rejected connection due to detected attack from: " + clientAddress);
            clientSocket.close();
            updateConnections(false, true);
            addToBlacklist(clientAddress);
            return false;
        }

        // Accept connection
        updateConnections(true, false);
        gui.log("✅ Accepted connection from: " + clientAddress);
        return true;
    }

    private static boolean checkForAttacks(String clientAddress, String connectionId) {
        // Register connection with Slowloris protector
        slowlorisProtector.registerConnection(clientAddress, connectionId);

        // Check all attack types
        boolean isDDoS = ddosProtector.isDDoSAttack(clientAddress);
        boolean isHTTPFlood = httpFloodProtector.isHTTPFlood(clientAddress, null);
        boolean isSlowloris = slowlorisProtector.isSlowlorisAttack(clientAddress, connectionId);

        return isDDoS || isHTTPFlood || isSlowloris;
    }

    public static synchronized void updateConnections(boolean isNewConnection, boolean isBlocked) {
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
        gui.log("⛔ Added to blacklist: " + ip);
        updateConnections(false, true);

        // Close any existing connections from this IP
        activeHandlers.values().stream()
                .filter(handler -> handler.getClientAddress().equals(ip))
                .forEach(ClientHandler::closeConnection);
    }

    public static void removeHandler(String connectionId) {
        activeHandlers.remove(connectionId);
    }

    public static void decrementConnections() {
        updateConnections(false, false);
    }

    // Getters for protectors
    public static DDoSProtector getDdosProtector() { return ddosProtector; }
    public static MITMProtector getMitmProtector() { return mitmProtector; }
    public static HTTPFloodProtector getHttpFloodProtector() { return httpFloodProtector; }
    public static PingOfDeathProtector getPodProtector() { return podProtector; }
    public static SlowlorisProtector getSlowlorisProtector() { return slowlorisProtector; }
}