package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import java.util.EnumMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/** Lightweight lock-free aggregate timings for the active world-generator instance. */
final class WorldgenTelemetry {

    enum Stage {
        BIOMES,
        SNAPSHOT,
        MATERIALIZE,
        HEIGHTMAP,
        NOISE_TOTAL,
        FEATURES
    }

    private final EnumMap<Stage, Counter> counters = new EnumMap<>(Stage.class);

    WorldgenTelemetry() {
        for (Stage stage : Stage.values()) {
            counters.put(stage, new Counter());
        }
    }

    void record(Stage stage, long elapsedNanos) {
        counters.get(stage).record(Math.max(0L, elapsedNanos));
    }

    String compactSummary() {
        return String.format(
                Locale.ROOT,
                "FTF avg ms biome=%.1f snapshot=%.1f materialize=%.1f noise=%.1f features=%.1f maxNoise=%.1f",
                averageMillis(Stage.BIOMES),
                averageMillis(Stage.SNAPSHOT),
                averageMillis(Stage.MATERIALIZE),
                averageMillis(Stage.NOISE_TOTAL),
                averageMillis(Stage.FEATURES),
                maximumMillis(Stage.NOISE_TOTAL));
    }

    private double averageMillis(Stage stage) {
        Counter counter = counters.get(stage);
        long count = counter.count.sum();
        return count == 0L ? 0.0D : counter.nanos.sum() / (double) count / 1_000_000.0D;
    }

    private double maximumMillis(Stage stage) {
        return counters.get(stage).maximumNanos.get() / 1_000_000.0D;
    }

    private static final class Counter {
        private final LongAdder count = new LongAdder();
        private final LongAdder nanos = new LongAdder();
        private final AtomicLong maximumNanos = new AtomicLong();

        private void record(long elapsedNanos) {
            count.increment();
            nanos.add(elapsedNanos);
            maximumNanos.accumulateAndGet(elapsedNanos, Math::max);
        }
    }
}
