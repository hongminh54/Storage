package net.danh.storage.NMS;

public class NMSAssistant {

    /**
     * Method to get the NMS Version which stands for the current server-version.
     *
     * @return {@link NMSVersion}.
     */
    public NMSVersion getNMSVersion() {
        return new NMSVersion();
    }

    /**
     * Method to determine if the server version is greater than the given version.
     *
     * @param version to test against.
     * @return {@code true} if it is.
     */
    public boolean isVersionGreaterThan(int version) {
        return getNMSVersion().getMinor() > version;
    }

    /**
     * Method to determine if the server version is greater than or equal to the given version.
     *
     * @param version to test against.
     * @return {@code true} if it is.
     */
    public boolean isVersionGreaterThanOrEqualTo(int version) {
        return getNMSVersion().getMinor() >= version;
    }

    /**
     * Method to determine if the server version is less than the given version.
     *
     * @param version to test against.
     * @return {@code true} if it is.
     */
    public boolean isVersionLessThan(int version) {
        return getNMSVersion().getMinor() < version;
    }

    /**
     * Method to determine if the server version is less than the given version.
     *
     * @param version to test against.
     * @return {@code true} if it is.
     */
    public boolean isVersionLessThanOrEqualTo(int version) {
        return getNMSVersion().getMinor() <= version;
    }

    /**
     * Method to determine if the server is currently running this specific version.
     *
     * @param version to check against.
     * @return {@code true} if it is.
     */
    public boolean isVersion(int version) {
        return getNMSVersion().getMinor() == version;
    }

    /**
     * Method to determine if the sever isn't currently running this specific version.
     *
     * @param version to check against.
     * @return {@code true} if it isn't.
     */
    public boolean isNotVersion(int version) {
        return getNMSVersion().getMinor() != version;
    }
}