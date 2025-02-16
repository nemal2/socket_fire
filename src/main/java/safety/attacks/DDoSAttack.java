package safety.attacks;

import java.io.IOException;
import java.net.Socket;

public class DDoSAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        for (int i = 0; i < 50; i++) { // Simulate 100 clients
            new Thread(() -> {
                try {
                    Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                    System.out.println("DDoS Attack launched by " + Thread.currentThread().getName());
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Server overwhelmed or blocked the attack!");
                }
            }).start();
        }
    }
}