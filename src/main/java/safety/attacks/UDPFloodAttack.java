package safety.attacks;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPFloodAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try {
            byte[] buffer = new byte[1024]; // Small packet size
            InetAddress address = InetAddress.getByName(SERVER_IP);
            DatagramSocket socket = new DatagramSocket();

            for (int i = 0; i < 1000; i++) { // Send 1000 packets
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, SERVER_PORT);
                socket.send(packet);
                System.out.println("UDP Flood Attack packet sent: " + i);
            }
            socket.close();
        } catch (Exception e) {
            System.out.println("Server blocked the attack!");
        }
    }
}