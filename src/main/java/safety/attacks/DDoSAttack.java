package safety.attacks;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;

public class DDoSAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        System.out.println("Starting DDoS Attack simulation against " + SERVER_IP + ":" + SERVER_PORT);
        System.out.println("Attempting to establish 50 connections rapidly to trigger DDoS detection...");

        // Trust all certificates for testing purposes
        System.setProperty("javax.net.ssl.trustStore", "client.truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        for (int i = 0; i < 50; i++) {
            new Thread(() -> {
                try {
                    SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(SERVER_IP, SERVER_PORT);
                    socket.startHandshake(); // Ensure proper SSL handshake

                    System.out.println("DDoS Attack: Connection established by " + Thread.currentThread().getName());

                    // Keep the connection alive briefly
                    Thread.sleep(50);
                    socket.close();
                } catch (Exception e) {
                    System.out.println("Server blocked the attack or connection failed: " + e.getMessage());
                }
            }).start();

            // Small delay to make it more realistic
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("DDoS Attack simulation completed. Check server logs for detection results.");
    }
}