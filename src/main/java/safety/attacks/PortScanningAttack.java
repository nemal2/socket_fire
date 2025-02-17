package safety.attacks;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PortScanningAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int TARGET_PORT = 5000;
    private static final int SCAN_THREADS = 10;
    private static final int PORTS_PER_THREAD = 10;

    public static void main(String[] args) {
        System.out.println("Starting aggressive port scanning attack simulation...");

        ExecutorService executor = Executors.newFixedThreadPool(SCAN_THREADS);

        for (int thread = 0; thread < SCAN_THREADS; thread++) {
            final int startPort = thread * PORTS_PER_THREAD;
            executor.submit(() -> {
                try {
                    // First try the known server port
                    attemptConnection(TARGET_PORT);

                    // Then scan nearby ports
                    for (int i = 0; i < PORTS_PER_THREAD; i++) {
                        int port = startPort + i;
                        if (port != TARGET_PORT) {
                            attemptConnection(port);
                        }
                        // Small delay between attempts
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Port scanning attack simulation completed.");
    }

    private static void attemptConnection(int port) {
        try (Socket socket = new Socket(SERVER_IP, port)) {
            System.out.println("Successfully connected to port " + port);
            // Try to send some data to trigger detection
            socket.getOutputStream().write("TEST\n".getBytes());
        } catch (Exception e) {
            System.out.println("Failed to connect to port " + port + ": " + e.getMessage());
        }
    }
}
