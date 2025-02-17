package safety.attacks;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class HTTPFloodAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        System.out.println("Starting HTTP Flood Attack simulation against " + SERVER_IP + ":" + SERVER_PORT);
        System.out.println("Sending 20 rapid HTTP requests to trigger flood detection...");

        // Trust all certificates for testing purposes
        System.setProperty("javax.net.ssl.trustStore", "client.truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        for (int i = 0; i < 20; i++) {
            new Thread(() -> {
                try {
                    SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(SERVER_IP, SERVER_PORT);
                    socket.startHandshake(); // Important for SSL connection

                    PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    // Send an HTTP-like request over SSL
                    out.println("GET / HTTP/1.1");
                    out.println("Host: localhost");
                    out.println(); // Empty line to indicate end of headers
                    out.flush();

                    System.out.println("HTTP Flood Attack request sent by " + Thread.currentThread().getName());

                    // Keep connection open briefly
                    Thread.sleep(50);
                    socket.close();
                } catch (Exception e) {
                    System.out.println("Server blocked the attack: " + e.getMessage());
                }
            }).start();

            // Add a very small delay between thread creation
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("HTTP Flood Attack simulation completed. Check server logs for detection results.");
    }
}