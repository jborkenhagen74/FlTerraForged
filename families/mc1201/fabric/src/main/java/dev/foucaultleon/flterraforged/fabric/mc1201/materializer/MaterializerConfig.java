package dev.foucaultleon.flterraforged.fabric.mc1201.materializer;

import dev.foucaultleon.flterraforged.api.mc1201.materializer.MaterializerRegistry;
import dev.foucaultleon.flterraforged.minecraft.mc1201.materializer.standard.VanillaBlockMaterializerProvider;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** Loads the server/client-wide materializer selection and provider options. */
public final class MaterializerConfig {

    /** Relative configuration path below Fabric Loader's config directory. */
    public static final String RELATIVE_PATH = "flterraforged/materializer.properties";

    private static final String KEY_MATERIALIZER = "materializer";

    private final Path path;
    private final String materializerId;
    private final Map<String, String> options;

    private MaterializerConfig(Path path, String materializerId, Map<String, String> options) {
        this.path = path;
        this.materializerId = materializerId;
        this.options = Map.copyOf(options);
    }

    /**
     * Loads or creates the materializer configuration.
     *
     * <p>All keys other than {@code materializer} are passed unchanged to the selected provider.
     * The built-in vanilla provider understands the {@code blockset.*} options documented in
     * {@code MATERIALIZER-SPI.md}. Missing block-set keys keep the complete built-in palette.</p>
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
                String template = String.join(System.lineSeparator(),
                        "# FlTerraForged block materializer configuration",
                        "# Select any provider registered through the flterraforged:materializer entrypoint.",
                        KEY_MATERIALIZER + "=" + VanillaBlockMaterializerProvider.ID,
                        "",
                        "# Optional standard-materializer block sets. Omit a key to keep normal behavior.",
                        "# blockset.river_bed=minecraft:gravel,minecraft:stone",
                        "# blockset.lake_bed=minecraft:gravel",
                        "# blockset.coast=minecraft:sand",
                        "# blockset.lake_shore_dry=minecraft:sand",
                        "# blockset.lake_shore_wet=minecraft:grass_block",
                        "# blockset.riparian=minecraft:grass_block",
                        "# blockset.land_surface=minecraft:grass_block",
                        "# blockset.land_filler=minecraft:dirt",
                        "# blockset.plains=minecraft:grass_block",
                        "# blockset.valley=minecraft:grass_block,minecraft:coarse_dirt",
                        "# blockset.ocean_bed=minecraft:sand,minecraft:gravel",
                        "# blockset.hills=minecraft:grass_block,minecraft:stone",
                        "# blockset.plateau=minecraft:grass_block,minecraft:stone",
                        "# blockset.mountains=minecraft:stone",
                        "# blockset.substrate=minecraft:stone",
                        "# blockset.seal=minecraft:stone",
                        "",
                        "# Hydrology protection used by the standard materializer.",
                        "# hydrology.cave_margin=4",
                        "# hydrology.bed_seal_depth=5",
                        "# hydrology.bank_seal_depth=6",
                        "");
                Files.writeString(path, template);
            }

            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            }
            String configured = properties.getProperty(
                    KEY_MATERIALIZER,
                    VanillaBlockMaterializerProvider.ID);
            Map<String, String> options = new LinkedHashMap<>();
            for (String name : properties.stringPropertyNames()) {
                if (!KEY_MATERIALIZER.equals(name)) {
                    options.put(name, properties.getProperty(name));
                }
            }
            return new MaterializerConfig(
                    path,
                    MaterializerRegistry.normalizeId(configured),
                    options);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read FlTerraForged materializer config at " + path,
                    exception);
        }
    }

    /** Returns the selected materializer provider identifier. */
    public String materializerId() {
        return materializerId;
    }

    /** Returns immutable provider-specific materializer options. */
    public Map<String, String> options() {
        return options;
    }

    /** Returns the backing configuration path. */
    public Path path() {
        return path;
    }
}
