package safety.firewall;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PingOfDeathProtector {
    private static final int PACKET_SIZE_THRESHOLD = 60000; // Large packet size threshold
    private static final int TIME_WINDOW_SECONDS = 10;
    private static final int LARGE_PACKET_THRESHOLD = 5; // Number of large packets in window to trigger alert

    private final Map<String, AtomicInteger> largePacketCounts = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastResetTime = new ConcurrentHashMap<>();
    private final Map<String, Boolean> attackStatus = new ConcurrentHashMap<>();

    public boolean isPingOfDeathAttack(String ipAddress, int packetSize) {
        // Check if it's time to reset the counter
        Instant now = Instant.now();
        Instant lastReset = lastResetTime.getOrDefault(ipAddress, Instant.MIN);
        if (Duration.between(lastReset, now).getSeconds() > TIME_WINDOW_SECONDS) {
            largePacketCounts.put(ipAddress, new AtomicInteger(0));
            lastResetTime.put(ipAddress, now);
        }

        // Increment counter if packet is large
        if (packetSize >= PACKET_SIZE_THRESHOLD) {
            AtomicInteger count = largePacketCounts.computeIfAbsent(ipAddress,
                    k -> new AtomicInteger(0));
            count.incrementAndGet();
        }

        // Check if threshold is reached
        boolean isAttack = largePacketCounts.getOrDefault(ipAddress, new AtomicInteger(0))
                .get() >= LARGE_PACKET_THRESHOLD;

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