// Update the UDPHandler to properly handle packets:
package safety.server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import safety.firewall.PingOfDeathProtector;

public class UDPHandler extends Thread {
    private final DatagramSocket socket;
    private final PingOfDeathProtector podProtector;
    private static final int BUFFER_SIZE = 65535;
    private volatile boolean running = true;

    public UDPHandler(int port) throws IOException {
        this.socket = new DatagramSocket(port);
        this.podProtector = FirewallServer.getPodProtector();
        FirewallServer.gui.log("UDP Handler initialized on port " + port);
    }

    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String clientAddress = packet.getAddress().getHostAddress();
                int packetSize = packet.getLength();

                FirewallServer.gui.log("Received UDP packet from " + clientAddress +
                        " (size: " + packetSize + " bytes)");

                // Check for Ping of Death attack
                if (podProtector.isPingOfDeathAttack(clientAddress, packetSize)) {
                    FirewallServer.addToBlacklist(clientAddress);
                    // Response packet to indicate blocking
                    byte[] response = "BLOCKED: Ping of Death Attack Detected".getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                            response, response.length, packet.getAddress(), packet.getPort());
                    socket.send(responsePacket);
                    continue;
                }

                // Process legitimate packets
                processPacket(packet);

            } catch (IOException e) {
                if (running) {
                    FirewallServer.gui.log("Error in UDP handler: " + e.getMessage());
                }
            }
        }
    }
    private void processPacket(DatagramPacket packet) {
        // Handle legitimate packets
        String clientAddress = packet.getAddress().getHostAddress();
        FirewallServer.gui.log("Processed legitimate UDP packet from: " + clientAddress);
    }

    public void shutdown() {
        running = false;
        socket.close();
    }
}