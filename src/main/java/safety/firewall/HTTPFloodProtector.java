package safety.firewall;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import safety.server.FirewallServer;

public class HTTPFloodProtector {
    private static final int HTTP_REQUEST_THRESHOLD = 10; // Maximum requests per time window
    private static final int TIME_WINDOW_SECONDS = 5; // Time window in seconds
    private static final Map<String, Queue<Instant>> requestHistory = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> attackStatus = new ConcurrentHashMap<>();

    public boolean isHTTPFlood(String ipAddress, String request) {
        // Modified to detect both plaintext HTTP and encrypted HTTP (just checking the connection frequency)
        if (request == null) {
            return false;
        }

        // For encrypted connections, we may not be able to see the HTTP format,
        // so we'll focus on connection frequency from the same IP

        cleanOldRequests(ipAddress);

        Queue<Instant> requests = requestHistory.computeIfAbsent(ipAddress,
                k -> new ConcurrentLinkedQueue<>());
        requests.add(Instant.now());

        boolean isFlood = requests.size() > HTTP_REQUEST_THRESHOLD;
        if (isFlood && !attackStatus.getOrDefault(ipAddress, false)) {
            attackStatus.put(ipAddress, true);
            String message = "HTTP Flood Attack detected from " + ipAddress +
                    " (Requests: " + requests.size() + " in " + TIME_WINDOW_SECONDS + " seconds)";
            FirewallServer.gui.logSecurityEvent(message, true);

            // Update GUI to show HTTP flood attack
            FirewallServer.gui.updateHTTPFloodStatus(true, ipAddress);
        }

        return isFlood;
    }

    private void cleanOldRequests(String ipAddress) {
        Queue<Instant> requests = requestHistory.get(ipAddress);
        if (requests != null) {
            Instant cutoff = Instant.now().minus(Duration.ofSeconds(TIME_WINDOW_SECONDS));
            while (!requests.isEmpty() && requests.peek().isBefore(cutoff)) {
                requests.poll();
            }

            if (requests.isEmpty()) {
                if (attackStatus.remove(ipAddress) != null) {
                    // Reset attack status in GUI if the attack is over
                    FirewallServer.gui.updateHTTPFloodStatus(false, ipAddress);
                }
            }
        }
    }

    public boolean isAttacking(String ipAddress) {
        return attackStatus.getOrDefault(ipAddress, false);
    }

    // Clear all attack records - useful for testing
    public void clearAllRecords() {
        requestHistory.clear();
        attackStatus.clear();
    }
}