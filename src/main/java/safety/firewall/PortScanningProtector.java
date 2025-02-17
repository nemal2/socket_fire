package safety.firewall;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PortScanningProtector {
    private static final int PORT_SCAN_THRESHOLD = 5;
    private static final int TIME_WINDOW_SECONDS = 5;
    private final Map<String, PortScanData> scanHistory = new ConcurrentHashMap<>();

    private class PortScanData {
        Instant firstAttempt;
        Set<Integer> ports;  // Track unique ports
        boolean attackReported;

        PortScanData() {
            this.firstAttempt = Instant.now();
            this.ports = new HashSet<>();
            this.attackReported = false;
        }
    }

    public boolean isPortScanningAttack(String ipAddress, int port) {
        cleanOldRecords();

        PortScanData data = scanHistory.computeIfAbsent(ipAddress, k -> new PortScanData());

        // Reset if time window expired
        if (Duration.between(data.firstAttempt, Instant.now()).getSeconds() > TIME_WINDOW_SECONDS) {
            data.firstAttempt = Instant.now();
            data.ports.clear();
            data.attackReported = false;
            return false;
        }

        // Add new port to set of scanned ports
        data.ports.add(port);

        // Only consider it a port scan if multiple unique ports are tried
        if (data.ports.size() >= PORT_SCAN_THRESHOLD && !data.attackReported) {
            data.attackReported = true;
            return true;
        }

        return data.attackReported;
    }

    private void cleanOldRecords() {
        Instant cutoff = Instant.now().minus(Duration.ofSeconds(TIME_WINDOW_SECONDS));
        scanHistory.entrySet().removeIf(entry ->
                entry.getValue().firstAttempt.isBefore(cutoff));
    }

    public boolean isAttacking(String ipAddress) {
        PortScanData data = scanHistory.get(ipAddress);
        return data != null && data.attackReported;
    }
}
