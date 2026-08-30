package dev.foucaultleon.flterraforged.engine.api;

/**
 * Semantic version of the engine API contract.
 *
 * <p>Before API 1.0, all compatibility guarantees are provisional.</p>
 */
public record EngineApiVersion(int major, int minor, int patch)
        implements Comparable<EngineApiVersion> {

    public static final EngineApiVersion CURRENT = new EngineApiVersion(0, 1, 0);

    public EngineApiVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version components must be non-negative");
        }
    }

    public boolean hasSameMajor(EngineApiVersion other) {
        return other != null && major == other.major;
    }

    @Override
    public int compareTo(EngineApiVersion other) {
        int result = Integer.compare(major, other.major);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(minor, other.minor);
        if (result != 0) {
            return result;
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
