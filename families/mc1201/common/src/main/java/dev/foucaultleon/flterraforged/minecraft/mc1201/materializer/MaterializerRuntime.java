package dev.foucaultleon.flterraforged.minecraft.mc1201.materializer;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializer;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.BlockMaterializerProvider;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerContext;
import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Loader-neutral runtime holder for the materializer registry and configured provider selection. */
public final class MaterializerRuntime {

    private static volatile State state;

    private MaterializerRuntime() {
    }

    /**
     * Installs the frozen registry and configured provider exactly once.
     *
     * @param registry completed materializer registry
     * @param selectedId configured provider identifier
     */
    public static synchronized void install(MaterializerRegistry registry, String selectedId) {
        install(registry, selectedId, Map.of());
    }

    /**
     * Installs the frozen registry, configured provider and immutable provider options.
     *
     * @param registry completed materializer registry
     * @param selectedId configured provider identifier
     * @param options provider-specific options
     */
    public static synchronized void install(
            MaterializerRegistry registry,
            String selectedId,
            Map<String, String> options) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(options, "options");
        if (!registry.isFrozen()) {
            throw new IllegalStateException("Materializer registry must be frozen before installation");
        }
        String normalizedId = MaterializerRegistry.normalizeId(selectedId);
        BlockMaterializerProvider provider = registry.require(normalizedId);
        State replacement = new State(registry, normalizedId, provider, Map.copyOf(options));
        if (state != null) {
            if (!state.selectedId().equals(normalizedId)) {
                throw new IllegalStateException(
                        "Materializer runtime already installed with '" + state.selectedId()
                                + "', cannot replace it with '" + normalizedId + "'");
            }
            return;
        }
        state = replacement;
    }

    /**
     * Creates the configured materializer for one chunk-generator context.
     *
     * @param context generation context
     * @return configured materializer
     */
    public static BlockMaterializer create(MaterializerContext context) {
        State current = requireState();
        BlockMaterializer materializer = Objects.requireNonNull(
                current.provider().create(context),
                "Materializer provider returned null: " + current.provider().getClass().getName());
        if (!context.equals(materializer.context())) {
            throw new IllegalStateException(
                    "Materializer provider '" + current.selectedId()
                            + "' returned an instance bound to a different MaterializerContext");
        }
        return materializer;
    }

    /**
     * Returns the configured provider identifier.
     *
     * @return active provider identifier
     */
    public static String selectedId() {
        return requireState().selectedId();
    }


    /**
     * Returns immutable options supplied to the selected materializer provider.
     *
     * @return provider options
     */
    public static Map<String, String> options() {
        return requireState().options();
    }

    /**
     * Returns all materializer ids that were available at bootstrap.
     *
     * @return immutable identifier list
     */
    public static List<String> availableIds() {
        return requireState().registry().ids();
    }

    private static State requireState() {
        State current = state;
        if (current == null) {
            throw new IllegalStateException(
                    "FlTerraForged materializers have not been bootstrapped by the platform yet");
        }
        return current;
    }

    private record State(
            MaterializerRegistry registry,
            String selectedId,
            BlockMaterializerProvider provider,
            Map<String, String> options) {
    }
}
