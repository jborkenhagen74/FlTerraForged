package dev.foucaultleon.flterraforged.engine.api;

import java.util.Objects;

/**
 * Semantic version of the engine API contract.
 *
 * <p>Before API 1.0, all compatibility guarantees are provisional.</p>
 *
 * @param major major API version
 * @param minor minor API version
 * @param patch patch API version
 */
public record EngineApiVersion(int major, int minor, int patch)
        implements Comparable<EngineApiVersion> {

    /** Current engine API version implemented by this artifact. */
    public static final EngineApiVersion CURRENT = new EngineApiVersion(0, 1, 1);

    /**
     * Creates and validates an API version.
     *
     * @param major major API version
     * @param minor minor API version
     * @param patch patch API version
     * @throws IllegalArgumentException if a version component is negative
     */
    public EngineApiVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version components must be non-negative");
        }
    }

    /**
     * Tests whether another API version has the same major version.
     *
     * @param other version to compare with
     * @return {@code true} when {@code other} is non-null and has the same major version
     */
    public boolean hasSameMajor(EngineApiVersion other) {
        return other != null && major == other.major;
    }

    /**
     * Compares API versions lexicographically by major, minor and patch number.
     *
     * @param other version to compare with
     * @return a negative value, zero, or a positive value according to the version ordering
     */
    @Override
    public int compareTo(EngineApiVersion other) {
        Objects.requireNonNull(other, "other");
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

    /**
     * Returns the dotted semantic representation of this API version.
     *
     * @return version in {@code major.minor.patch} form
     */
    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
