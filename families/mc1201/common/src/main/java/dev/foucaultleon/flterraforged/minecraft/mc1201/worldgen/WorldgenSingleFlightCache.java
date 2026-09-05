package dev.foucaultleon.flterraforged.minecraft.mc1201.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Bounded completed-value cache that coalesces concurrent world-generation misses per key.
 *
 * <p>The owning caller computes a missing value synchronously on its current thread. The cache does
 * not submit work to Minecraft's world-generation executor and does not hold the completed-value
 * monitor while a loader runs. Other callers requesting the same key reuse the owner's future,
 * preventing the duplicate Engine environment probes that previously occurred under cold parallel
 * structure checks.</p>
 *
 * <p>Loaders must form an acyclic dependency graph. A same-thread request for a key already owned by
 * that thread fails immediately instead of waiting on itself.</p>
 *
 * @param <K> cache key type
 * @param <V> cached value type
 */
final class WorldgenSingleFlightCache<K, V> {

    private final BoundedMap<K, V> completed;
    private final ConcurrentHashMap<K, Flight<V>> inFlight = new ConcurrentHashMap<>();

    /**
     * Creates a bounded cache.
     *
     * @param maximumSize maximum number of completed values retained
     */
    WorldgenSingleFlightCache(int maximumSize) {
        if (maximumSize < 1) {
            throw new IllegalArgumentException("maximumSize must be >= 1");
        }
        this.completed = new BoundedMap<>(maximumSize);
    }

    /**
     * Returns a completed value or directly computes the missing key once.
     *
     * @param key canonical cache key
     * @param loader deterministic loader for a missing key
     * @return completed shared value
     */
    V get(K key, Supplier<? extends V> loader) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(loader, "loader");

        V cached = completed(key);
        if (cached != null) {
            return cached;
        }

        Thread current = Thread.currentThread();
        Flight<V> mine = new Flight<>(current);
        Flight<V> existing = inFlight.putIfAbsent(key, mine);
        if (existing != null) {
            if (existing.owner == current) {
                throw new IllegalStateException("Recursive worldgen single-flight load for key " + key);
            }
            return await(existing.future);
        }

        try {
            cached = completed(key);
            if (cached != null) {
                mine.future.complete(cached);
                return cached;
            }

            V loaded = Objects.requireNonNull(loader.get(), "cache loader returned null");
            V retained;
            synchronized (completed) {
                V secondLook = completed.get(key);
                if (secondLook == null) {
                    completed.put(key, loaded);
                    retained = loaded;
                } else {
                    retained = secondLook;
                }
            }
            mine.future.complete(retained);
            return retained;
        } catch (Throwable throwable) {
            mine.future.completeExceptionally(throwable);
            throw propagate(throwable);
        } finally {
            inFlight.remove(key, mine);
        }
    }

    /**
     * Returns the number of completed retained values.
     *
     * @return bounded completed-cache size
     */
    int completedSize() {
        synchronized (completed) {
            return completed.size();
        }
    }

    private V completed(K key) {
        synchronized (completed) {
            return completed.get(key);
        }
    }

    private static <V> V await(CompletableFuture<V> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            throw propagate(cause == null ? exception : cause);
        }
    }

    private static RuntimeException propagate(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        return new IllegalStateException("Worldgen single-flight cache loader failed", throwable);
    }

    private static final class Flight<V> {

        private final Thread owner;
        private final CompletableFuture<V> future = new CompletableFuture<>();

        Flight(Thread owner) {
            this.owner = owner;
        }
    }

    private static final class BoundedMap<K, V> extends LinkedHashMap<K, V> {

        private static final long serialVersionUID = 1L;
        private final int maximumSize;

        BoundedMap(int maximumSize) {
            super(maximumSize + 1, 0.75F, true);
            this.maximumSize = maximumSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maximumSize;
        }
    }
}
