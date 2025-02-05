package safety.attacks;

import java.io.IOException;
import java.net.Socket;

public class MITMAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            System.out.println("MITM Attack: Connected to server");
            // Simulate intercepting and modifying data
            socket.getOutputStream().write("Malicious data".getBytes());
            System.out.println("MITM Attack: Sent malicious data");
        } catch (IOException e) {
            System.out.println("Server blocked the attack!");
        }
    }
}
