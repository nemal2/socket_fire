package safety.client.good;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.util.Scanner;

public class GoodClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;
    private static final String TRUSTSTORE_PATH = "client.truststore";
    private static final String TRUSTSTORE_PASSWORD = "password";  // Matching your existing password

    private static SSLSocket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        try {
            setupSSLConnection();
            startClientCommunication();
        } catch (Exception e) {
            System.err.println("Error in client: " + e.getMessage());
            e.printStackTrace(); // Add stack trace for debugging
        } finally {
            cleanup();
        }
    }

    private static void setupSSLConnection() throws IOException {
        // Set up SSL connection with proper certificate validation
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_PATH);
        System.setProperty("javax.net.ssl.trustStorePassword", TRUSTSTORE_PASSWORD);

        try {
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = (SSLSocket) sslSocketFactory.createSocket(SERVER_IP, SERVER_PORT);

            System.out.println("Initiating SSL handshake...");
            socket.startHandshake();
            System.out.println("SSL handshake completed successfully");

            // Set up input/output streams
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connected to server at " + SERVER_IP + ":" + SERVER_PORT);
        } catch (Exception e) {
            System.err.println("SSL Connection Error: " + e.getMessage());
            throw e;
        }
    }

    private static void startClientCommunication() {
        // Start a thread for receiving server messages
        Thread receiveThread = new Thread(() -> {
            try {
                String serverMessage;
                while (running && (serverMessage = in.readLine()) != null) {
                    System.out.println("Server: " + serverMessage);

                    if (serverMessage.startsWith("BLOCKED:")) {
                        System.out.println("Connection was blocked by server. Exiting...");
                        running = false;
                        break;
                    }

                    if (serverMessage.startsWith("CONNECTION_CLOSED:")) {
                        System.out.println("Server closed the connection. Exiting...");
                        running = false;
                        break;
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error receiving message: " + e.getMessage());
                }
            }
        });
        receiveThread.start();

        // Handle user input
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Start typing messages (type 'exit' to quit):");

            while (running) {
                String userInput = scanner.nextLine().trim();

                if (userInput.equalsIgnoreCase("exit")) {
                    running = false;
                    break;
                }

                if (!userInput.isEmpty()) {
                    // Implement rate limiting
                    Thread.sleep(500); // Add a small delay between messages

                    out.println(userInput);
                    out.flush();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Client interrupted: " + e.getMessage());
        }
    }

    private static void cleanup() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
        System.out.println("Client shutdown complete.");
    }
}