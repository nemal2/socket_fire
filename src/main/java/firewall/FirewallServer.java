package firewall;

import java.io.*;
import java.net.*;
import java.util.*;

public class FirewallServer {
    private static final int PORT = 12345;
    private static final Set<String> BLACKLISTED_IPS = new HashSet<>(Arrays.asList("192.168.1.100")); // Example blacklisted IPs

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                String clientIP = socket.getInetAddress().getHostAddress();
                System.out.println("New client connected: " + clientIP);

                if (BLACKLISTED_IPS.contains(clientIP)) {
                    System.out.println("Blocked connection from blacklisted IP: " + clientIP);
                    socket.close();
                } else {
                    new ClientHandler(socket).start();
                }
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream input = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true)) {

            String text;
            while ((text = reader.readLine()) != null) {
                System.out.println("Received: " + text);
                if (text.equals("attack")) {
                    writer.println("ALERT: Attack detected!");
                } else {
                    writer.println("Message received: " + text);
                }
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}