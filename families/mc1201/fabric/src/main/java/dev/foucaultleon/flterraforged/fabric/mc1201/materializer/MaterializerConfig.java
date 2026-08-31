package dev.foucaultleon.flterraforged.fabric.mc1201.materializer;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerRegistry;
import dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard.VanillaBlockMaterializerProvider;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/** Loads the server/client-wide materializer selection from the Fabric configuration directory. */
public final class MaterializerConfig {

    /** Relative configuration path below Fabric Loader's config directory. */
    public static final String RELATIVE_PATH = "flterraforged/materializer.properties";

    private static final String KEY_MATERIALIZER = "materializer";

    private final Path path;
    private final String materializerId;

    private MaterializerConfig(Path path, String materializerId) {
        this.path = path;
        this.materializerId = materializerId;
    }

    /**
     * Loads or creates the materializer configuration.
     *
     * @param configDirectory Fabric Loader configuration directory
     * @return parsed configuration
     */
    public static MaterializerConfig load(Path configDirectory) {
        Objects.requireNonNull(configDirectory, "configDirectory");
        Path path = configDirectory.resolve(RELATIVE_PATH);
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Properties defaults = new Properties();
                defaults.setProperty(KEY_MATERIALIZER, VanillaBlockMaterializerProvider.ID);
                try (Writer writer = Files.newBufferedWriter(path)) {
                    defaults.store(
                            writer,
                            "FlTerraForged block materializer. Add-on providers are registered via "
                                    + "the flterraforged:materializer Fabric entrypoint.");
                }
            }

            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            }
            String configured = properties.getProperty(
                    KEY_MATERIALIZER,
                    VanillaBlockMaterializerProvider.ID);
            return new MaterializerConfig(path, MaterializerRegistry.normalizeId(configured));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read FlTerraForged materializer config at " + path,
                    exception);
        }
    }

    /**
     * Returns the selected materializer provider identifier.
     *
     * @return selected provider id
     */
    public String materializerId() {
        return materializerId;
    }

    /**
     * Returns the backing configuration path.
     *
     * @return configuration path
     */
    public Path path() {
        return path;
    }
}
