package safety.firewall;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.time.Duration;

public class MITMProtector {
    private final Map<String, Integer> attackPatterns = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastDetectionTime = new ConcurrentHashMap<>();
    private static final int ATTACK_THRESHOLD = 2;

    public boolean detectMITMAttempt(String message, String clientAddress) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lowercaseMsg = message.toLowerCase();
        boolean isAttack = false;

        // Check for known MITM patterns
        if (lowercaseMsg.contains("mitm:") ||
                lowercaseMsg.contains("cert_intercept") ||
                lowercaseMsg.contains("ssl_strip") ||
                lowercaseMsg.contains("forge_cert") ||
                lowercaseMsg.contains("proxy:ssl") ||
                message.contains("CERT:FORGE")) {

            // Increment attack counter for this client
            attackPatterns.merge(clientAddress, 1, Integer::sum);
            lastDetectionTime.put(clientAddress, Instant.now());

            // If we've seen multiple patterns, consider it an attack
            isAttack = attackPatterns.get(clientAddress) >= ATTACK_THRESHOLD;

            if (isAttack) {
                safety.server.FirewallServer.gui.updateMITMStatus(true, clientAddress);
                safety.server.FirewallServer.gui.logSecurityEvent(
                        "MITM Attack detected from " + clientAddress +
                                " (Pattern count: " + attackPatterns.get(clientAddress) + ")", true);
            }
        }

        cleanOldRecords(clientAddress);
        return isAttack;
    }

    private void cleanOldRecords(String clientAddress) {
        Instant lastDetection = lastDetectionTime.get(clientAddress);
        if (lastDetection != null &&
                Duration.between(lastDetection, Instant.now()).toMinutes() > 5) {
            attackPatterns.remove(clientAddress);
            lastDetectionTime.remove(clientAddress);
        }
    }
}