package safety.firewall;

import safety.server.FirewallServer;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SlowlorisProtector {
    private static final int MAX_CONCURRENT_CONNECTIONS = 50;
    private static final int MAX_HEADER_TIME = 10; // seconds
    private static final Duration CLEANUP_INTERVAL = Duration.ofSeconds(30);

    private final Map<String, ClientConnections> clientsMap = new ConcurrentHashMap<>();
    private Instant lastCleanup = Instant.now();

    private class ClientConnections {
        int connectionCount;
        Map<String, ConnectionData> connections = new ConcurrentHashMap<>();
        boolean isAttacking;

        void addConnection(String connectionId) {
            connections.put(connectionId, new ConnectionData());
            connectionCount++;
            checkAttackStatus();
        }

        void removeConnection(String connectionId) {
            connections.remove(connectionId);
            connectionCount--;
            checkAttackStatus();
        }

        private void checkAttackStatus() {
            boolean newAttackStatus = connectionCount >= MAX_CONCURRENT_CONNECTIONS;
            if (newAttackStatus != isAttacking) {
                isAttacking = newAttackStatus;
                FirewallServer.gui.updateSlowlorisStatus(isAttacking, connectionCount);
            }
        }
    }

    private class ConnectionData {
        Instant lastHeaderTime = Instant.now();
        int headerCount = 0;
    }

    public void registerConnection(String clientIP, String connectionId) {
        cleanupOldConnections();
        clientsMap.computeIfAbsent(clientIP, k -> new ClientConnections())
                .addConnection(connectionId);
    }

    public void registerHeaderReceived(String clientIP, String connectionId) {
        ClientConnections client = clientsMap.get(clientIP);
        if (client != null) {
            ConnectionData conn = client.connections.get(connectionId);
            if (conn != null) {
                conn.lastHeaderTime = Instant.now();
                conn.headerCount++;
            }
        }
    }

    public boolean isSlowlorisAttack(String clientIP, String connectionId) {
        cleanupOldConnections();

        ClientConnections client = clientsMap.get(clientIP);
        if (client == null) return false;

        ConnectionData conn = client.connections.get(connectionId);
        if (conn == null) return false;

        // Check for slow header sending
        Duration timeSinceLastHeader = Duration.between(conn.lastHeaderTime, Instant.now());
        boolean isSlow = timeSinceLastHeader.getSeconds() > MAX_HEADER_TIME;

        // Check for excessive connections
        boolean tooManyConnections = client.connectionCount >= MAX_CONCURRENT_CONNECTIONS;

        if (isSlow || tooManyConnections) {
            FirewallServer.gui.logSecurityEvent(
                    String.format("Slowloris attack detected from %s (Connections: %d, Header delay: %ds)",
                            clientIP, client.connectionCount, timeSinceLastHeader.getSeconds()),
                    true
            );
            return true;
        }

        return false;
    }

    private void cleanupOldConnections() {
        if (Duration.between(lastCleanup, Instant.now()).compareTo(CLEANUP_INTERVAL) < 0) {
            return;
        }

        lastCleanup = Instant.now();
        clientsMap.forEach((clientIP, client) -> {
            client.connections.entrySet().removeIf(entry -> {
                ConnectionData conn = entry.getValue();
                if (Duration.between(conn.lastHeaderTime, Instant.now())
                        .getSeconds() > MAX_HEADER_TIME * 2) {
                    client.connectionCount--;
                    return true;
                }
                return false;
            });

            if (client.connections.isEmpty()) {
                clientsMap.remove(clientIP);
            }
        });
    }

    public void removeConnection(String clientIP, String connectionId) {
        ClientConnections client = clientsMap.get(clientIP);
        if (client != null) {
            client.removeConnection(connectionId);
            if (client.connections.isEmpty()) {
                clientsMap.remove(clientIP);
            }
        }
    }
}