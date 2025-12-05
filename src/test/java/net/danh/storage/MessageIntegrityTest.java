package net.danh.storage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.io.File.separator;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Message Key Integrity Tests")
class MessageIntegrityTest {

    private static final Path SOURCE_ROOT = Paths.get("src/main/java");
    private static final Path MESSAGE_YML = Paths.get("src/main/resources/message.yml");

    // Patterns to extract message keys from Java source code
    // Pattern 1: File.getMessage().getString("key") or File.getMessage().getString("key", default)
    private static final Pattern DIRECT_GET_STRING = Pattern.compile(
            "File\\.getMessage\\(\\)\\.getString\\s*\\(\\s*\"([^\"]+)\"");

    // Pattern 2: File.getMessage().getStringList("key")
    private static final Pattern DIRECT_GET_STRING_LIST = Pattern.compile(
            "File\\.getMessage\\(\\)\\.getStringList\\s*\\(\\s*\"([^\"]+)\"");

    // Pattern 3: sendMessage(sender, "key") or sendMessage(sender, "key", ...)
    private static final Pattern SEND_MESSAGE = Pattern.compile(
            "sendMessage\\s*\\([^,]+,\\s*\"([^\"]+)\"");

    // Pattern 4: sendMessageList(sender, "key")
    private static final Pattern SEND_MESSAGE_LIST = Pattern.compile(
            "sendMessageList\\s*\\([^,]+,\\s*\"([^\"]+)\"");

    // Pattern 5: sendColorizedMessage(sender, "key") variants
    private static final Pattern SEND_COLORIZED_MESSAGE = Pattern.compile(
            "sendColorizedMessage\\s*\\([^,]+,\\s*\"([^\"]+)\"");

    // Pattern 6: sendColorizedMessageList(sender, "key")
    private static final Pattern SEND_COLORIZED_MESSAGE_LIST = Pattern.compile(
            "sendColorizedMessageList\\s*\\([^,]+,\\s*\"([^\"]+)\"");

    private static Set<String> ymlKeys;
    private static Map<String, List<KeyLocation>> codeKeys;

    @BeforeAll
    static void setup() throws IOException {
        assertTrue(Files.exists(SOURCE_ROOT),
                "Source directory not found: " + SOURCE_ROOT.toAbsolutePath());
        assertTrue(Files.exists(MESSAGE_YML),
                "message.yml not found: " + MESSAGE_YML.toAbsolutePath());

        ymlKeys = loadYmlKeys();
        codeKeys = scanSourceCode();

        assertFalse(ymlKeys.isEmpty(), "No keys found in message.yml - file may be empty or malformed");
    }

    private static Set<String> loadYmlKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        Yaml yaml = new Yaml();

        try (InputStream is = Files.newInputStream(MESSAGE_YML)) {
            Map<String, Object> data = yaml.load(is);
            if (data != null) {
                extractKeys(data, "", keys);
            }
        }

        return keys;
    }

    @SuppressWarnings("unchecked")
    private static void extractKeys(Map<String, Object> map, String prefix, Set<String> keys) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                // Recurse into nested maps
                extractKeys((Map<String, Object>) value, key, keys);
            } else {
                // Leaf node - this is a valid key
                keys.add(key);
            }
        }
    }

    private static Map<String, List<KeyLocation>> scanSourceCode() throws IOException {
        Map<String, List<KeyLocation>> keys = new HashMap<>();

        Files.walkFileTree(SOURCE_ROOT, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    scanJavaFile(file, keys);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return keys;
    }

    private static void scanJavaFile(Path file, Map<String, List<KeyLocation>> keys) throws IOException {
        List<String> lines = Files.readAllLines(file);
        String relativePath = SOURCE_ROOT.relativize(file).toString();

        String keyPrefix = detectKeyPrefix(file, lines);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;

            // Skip comments
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                continue;
            }

            // Apply all patterns
            extractKeysFromLine(DIRECT_GET_STRING, line, relativePath, lineNumber, keys, "");
            extractKeysFromLine(DIRECT_GET_STRING_LIST, line, relativePath, lineNumber, keys, "");
            extractKeysFromLine(SEND_MESSAGE, line, relativePath, lineNumber, keys, keyPrefix);
            extractKeysFromLine(SEND_MESSAGE_LIST, line, relativePath, lineNumber, keys, keyPrefix);
            extractKeysFromLine(SEND_COLORIZED_MESSAGE, line, relativePath, lineNumber, keys, keyPrefix);
            extractKeysFromLine(SEND_COLORIZED_MESSAGE_LIST, line, relativePath, lineNumber, keys, keyPrefix);
        }
    }

    private static String detectKeyPrefix(Path file, List<String> lines) {
        String filePath = file.toString();
        String fileName = file.getFileName().toString();

        if (filePath.contains("CMD" + separator + "handler" + separator + "mythic")) {
            if (fileName.equals("MythicCommand.java")) {
                return "mythicstorage.";
            }
            for (String line : lines) {
                if (line.contains("extends MythicCommand")) {
                    return "mythicstorage.";
                }
            }
        }
        return "";
    }

    private static void extractKeysFromLine(Pattern pattern, String line, String filePath,
                                            int lineNumber, Map<String, List<KeyLocation>> keys,
                                            String keyPrefix) {
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            String key = matcher.group(1);

            // Skip empty keys or keys that are clearly variables
            if (key.isEmpty() || key.contains("+") || key.startsWith("$")) {
                continue;
            }

            String fullKey = keyPrefix.isEmpty() ? key : keyPrefix + key;

            keys.computeIfAbsent(fullKey, k -> new ArrayList<>())
                    .add(new KeyLocation(filePath, lineNumber, line));
        }
    }

    private static boolean isDynamicKey(String key) {
        // Keys with string concatenation or variable references
        if (key.contains("+") || key.contains("$") || key.contains("{")) {
            return true;
        }
        // Keys ending with a dot are prefixes for dynamic construction
        if (key.endsWith(".")) {
            return true;
        }
        // Placeholder patterns (e.g., #material#, <amount>)
        if (key.startsWith("#") || key.startsWith("<") || key.startsWith("%")) {
            return true;
        }
        // URLs are not message keys
        if (key.startsWith("http://") || key.startsWith("https://")) {
            return true;
        }
        // Keys that are clearly not message paths
        return !key.contains(".") && key.length() < 3;
    }

    private static boolean isSystemKey(String key) {
        return key.equals("message_version") ||
                key.startsWith("_") ||
                key.equals("prefix");
    }

    @Test
    @DisplayName("All message keys used in code must exist in message.yml")
    void testMissingKeys() {
        List<String> missingKeys = new ArrayList<>();
        StringBuilder report = new StringBuilder();

        for (Map.Entry<String, List<KeyLocation>> entry : codeKeys.entrySet()) {
            String key = entry.getKey();

            if (isDynamicKey(key)) {
                continue;
            }

            if (!ymlKeys.contains(key)) {
                missingKeys.add(key);
                report.append("\n\nMISSING KEY: \"").append(key).append("\"\n");
                report.append("   Used in:\n");
                for (KeyLocation loc : entry.getValue()) {
                    report.append("   • ").append(loc).append("\n");
                }
            }
        }

        if (!missingKeys.isEmpty()) {
            fail(String.format(
                    "\n\n========================================\n" +
                            "MESSAGE KEY INTEGRITY CHECK FAILED\n" +
                            "========================================\n" +
                            "Found %d missing message key(s) in message.yml!\n" +
                            "These keys are used in code but not defined in message.yml.\n" +
                            "%s\n" +
                            "========================================\n" +
                            "ACTION REQUIRED: Add the missing keys to src/main/resources/message.yml\n" +
                            "========================================",
                    missingKeys.size(), report));
        }
    }

    @Test
    @DisplayName("Report stale keys in message.yml (not used in code)")
    void testStaleKeys() {
        Set<String> usedKeys = codeKeys.keySet();
        List<String> staleKeys = ymlKeys.stream()
                .filter(key -> !usedKeys.contains(key))
                .filter(key -> !isSystemKey(key))
                .sorted()
                .collect(Collectors.toList());

        if (!staleKeys.isEmpty()) {
            StringBuilder report = new StringBuilder();
            report.append("\n\nWARNING: Found ").append(staleKeys.size())
                    .append(" potentially stale key(s) in message.yml:\n");

            Map<String, List<String>> grouped = staleKeys.stream()
                    .collect(Collectors.groupingBy(k -> k.contains(".") ? k.split("\\.")[0] : k));

            for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
                report.append("\n[").append(entry.getKey()).append("]\n");
                for (String key : entry.getValue()) {
                    report.append("  • ").append(key).append("\n");
                }
            }

            report.append("\nNote: These keys may be used dynamically or by external plugins.\n");
            report.append("Review and remove if truly unused to keep message.yml clean.\n");

            // This is a warning, not a failure - just log it
            System.out.println(report);
        }
    }

    @Test
    @DisplayName("Message keys should follow naming convention")
    void testKeyNamingConvention() {
        List<String> invalidKeys = new ArrayList<>();

        for (String key : codeKeys.keySet()) {
            if (isDynamicKey(key)) continue;

            // Keys should be lowercase with dots and underscores
            if (!key.matches("^[a-z][a-z0-9_.]*$")) {
                invalidKeys.add(key);
            }
        }

        if (!invalidKeys.isEmpty()) {
            System.out.println("\nWARNING: Keys with non-standard naming convention:");
            invalidKeys.forEach(k -> System.out.println("  • " + k));
        }
    }

    @Test
    @DisplayName("Summary: Code coverage statistics")
    void testCoverageStatistics() {
        int totalYmlKeys = ymlKeys.size();
        int totalCodeKeys = codeKeys.size();
        int matchedKeys = (int) codeKeys.keySet().stream()
                .filter(ymlKeys::contains)
                .count();

        double coveragePercent = totalCodeKeys > 0
                ? (matchedKeys * 100.0 / totalCodeKeys)
                : 100.0;

        System.out.println("\n========================================");
        System.out.println("MESSAGE KEY COVERAGE STATISTICS");
        System.out.println("========================================");
        System.out.printf("Keys defined in message.yml: %d%n", totalYmlKeys);
        System.out.printf("Keys referenced in code:     %d%n", totalCodeKeys);
        System.out.printf("Keys matched:                %d%n", matchedKeys);
        System.out.printf("Coverage:                    %.1f%%%n", coveragePercent);
        System.out.println("========================================\n");
    }

    static class KeyLocation {
        final String filePath;
        final int lineNumber;
        final String lineContent;

        KeyLocation(String filePath, int lineNumber, String lineContent) {
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.lineContent = lineContent.trim();
        }

        @Override
        public String toString() {
            return String.format("%s:%d -> %s", filePath, lineNumber,
                    lineContent.length() > 80 ? lineContent.substring(0, 77) + "..." : lineContent);
        }
    }
}
