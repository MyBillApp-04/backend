package com.mybill.MyBill_Backend.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for JVM memory utilization.
 *
 * <p>Reports {@code DEGRADED} status (mapped to {@code DOWN} in Actuator) when
 * heap usage exceeds 90 % of committed heap to give Prometheus an early-warning
 * signal before an {@code OutOfMemoryError} occurs.</p>
 */
@Component("jvmMemory")
public class JvmMemoryHealthIndicator implements HealthIndicator {

    private static final double CRITICAL_HEAP_RATIO = 0.90;

    @Override
    public Health health() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory   = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory  = runtime.freeMemory();
        long usedMemory  = totalMemory - freeMemory;

        double heapUsageRatio = maxMemory > 0 ? (double) usedMemory / maxMemory : 0.0;
        long usedMb  = usedMemory  / (1024 * 1024);
        long maxMb   = maxMemory   / (1024 * 1024);

        Health.Builder builder = heapUsageRatio > CRITICAL_HEAP_RATIO
                ? Health.down()
                : Health.up();

        return builder
                .withDetail("heap.used_mb", usedMb)
                .withDetail("heap.max_mb", maxMb)
                .withDetail("heap.usage_ratio", String.format("%.2f", heapUsageRatio))
                .withDetail("threshold", CRITICAL_HEAP_RATIO)
                .build();
    }
}
