package dev.foucaultleon.flterraforged.api.mc1201.materializer;

/**
 * Factory discovered from FlTerraForged's loader-specific materializer extension point.
 *
 * <p>Fabric add-ons register an implementation as a {@code flterraforged:materializer}
 * entrypoint. The provider identifier is then selectable in
 * {@code config/flterraforged/materializer.properties}.</p>
 */
public interface BlockMaterializerProvider {

    /**
     * Returns the globally unique namespaced provider identifier.
     *
     * @return identifier such as {@code example:conquest}
     */
    String id();

    /**
     * Returns a human-readable provider name used in diagnostics.
     *
     * @return display name
     */
    default String displayName() {
        return id();
    }

    /**
     * Creates one materializer for the supplied generation context.
     *
     * @param context immutable generation context
     * @return materializer instance
     */
    BlockMaterializer create(MaterializerContext context);
}
