package safety.attacks;

import java.net.Socket;

public class PortScanningAttack {
    private static final String SERVER_IP = "127.0.0.1";

    public static void main(String[] args) {
        for (int port = 1; port <= 1024; port++) {
            try (Socket socket = new Socket(SERVER_IP, port)) {
                System.out.println("Port " + port + " is open!");
            } catch (Exception e) {
                System.out.println("Port " + port + " is closed.");
            }
        }
    }
}