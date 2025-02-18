package safety.firewall;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import safety.server.FirewallServer;

public class MITMProtector {
    private final Map<String, AttackData> clientData = new ConcurrentHashMap<>();
    private final Set<String> knownCertificates = new HashSet<>();
    private static final int ATTACK_THRESHOLD = 3;
    private static final Duration DETECTION_WINDOW = Duration.ofMinutes(5);

    private static class AttackData {
        int suspiciousPatterns;
        int invalidCertificates;
        int sslStripAttempts;
        Instant lastDetectionTime;
        boolean isCurrentlyAttacking;

        AttackData() {
            this.lastDetectionTime = Instant.now();
            this.isCurrentlyAttacking = false;
        }
    }

    public MITMProtector() {
        // Initialize with known good certificate fingerprints
        initializeKnownCertificates();
    }

    private void initializeKnownCertificates() {
        // Add known good certificate fingerprints
        knownCertificates.add("valid_cert_hash_1");
        knownCertificates.add("valid_cert_hash_2");
    }

    public boolean detectMITMAttempt(String message, String clientAddress) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        AttackData data = clientData.computeIfAbsent(clientAddress, k -> new AttackData());

        // Reset detection if window has expired
        if (Duration.between(data.lastDetectionTime, Instant.now()).compareTo(DETECTION_WINDOW) > 0) {
            data = new AttackData();
            clientData.put(clientAddress, data);
        }

        boolean isAttack = analyzeMessage(message, data);

        // Update status and notify GUI if attack status changes
        if (isAttack != data.isCurrentlyAttacking) {
            data.isCurrentlyAttacking = isAttack;
            FirewallServer.gui.updateMITMStatus(isAttack, clientAddress);

            if (isAttack) {
                String details = String.format(
                        "Suspicious Patterns: %d, Invalid Certs: %d, SSL Strip Attempts: %d",
                        data.suspiciousPatterns, data.invalidCertificates, data.sslStripAttempts
                );
                FirewallServer.gui.logSecurityEvent(
                        "MITM Attack detected from " + clientAddress + " - " + details, true
                );
            } else {
                FirewallServer.gui.logSecurityEvent(
                        "MITM Attack threat from " + clientAddress + " has cleared", false
                );
            }
        }

        data.lastDetectionTime = Instant.now();
        return isAttack;
    }

    private boolean analyzeMessage(String message, AttackData data) {
        String lowercaseMsg = message.toLowerCase();

        // Check for certificate manipulation
        if (message.contains("CERT:") || lowercaseMsg.contains("certificate")) {
            if (validateCertificate(message)) {
                data.invalidCertificates++;
            }
        }

        // Check for SSL stripping attempts
        if (lowercaseMsg.contains("ssl_strip") ||
                lowercaseMsg.contains("sslstrip") ||
                lowercaseMsg.contains("http:")) {
            data.sslStripAttempts++;
        }

        // Check for other suspicious patterns
        if (lowercaseMsg.contains("mitm:") ||
                lowercaseMsg.contains("proxy:") ||
                lowercaseMsg.contains("intercept") ||
                lowercaseMsg.contains("forge")) {
            data.suspiciousPatterns++;
        }

        // Calculate threat score
        int threatScore = data.suspiciousPatterns +
                (data.invalidCertificates * 2) +
                (data.sslStripAttempts * 2);

        return threatScore >= ATTACK_THRESHOLD;
    }

    private boolean validateCertificate(String message) {
        try {
            // Extract certificate data (assuming Base64 encoded)
            int startIndex = message.indexOf("CERT:") + 5;
            int endIndex = message.indexOf(":", startIndex);
            if (endIndex == -1) endIndex = message.length();

            String certData = message.substring(startIndex, endIndex).trim();

            // Calculate certificate fingerprint
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Base64.getDecoder().decode(certData));
            String fingerprint = Base64.getEncoder().encodeToString(hash);

            // Check if this is an unknown certificate
            return !knownCertificates.contains(fingerprint);
        } catch (Exception e) {
            // If we can't validate the certificate, consider it suspicious
            return true;
        }
    }

    public void clearAttackStatus(String clientAddress) {
        clientData.remove(clientAddress);
    }

    public boolean isAttacking(String clientAddress) {
        AttackData data = clientData.get(clientAddress);
        return data != null && data.isCurrentlyAttacking;
    }
}