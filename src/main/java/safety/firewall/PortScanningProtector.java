package safety.firewall;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class PortScanningProtector {
    private static final int PORT_COUNT_THRESHOLD = 10; // Number of different ports tried
    private static final int TIME_WINDOW_SECONDS = 30;

    private final Map<String, Set<Integer>> portAccessHistory = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastResetTime = new ConcurrentHashMap<>();
    private final Map<String, Boolean> attackStatus = new ConcurrentHashMap<>();

    public boolean isPortScanningAttack(String ipAddress, int port) {
        // Check if it's time to reset the set of ports
        Instant now = Instant.now();
        Instant lastReset = lastResetTime.getOrDefault(ipAddress, Instant.MIN);
        if (Duration.between(lastReset, now).getSeconds() > TIME_WINDOW_SECONDS) {
            portAccessHistory.put(ipAddress, new ConcurrentSkipListSet<>());
            lastResetTime.put(ipAddress, now);
        }

        // Add port to the set of accessed ports
        Set<Integer> ports = portAccessHistory.computeIfAbsent(ipAddress,
                k -> new ConcurrentSkipListSet<>());
        ports.add(port);

        // Check if threshold is reached
        boolean isAttack = ports.size() >= PORT_COUNT_THRESHOLD;

        // Update attack status if changed
        if (isAttack != attackStatus.getOrDefault(ipAddress, false)) {
            attackStatus.put(ipAddress, isAttack);
        }

        return isAttack;
    }

    public boolean isAttacking(String ipAddress) {
        return attackStatus.getOrDefault(ipAddress, false);
    }
}