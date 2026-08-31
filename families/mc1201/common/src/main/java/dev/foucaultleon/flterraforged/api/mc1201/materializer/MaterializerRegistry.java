package dev.foucaultleon.flterraforged.api.mc1201.materializer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Mutable-during-bootstrap registry of available block-materializer providers. */
public final class MaterializerRegistry {

    private static final Pattern ID_PATTERN = Pattern.compile(
            "[a-z0-9_.-]+:[a-z0-9_./-]+");

    private final Map<String, BlockMaterializerProvider> providers = new LinkedHashMap<>();
    private boolean frozen;

    /** Creates an empty registry. */
    public MaterializerRegistry() {
    }

    /**
     * Registers one provider.
     *
     * @param provider provider to register
     * @throws IllegalStateException when the registry is frozen or the identifier is duplicated
     */
    public synchronized void register(BlockMaterializerProvider provider) {
        if (frozen) {
            throw new IllegalStateException("Materializer registry is already frozen");
        }
        Objects.requireNonNull(provider, "provider");
        String id = normalizeId(provider.id());
        BlockMaterializerProvider previous = providers.putIfAbsent(id, provider);
        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate materializer provider id '" + id + "': "
                            + previous.getClass().getName() + " and "
                            + provider.getClass().getName());
        }
    }

    /**
     * Freezes the registry against further mutations.
     */
    public synchronized void freeze() {
        frozen = true;
    }

    /**
     * Returns whether the registry is frozen.
     *
     * @return {@code true} after bootstrap has completed
     */
    public synchronized boolean isFrozen() {
        return frozen;
    }

    /**
     * Resolves a provider or fails with the complete set of available identifiers.
     *
     * @param id configured provider identifier
     * @return registered provider
     */
    public synchronized BlockMaterializerProvider require(String id) {
        String normalized = normalizeId(id);
        BlockMaterializerProvider provider = providers.get(normalized);
        if (provider == null) {
            throw new IllegalStateException(
                    "No block materializer registered for '" + normalized
                            + "'. Available materializers: " + String.join(", ", providers.keySet()));
        }
        return provider;
    }

    /**
     * Returns an immutable ordered snapshot of registered providers.
     *
     * @return registered providers
     */
    public synchronized Collection<BlockMaterializerProvider> providers() {
        return List.copyOf(providers.values());
    }

    /**
     * Returns the registered provider identifiers in deterministic order.
     *
     * @return immutable identifier list
     */
    public synchronized List<String> ids() {
        return List.copyOf(providers.keySet());
    }

    /**
     * Validates and normalizes a provider identifier.
     *
     * @param id provider identifier
     * @return normalized identifier
     */
    public static String normalizeId(String id) {
        String normalized = Objects.requireNonNull(id, "id").trim().toLowerCase(java.util.Locale.ROOT);
        if (!ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Materializer id must be namespaced and lowercase: '" + id + "'");
        }
        return normalized;
    }
}
