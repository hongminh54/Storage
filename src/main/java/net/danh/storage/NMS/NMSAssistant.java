package net.danh.storage.NMS;

/**
 * Optimized NMS version assistant with cached version checking
 */
public class NMSAssistant {

    // Cache the version for performance
    private static final NMSVersion CACHED_VERSION = new NMSVersion();
    private static final int MINOR_VERSION = CACHED_VERSION.getMinor();

    /**
     * Get cached NMS version
     */
    public NMSVersion getNMSVersion() {
        return CACHED_VERSION;
    }

    /**
     * Check if server version is greater than given version (cached)
     */
    public boolean isVersionGreaterThan(int version) {
        return MINOR_VERSION > version;
    }

    /**
     * Check if server version is greater than or equal to given version (cached)
     */
    public boolean isVersionGreaterThanOrEqualTo(int version) {
        return MINOR_VERSION >= version;
    }

    /**
     * Check if server version is less than given version (cached)
     */
    public boolean isVersionLessThan(int version) {
        return MINOR_VERSION < version;
    }

    /**
     * Check if server version is less than or equal to given version (cached)
     */
    public boolean isVersionLessThanOrEqualTo(int version) {
        return MINOR_VERSION <= version;
    }

    /**
     * Check if server is running specific version (cached)
     */
    public boolean isVersion(int version) {
        return MINOR_VERSION == version;
    }

    /**
     * Check if server is not running specific version (cached)
     */
    public boolean isNotVersion(int version) {
        return MINOR_VERSION != version;
    }
}