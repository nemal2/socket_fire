package safety.server;

import safety.gui.ServerGUI;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class FirewallServer {
    private static final int PORT = 5000;
    private static final Set<String> blacklist = new HashSet<>();
    public static ServerGUI gui;
    private static int activeConnections = 0;

    public static void main(String[] args) {
        // Start the GUI
        gui = new ServerGUI();
        gui.setVisible(true);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            gui.log("Firewall Server is running on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientAddress = clientSocket.getInetAddress().getHostAddress();

                if (blacklist.contains(clientAddress)) {
                    gui.log("Blocked blacklisted client: " + clientAddress);
                    clientSocket.close();
                } else {
                    activeConnections++;
                    gui.log("Accepted connection from: " + clientAddress);
                    new ClientHandler(clientSocket).start();
                }
            }
        } catch (IOException e) {
            gui.log("Server error: " + e.getMessage());
        }
    }

    public static void addToBlacklist(String ip) {
        blacklist.add(ip);
        gui.log("Added to blacklist: " + ip);
    }

    public static void decrementConnections() {
        if (activeConnections > 0) {
            activeConnections--;
        }
        else {
            activeConnections = 0;
        }
    }

    public static void logActiveConnections() {
        gui.log("Active Connections: " + activeConnections);
    }
}