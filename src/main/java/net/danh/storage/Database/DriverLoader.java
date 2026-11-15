package net.danh.storage.Database;

import net.danh.storage.Storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Downloads optional JDBC/Mongo drivers from Maven Central at runtime,
 * verifies them against pinned SHA-256 checksums and exposes them through
 * isolated classloaders (child of the plugin classloader).
 */
public final class DriverLoader {

    private DriverLoader() {
    }

    public static synchronized ClassLoader obtainClassLoader(Storage plugin, Library... libraries) {
        List<URL> urls = new ArrayList<>();
        try {
            Path libsDir = plugin.getDataFolder().toPath().resolve("libs");
            Files.createDirectories(libsDir);
            for (Library library : libraries) {
                Path file = libsDir.resolve(library.getFileName());
                ensureLibrary(plugin, library, file);
                urls.add(file.toUri().toURL());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to prepare driver libraries: " + e.getMessage(), e);
        }
        return new URLClassLoader("storage-drivers", urls.toArray(new URL[0]), plugin.getClass().getClassLoader());
    }

    private static void ensureLibrary(Storage plugin, Library library, Path file) throws IOException {
        if (Files.exists(file)) {
            if (sha256(file).equalsIgnoreCase(library.sha256)) {
                return;
            }
            plugin.getLogger().warning("Checksum mismatch for " + library.getFileName() + ", re-downloading...");
            Files.delete(file);
        }

        plugin.getLogger().info("Downloading driver " + library.getFileName() + " from Maven Central...");
        Path temp = file.resolveSibling(library.getFileName() + ".tmp");
        try (InputStream in = new URL(library.getUrl()).openStream()) {
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw new IOException("Failed to download " + library.getUrl()
                    + " - the server needs internet access the first time this database type is used", e);
        }

        String actual = sha256(temp);
        if (!actual.equalsIgnoreCase(library.sha256)) {
            Files.deleteIfExists(temp);
            throw new IOException("Checksum verification failed for " + library.getFileName()
                    + " (expected " + library.sha256 + ", got " + actual + ")");
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        plugin.getLogger().info("Driver " + library.getFileName() + " downloaded and verified.");
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public enum Library {
        POSTGRESQL("org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar",
                "188976721ead8e8627eb6d8389d500dccc0c9bebd885268a3047180274a6031e"),
        MARIADB("org/mariadb/jdbc/mariadb-java-client/3.4.1/mariadb-java-client-3.4.1.jar",
                "f60e4b282f1f4bdb74f0a26436ba7078a5e480b6f6702f6a7b45d9ba5e604a24"),
        H2("com/h2database/h2/2.2.224/h2-2.2.224.jar",
                "b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497"),
        MONGO_DRIVER_SYNC("org/mongodb/mongodb-driver-sync/5.2.1/mongodb-driver-sync-5.2.1.jar",
                "c10ad97ab8a148c53ab8377a3fa89e49c2f9be277f1216c7f69967b7cc1e1699"),
        MONGO_BSON("org/mongodb/bson/5.2.1/bson-5.2.1.jar",
                "5bca0022f4a933795788244b0ba3d7d92d6969730cf3b279bb37f9f7ffba5f35"),
        MONGO_DRIVER_CORE("org/mongodb/mongodb-driver-core/5.2.1/mongodb-driver-core-5.2.1.jar",
                "5ae1d49fc6b1b42046cb16c1ce9f6952e36d0203f085d9fe045266460c51da19");

        private final String path;
        private final String sha256;

        Library(String path, String sha256) {
            this.path = path;
            this.sha256 = sha256;
        }

        public String getFileName() {
            return path.substring(path.lastIndexOf('/') + 1);
        }

        public String getUrl() {
            return "https://repo.maven.apache.org/maven2/" + path;
        }
    }
}