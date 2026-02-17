package net.danh.storage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

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
    private static Map<String, YmlValueType> ymlKeyTypes;
    private static Map<String, List<KeyLocation>> codeKeys;
    private static Map<String, Set<CodeKeyType>> codeKeyTypes;

    @BeforeAll
    static void setup() throws IOException {
        assertTrue(Files.exists(SOURCE_ROOT),
                "Source directory not found: " + SOURCE_ROOT.toAbsolutePath());
        assertTrue(Files.exists(MESSAGE_YML),
                "message.yml not found: " + MESSAGE_YML.toAbsolutePath());

        ymlKeyTypes = loadYmlKeyTypes();
        ymlKeys = new HashSet<>(ymlKeyTypes.keySet());
        codeKeyTypes = new HashMap<>();
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

    private static Map<String, YmlValueType> loadYmlKeyTypes() throws IOException {
        Map<String, YmlValueType> keyTypes = new HashMap<>();
        Yaml yaml = createStrictYaml();

        try (InputStream is = Files.newInputStream(MESSAGE_YML)) {
            Object loaded = yaml.load(is);
            if (loaded instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) loaded;
                extractKeyTypes(data, "", keyTypes);
            }
        } catch (YAMLException e) {
            fail(String.format(
                    "\n\n========================================\n" +
                            "MESSAGE.YML PARSE FAILED\n" +
                            "========================================\n" +
                            "Failed to parse src/main/resources/message.yml.\n" +
                            "This can happen due to malformed YAML or duplicate keys.\n" +
                            "\nError: %s\n" +
                            "========================================\n" +
                            "ACTION REQUIRED: Fix YAML syntax and remove duplicates.\n" +
                            "========================================\n",
                    e.getMessage()),
                    e);
        }

        return keyTypes;
    }

    private static Yaml createStrictYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        return new Yaml(new SafeConstructor(options));
    }

    @SuppressWarnings("unchecked")
    private static void extractKeyTypes(Map<String, Object> map, String prefix,
                                        Map<String, YmlValueType> keyTypes) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String childKey = String.valueOf(entry.getKey());
            String key = prefix.isEmpty() ? childKey : prefix + "." + childKey;
            Object value = entry.getValue();

            if (value instanceof Map) {
                extractKeyTypes((Map<String, Object>) value, key, keyTypes);
            } else if (value instanceof List) {
                keyTypes.put(key, YmlValueType.LIST);
            } else if (value instanceof String) {
                keyTypes.put(key, YmlValueType.STRING);
            } else {
                keyTypes.put(key, YmlValueType.OTHER);
            }
        }
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

        String content = joinLines(lines);
        List<Integer> lineStartOffsets = calculateLineStartOffsets(lines);
        String commentFreeContent = stripCommentsPreserveLines(content);

        extractKeysFromContent(DIRECT_GET_STRING, commentFreeContent, relativePath,
                lineStartOffsets, lines, keys, "", CodeKeyType.STRING);
        extractKeysFromContent(DIRECT_GET_STRING_LIST, commentFreeContent, relativePath,
                lineStartOffsets, lines, keys, "", CodeKeyType.LIST);
        extractKeysFromContent(SEND_MESSAGE, commentFreeContent, relativePath,
                lineStartOffsets, lines, keys, keyPrefix, CodeKeyType.STRING);
        extractKeysFromContent(SEND_MESSAGE_LIST, commentFreeContent, relativePath,
                lineStartOffsets, lines, keys, keyPrefix, CodeKeyType.LIST);
        extractKeysFromContent(SEND_COLORIZED_MESSAGE, commentFreeContent, relativePath,
                lineStartOffsets, lines, keys, keyPrefix, CodeKeyType.STRING);
        extractKeysFromContent(SEND_COLORIZED_MESSAGE_LIST, commentFreeContent, relativePath,
                lineStartOffsets, lines, keys, keyPrefix, CodeKeyType.LIST);
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

        if (filePath.contains("CMD" + separator + "handler" + separator + "crop")) {
            if (fileName.equals("CropCommand.java")) {
                return "cropstorage.";
            }
            for (String line : lines) {
                if (line.contains("extends CropCommand")) {
                    return "cropstorage.";
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

    private static void extractKeysFromContent(Pattern pattern, String content, String filePath,
                                               List<Integer> lineStartOffsets,
                                               List<String> lines,
                                               Map<String, List<KeyLocation>> keys,
                                               String keyPrefix,
                                               CodeKeyType codeKeyType) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            if (isSendMethodPattern(pattern) &&
                    isDisallowedQualifiedSendCall(content, matcher.start())) {
                continue;
            }

            String key = matcher.group(1);

            if (key == null || key.isEmpty() || key.contains("+") ||
                    key.startsWith("$")) {
                continue;
            }

            if (isFollowedByConcatenation(content, matcher.end())) {
                continue;
            }

            String fullKey = keyPrefix.isEmpty() ? key : keyPrefix + key;
            int lineNumber = findLineNumber(lineStartOffsets, matcher.start());
            String lineContent = getLineSafely(lines, lineNumber);

            keys.computeIfAbsent(fullKey, k -> new ArrayList<>())
                    .add(new KeyLocation(filePath, lineNumber, lineContent));
            codeKeyTypes.computeIfAbsent(fullKey, k -> new HashSet<>())
                    .add(codeKeyType);
        }
    }

    private static boolean isSendMethodPattern(Pattern pattern) {
        return pattern == SEND_MESSAGE ||
                pattern == SEND_MESSAGE_LIST ||
                pattern == SEND_COLORIZED_MESSAGE ||
                pattern == SEND_COLORIZED_MESSAGE_LIST;
    }

    private static boolean isDisallowedQualifiedSendCall(String content,
                                                         int matchStart) {
        int i = matchStart - 1;
        while (i >= 0 && Character.isWhitespace(content.charAt(i))) {
            i--;
        }

        if (i < 0 || content.charAt(i) != '.') {
            return false;
        }

        i--;
        while (i >= 0 && Character.isWhitespace(content.charAt(i))) {
            i--;
        }

        int end = i;
        while (i >= 0 &&
                (Character.isJavaIdentifierPart(content.charAt(i)) ||
                        content.charAt(i) == '$')) {
            i--;
        }

        String identifier = content.substring(i + 1, end + 1);
        return !"this".equals(identifier) && !"super".equals(identifier);
    }

    private static String joinLines(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(lines.get(i));
        }
        return builder.toString();
    }

    private static List<Integer> calculateLineStartOffsets(List<String> lines) {
        List<Integer> offsets = new ArrayList<>(lines.size());
        int offset = 0;
        for (int i = 0; i < lines.size(); i++) {
            offsets.add(offset);
            offset += lines.get(i).length();
            if (i < lines.size() - 1) {
                offset += 1;
            }
        }
        return offsets;
    }

    private static int findLineNumber(List<Integer> lineStartOffsets, int index) {
        int low = 0;
        int high = lineStartOffsets.size() - 1;
        int result = 0;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int start = lineStartOffsets.get(mid);
            if (start <= index) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return result + 1;
    }

    private static String getLineSafely(List<String> lines, int lineNumber) {
        if (lines.isEmpty()) {
            return "";
        }
        int index = Math.max(0, Math.min(lines.size() - 1, lineNumber - 1));
        return lines.get(index);
    }

    private static boolean isFollowedByConcatenation(String content, int matchEnd) {
        int i = matchEnd;
        while (i < content.length()) {
            char c = content.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            return c == '+';
        }
        return false;
    }

    private static String stripCommentsPreserveLines(String source) {
        StringBuilder result = new StringBuilder(source.length());

        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    result.append(c);
                } else {
                    result.append(' ');
                }
                continue;
            }

            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    result.append(' ');
                    result.append(' ');
                    i++;
                    inBlockComment = false;
                } else if (c == '\n') {
                    result.append('\n');
                } else {
                    result.append(' ');
                }
                continue;
            }

            if (inString) {
                result.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (inChar) {
                result.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '\'') {
                    inChar = false;
                }
                continue;
            }

            if (c == '/' && next == '/') {
                result.append(' ');
                result.append(' ');
                i++;
                inLineComment = true;
                continue;
            }

            if (c == '/' && next == '*') {
                result.append(' ');
                result.append(' ');
                i++;
                inBlockComment = true;
                continue;
            }

            if (c == '"') {
                inString = true;
                result.append(c);
                continue;
            }

            if (c == '\'') {
                inChar = true;
                result.append(c);
                continue;
            }

            result.append(c);
        }

        return result.toString();
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
    @DisplayName("Message key usage types (String vs List) must match message.yml")
    void testKeyTypeConsistency() {
        List<String> invalidKeys = new ArrayList<>();
        StringBuilder report = new StringBuilder();

        for (Map.Entry<String, Set<CodeKeyType>> entry : codeKeyTypes.entrySet()) {
            String key = entry.getKey();
            Set<CodeKeyType> usageTypes = entry.getValue();

            if (isDynamicKey(key)) {
                continue;
            }

            if (usageTypes == null || usageTypes.isEmpty()) {
                continue;
            }

            YmlValueType ymlType = ymlKeyTypes.get(key);
            if (ymlType == null) {
                continue;
            }

            if (usageTypes.size() > 1) {
                invalidKeys.add(key);
                report.append("\n\nINCONSISTENT CODE USAGE: \"")
                        .append(key)
                        .append("\" used as ")
                        .append(usageTypes)
                        .append("\n");
                appendLocations(report, key);
                continue;
            }

            CodeKeyType codeType = usageTypes.iterator().next();
            boolean matches = (codeType == CodeKeyType.STRING &&
                    ymlType == YmlValueType.STRING) ||
                    (codeType == CodeKeyType.LIST && ymlType == YmlValueType.LIST);

            if (!matches) {
                invalidKeys.add(key);
                report.append("\n\nTYPE MISMATCH: \"")
                        .append(key)
                        .append("\"\n")
                        .append("   Used in code as: ")
                        .append(codeType)
                        .append("\n")
                        .append("   Defined in message.yml as: ")
                        .append(ymlType)
                        .append("\n");
                appendLocations(report, key);
            }
        }

        if (!invalidKeys.isEmpty()) {
            fail(String.format(
                    "\n\n========================================\n" +
                            "MESSAGE KEY TYPE CONSISTENCY FAILED\n" +
                            "========================================\n" +
                            "Found %d key(s) with type mismatches between code and message.yml!\n" +
                            "Keys used with getString/sendMessage must map to a String in YAML.\n" +
                            "Keys used with getStringList/sendMessageList must map to a List in YAML.\n" +
                            "%s\n" +
                            "========================================\n" +
                            "ACTION REQUIRED: Fix key usage in code OR fix value type in message.yml\n" +
                            "========================================",
                    invalidKeys.size(), report));
        }
    }

    private static void appendLocations(StringBuilder report, String key) {
        List<KeyLocation> locations = codeKeys.get(key);
        if (locations == null || locations.isEmpty()) {
            return;
        }
        report.append("   Locations:\n");
        for (KeyLocation loc : locations) {
            report.append("   • ").append(loc).append("\n");
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

    private enum YmlValueType {
        STRING,
        LIST,
        OTHER
    }

    private enum CodeKeyType {
        STRING,
        LIST
    }
}
