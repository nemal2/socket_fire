package safety.firewall;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import safety.server.FirewallServer;

public class DDoSProtector {
    private static final int CONNECTION_THRESHOLD = 20;
    private static final int TIME_WINDOW_SECONDS = 10;
    private static final Map<String, Queue<Instant>> connectionHistory = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> attackStatus = new ConcurrentHashMap<>();

    public boolean isDDoSAttack(String ipAddress) {
        cleanOldConnections(ipAddress);

        Queue<Instant> connections = connectionHistory.computeIfAbsent(ipAddress,
                k -> new ConcurrentLinkedQueue<>());
        connections.add(Instant.now());

        boolean isDDoS = connections.size() > CONNECTION_THRESHOLD;

        // Update attack status if changed
        if (isDDoS && !attackStatus.getOrDefault(ipAddress, false)) {
            attackStatus.put(ipAddress, true);
            String message = "DDoS Attack detected from " + ipAddress +
                    " (Connections: " + connections.size() + " in " + TIME_WINDOW_SECONDS + " seconds)";
            FirewallServer.gui.logSecurityEvent(message, true);
            FirewallServer.gui.updateDDoSStatus(true, ipAddress);
        } else if (!isDDoS && attackStatus.getOrDefault(ipAddress, false)) {
            attackStatus.put(ipAddress, false);
            FirewallServer.gui.logSecurityEvent("DDoS Attack from " + ipAddress + " has stopped", false);
            FirewallServer.gui.updateDDoSStatus(false, ipAddress);
        }

        return isDDoS;
    }

    private void cleanOldConnections(String ipAddress) {
        Queue<Instant> connections = connectionHistory.get(ipAddress);
        if (connections != null) {
            Instant cutoff = Instant.now().minus(Duration.ofSeconds(TIME_WINDOW_SECONDS));
            while (!connections.isEmpty() && connections.peek().isBefore(cutoff)) {
                connections.poll();
            }

            if (connections.isEmpty()) {
                if (attackStatus.remove(ipAddress) != null) {
                    FirewallServer.gui.updateDDoSStatus(false, ipAddress);
                }
            }
        }
    }

    public boolean isAttacking(String ipAddress) {
        return attackStatus.getOrDefault(ipAddress, false);
    }

    // Clear all attack records - useful for testing
    public void clearAllRecords() {
        connectionHistory.clear();
        attackStatus.clear();
    }
}