package safety.attacks;

import java.io.OutputStream;
import java.net.Socket;

public class HTTPFloodAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        for (int i = 0; i < 20; i++) { // Simulate 100 HTTP requests
            new Thread(() -> {
                try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                     OutputStream out = socket.getOutputStream()) {
                    String httpRequest = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n";
                    out.write(httpRequest.getBytes());
                    System.out.println("HTTP Flood Attack request sent by " + Thread.currentThread().getName());
                } catch (Exception e) {
                    System.out.println("Server blocked the attack!");
                }
            }).start();
        }
    }
}