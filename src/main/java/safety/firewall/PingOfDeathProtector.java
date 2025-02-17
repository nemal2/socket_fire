package safety.firewall;

import safety.server.FirewallServer;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PingOfDeathProtector {
    private static final int PACKET_SIZE_THRESHOLD = 65000;
    private static final int TIME_WINDOW_SECONDS = 5;
    private static final int LARGE_PACKET_THRESHOLD = 10;

    private final Map<String, AtomicInteger> largePacketCounts = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastResetTime = new ConcurrentHashMap<>();
    private final Map<String, Boolean> attackStatus = new ConcurrentHashMap<>();

    public boolean isPingOfDeathAttack(String ipAddress, int packetSize) {
        // Reset counter if time window expired
        Instant now = Instant.now();
        Instant lastReset = lastResetTime.getOrDefault(ipAddress, Instant.MIN);
        if (Duration.between(lastReset, now).getSeconds() > TIME_WINDOW_SECONDS) {
            largePacketCounts.put(ipAddress, new AtomicInteger(0));
            lastResetTime.put(ipAddress, now);

            // Reset attack status if it was previously attacking
            if (attackStatus.getOrDefault(ipAddress, false)) {
                attackStatus.put(ipAddress, false);
                FirewallServer.gui.updatePoDStatus(false, ipAddress);
            }
        }

        // Count large packets
        if (packetSize >= PACKET_SIZE_THRESHOLD) {
            AtomicInteger count = largePacketCounts.computeIfAbsent(ipAddress,
                    k -> new AtomicInteger(0));
            int currentCount = count.incrementAndGet();

            // Log for debugging
            FirewallServer.gui.log("Large packet received from " + ipAddress +
                    " (Count: " + currentCount + "/" + LARGE_PACKET_THRESHOLD + ")");
        }

        // Check if threshold is reached
        boolean isAttack = largePacketCounts.getOrDefault(ipAddress, new AtomicInteger(0))
                .get() >= LARGE_PACKET_THRESHOLD;

        // Update attack status and GUI if attack status changed
        if (isAttack && !attackStatus.getOrDefault(ipAddress, false)) {
            attackStatus.put(ipAddress, true);
            FirewallServer.gui.updatePoDStatus(true, ipAddress);
        }

        return isAttack;
    }

    public boolean isAttacking(String ipAddress) {
        return attackStatus.getOrDefault(ipAddress, false);
    }
}
