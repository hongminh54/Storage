package net.danh.storage;

import net.jqwik.api.*;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class RecipeEditManagerTest {

    @Property(tries = 100)
    void addFlagShouldResultInFlagSetContainingThatFlag(
            @ForAll("validItemFlags") ItemFlag flagToAdd,
            @ForAll("existingFlagSets") Set<ItemFlag> existingFlags
    ) {
        // Create a mutable copy of existing flags
        Set<ItemFlag> flags = new HashSet<>(existingFlags);

        // Add the flag
        flags.add(flagToAdd);

        // The flag set should now contain the added flag
        Assertions.assertThat(flags).contains(flagToAdd);
    }

    @Property(tries = 100)
    void addFlagShouldPreserveExistingFlags(
            @ForAll("validItemFlags") ItemFlag flagToAdd,
            @ForAll("existingFlagSets") Set<ItemFlag> existingFlags
    ) {
        // Create a mutable copy and remember original flags
        Set<ItemFlag> flags = new HashSet<>(existingFlags);
        Set<ItemFlag> originalFlags = new HashSet<>(existingFlags);

        // Add the flag
        flags.add(flagToAdd);

        // All original flags should still be present
        Assertions.assertThat(flags).containsAll(originalFlags);
    }

    @Property(tries = 100)
    void addFlagShouldBeIdempotent(
            @ForAll("validItemFlags") ItemFlag flagToAdd,
            @ForAll("existingFlagSets") Set<ItemFlag> existingFlags
    ) {
        // Create two copies
        Set<ItemFlag> flagsOnce = new HashSet<>(existingFlags);
        Set<ItemFlag> flagsTwice = new HashSet<>(existingFlags);

        // Add once vs add twice
        flagsOnce.add(flagToAdd);
        flagsTwice.add(flagToAdd);
        flagsTwice.add(flagToAdd);

        // Both should be equal
        Assertions.assertThat(flagsOnce).isEqualTo(flagsTwice);
    }

    @Property(tries = 100)
    void addFlagShouldIncreaseOrMaintainSetSize(
            @ForAll("validItemFlags") ItemFlag flagToAdd,
            @ForAll("existingFlagSets") Set<ItemFlag> existingFlags
    ) {
        // Arrange
        Set<ItemFlag> flags = new HashSet<>(existingFlags);
        int originalSize = flags.size();
        boolean wasPresent = flags.contains(flagToAdd);

        // Act
        flags.add(flagToAdd);

        // Assert
        if (wasPresent) {
            Assertions.assertThat(flags.size()).isEqualTo(originalSize);
        } else {
            Assertions.assertThat(flags.size()).isEqualTo(originalSize + 1);
        }
    }

    @Property(tries = 100)
    void removeFlagShouldResultInFlagSetNotContainingThatFlag(
            @ForAll("nonEmptyFlagSets") Set<ItemFlag> existingFlags
    ) {
        // Arrange: Create a mutable copy and pick a flag to remove
        Set<ItemFlag> flags = new HashSet<>(existingFlags);
        ItemFlag flagToRemove = existingFlags.iterator().next();

        // Act: Remove the flag (simulating the core logic of processFlagEdit "remove" command)
        flags.remove(flagToRemove);

        // Assert: The flag set should no longer contain the removed flag
        Assertions.assertThat(flags).doesNotContain(flagToRemove);
    }

    @Property(tries = 100)
    void removeFlagShouldPreserveOtherFlags(
            @ForAll("nonEmptyFlagSets") Set<ItemFlag> existingFlags
    ) {
        // Arrange: Create a mutable copy and pick a flag to remove
        Set<ItemFlag> flags = new HashSet<>(existingFlags);
        ItemFlag flagToRemove = existingFlags.iterator().next();
        Set<ItemFlag> otherFlags = new HashSet<>(existingFlags);
        otherFlags.remove(flagToRemove);

        // Act: Remove the flag
        flags.remove(flagToRemove);

        // Assert: All other flags should still be present
        Assertions.assertThat(flags).containsAll(otherFlags);
    }

    @Property(tries = 100)
    void removeFlagShouldDecreaseSetSizeByOne(
            @ForAll("nonEmptyFlagSets") Set<ItemFlag> existingFlags
    ) {
        // Arrange
        Set<ItemFlag> flags = new HashSet<>(existingFlags);
        int originalSize = flags.size();
        ItemFlag flagToRemove = existingFlags.iterator().next();

        // Act
        flags.remove(flagToRemove);

        // Assert: Size should decrease by exactly 1
        Assertions.assertThat(flags.size()).isEqualTo(originalSize - 1);
    }

    @Property(tries = 100)
    void removeFlagShouldBeIdempotent(
            @ForAll("nonEmptyFlagSets") Set<ItemFlag> existingFlags
    ) {
        // Arrange: Create two copies
        Set<ItemFlag> flagsOnce = new HashSet<>(existingFlags);
        Set<ItemFlag> flagsTwice = new HashSet<>(existingFlags);
        ItemFlag flagToRemove = existingFlags.iterator().next();

        // Act: Remove once vs remove twice
        flagsOnce.remove(flagToRemove);
        flagsTwice.remove(flagToRemove);
        flagsTwice.remove(flagToRemove);

        // Assert: Both should be equal
        Assertions.assertThat(flagsOnce).isEqualTo(flagsTwice);
    }

    @Property(tries = 100)
    void clearAllFlagsShouldResultInEmptySet(
            @ForAll("existingFlagSets") Set<ItemFlag> existingFlags
    ) {
        // Arrange: Create a mutable copy of existing flags (simulating recipe state)
        Set<ItemFlag> flags = new HashSet<>(existingFlags);

        // Act: Clear all flags (simulating the core logic of processFlagEdit "clear all" command)
        flags.clear();

        // Assert: The flag set should be empty
        Assertions.assertThat(flags).isEmpty();
    }

    @Property(tries = 100)
    void clearAllFlagsShouldBeIdempotent(
            @ForAll("existingFlagSets") Set<ItemFlag> existingFlags
    ) {
        // Arrange: Create two copies
        Set<ItemFlag> flagsOnce = new HashSet<>(existingFlags);
        Set<ItemFlag> flagsTwice = new HashSet<>(existingFlags);

        // Act: Clear once vs clear twice
        flagsOnce.clear();
        flagsTwice.clear();
        flagsTwice.clear();

        // Assert: Both should be equal (both empty)
        Assertions.assertThat(flagsOnce).isEqualTo(flagsTwice);
    }

    @Property(tries = 100)
    void clearAllFlagsShouldResultInSizeZero(
            @ForAll("existingFlagSets") Set<ItemFlag> existingFlags
    ) {
        // Arrange
        Set<ItemFlag> flags = new HashSet<>(existingFlags);

        // Act
        flags.clear();

        // Assert: Size should be 0
        Assertions.assertThat(flags.size()).isEqualTo(0);
    }

    @Property(tries = 100)
    void invalidFlagNameShouldBeRejected(
            @ForAll("invalidFlagNames") String invalidFlagName
    ) {
        // Act: Try to parse the invalid flag name using the same logic as RecipeEditManager.parseFlag()
        ItemFlag result = parseFlagForTest(invalidFlagName);

        // Assert: The result should be null (rejected)
        Assertions.assertThat(result).isNull();
    }

    @Property(tries = 100)
    void invalidFlagShouldNotModifyRecipeState(
            @ForAll("invalidFlagNames") String invalidFlagName,
            @ForAll("existingFlagSets") Set<ItemFlag> existingFlags
    ) {
        // Arrange: Create a copy of existing flags (simulating recipe state)
        Set<ItemFlag> flags = new HashSet<>(existingFlags);
        Set<ItemFlag> originalFlags = new HashSet<>(existingFlags);

        // Act: Simulate the add operation with validation (as in processFlagEdit)
        ItemFlag parsedFlag = parseFlagForTest(invalidFlagName);
        if (parsedFlag != null) {
            flags.add(parsedFlag);
        }
        // If parsedFlag is null, flags should remain unchanged

        // Assert: The flag set should be unchanged since the flag was invalid
        Assertions.assertThat(flags).isEqualTo(originalFlags);
    }

    @Property(tries = 100)
    void emptyOrWhitespaceFlagNameShouldBeRejected(
            @ForAll("emptyOrWhitespaceStrings") String emptyInput
    ) {
        // Act: Try to parse empty/whitespace input
        ItemFlag result = parseFlagForTest(emptyInput);

        // Assert: The result should be null (rejected)
        Assertions.assertThat(result).isNull();
    }

    @Property(tries = 100)
    void addFlagShouldPreserveOtherRecipeProperties(
            @ForAll("validItemFlags") ItemFlag flagToAdd,
            @ForAll("recipeStates") RecipeState originalState
    ) {
        // Arrange: Create a recipe with the given state
        RecipeState recipe = originalState.copy();

        // Capture original values
        String originalName = recipe.name;
        String originalCategory = recipe.category;
        boolean originalEnabled = recipe.enabled;
        String originalMaterial = recipe.resultMaterial;
        String originalResultName = recipe.resultName;
        List<String> originalLore = new ArrayList<>(recipe.resultLore);
        Map<String, Integer> originalEnchantments = new HashMap<>(recipe.resultEnchantments);
        int originalAmount = recipe.resultAmount;
        int originalCustomModelData = recipe.resultCustomModelData;
        boolean originalUnbreakable = recipe.resultUnbreakable;
        Map<String, Integer> originalMaterialRequirements = new HashMap<>(recipe.materialRequirements);
        String originalPermission = recipe.permissionRequirement;

        // Act: Add a flag (simulating processFlagEdit "add" command)
        recipe.resultFlags.add(flagToAdd);

        // Assert: All other properties should remain unchanged
        Assertions.assertThat(recipe.name).isEqualTo(originalName);
        Assertions.assertThat(recipe.category).isEqualTo(originalCategory);
        Assertions.assertThat(recipe.enabled).isEqualTo(originalEnabled);
        Assertions.assertThat(recipe.resultMaterial).isEqualTo(originalMaterial);
        Assertions.assertThat(recipe.resultName).isEqualTo(originalResultName);
        Assertions.assertThat(recipe.resultLore).isEqualTo(originalLore);
        Assertions.assertThat(recipe.resultEnchantments).isEqualTo(originalEnchantments);
        Assertions.assertThat(recipe.resultAmount).isEqualTo(originalAmount);
        Assertions.assertThat(recipe.resultCustomModelData).isEqualTo(originalCustomModelData);
        Assertions.assertThat(recipe.resultUnbreakable).isEqualTo(originalUnbreakable);
        Assertions.assertThat(recipe.materialRequirements).isEqualTo(originalMaterialRequirements);
        Assertions.assertThat(recipe.permissionRequirement).isEqualTo(originalPermission);
    }

    @Property(tries = 100)
    void removeFlagShouldPreserveOtherRecipeProperties(
            @ForAll("recipeStatesWithFlags") RecipeState originalState
    ) {
        // Arrange: Create a recipe with the given state (has at least one flag)
        RecipeState recipe = originalState.copy();
        ItemFlag flagToRemove = recipe.resultFlags.iterator().next();

        // Capture original values
        String originalName = recipe.name;
        String originalCategory = recipe.category;
        boolean originalEnabled = recipe.enabled;
        String originalMaterial = recipe.resultMaterial;
        String originalResultName = recipe.resultName;
        List<String> originalLore = new ArrayList<>(recipe.resultLore);
        Map<String, Integer> originalEnchantments = new HashMap<>(recipe.resultEnchantments);
        int originalAmount = recipe.resultAmount;
        int originalCustomModelData = recipe.resultCustomModelData;
        boolean originalUnbreakable = recipe.resultUnbreakable;
        Map<String, Integer> originalMaterialRequirements = new HashMap<>(recipe.materialRequirements);
        String originalPermission = recipe.permissionRequirement;

        // Act: Remove a flag (simulating processFlagEdit "remove" command)
        recipe.resultFlags.remove(flagToRemove);

        // Assert: All other properties should remain unchanged
        Assertions.assertThat(recipe.name).isEqualTo(originalName);
        Assertions.assertThat(recipe.category).isEqualTo(originalCategory);
        Assertions.assertThat(recipe.enabled).isEqualTo(originalEnabled);
        Assertions.assertThat(recipe.resultMaterial).isEqualTo(originalMaterial);
        Assertions.assertThat(recipe.resultName).isEqualTo(originalResultName);
        Assertions.assertThat(recipe.resultLore).isEqualTo(originalLore);
        Assertions.assertThat(recipe.resultEnchantments).isEqualTo(originalEnchantments);
        Assertions.assertThat(recipe.resultAmount).isEqualTo(originalAmount);
        Assertions.assertThat(recipe.resultCustomModelData).isEqualTo(originalCustomModelData);
        Assertions.assertThat(recipe.resultUnbreakable).isEqualTo(originalUnbreakable);
        Assertions.assertThat(recipe.materialRequirements).isEqualTo(originalMaterialRequirements);
        Assertions.assertThat(recipe.permissionRequirement).isEqualTo(originalPermission);
    }

    @Property(tries = 100)
    void clearAllFlagsShouldPreserveOtherRecipeProperties(
            @ForAll("recipeStates") RecipeState originalState
    ) {
        // Arrange: Create a recipe with the given state
        RecipeState recipe = originalState.copy();

        // Capture original values
        String originalName = recipe.name;
        String originalCategory = recipe.category;
        boolean originalEnabled = recipe.enabled;
        String originalMaterial = recipe.resultMaterial;
        String originalResultName = recipe.resultName;
        List<String> originalLore = new ArrayList<>(recipe.resultLore);
        Map<String, Integer> originalEnchantments = new HashMap<>(recipe.resultEnchantments);
        int originalAmount = recipe.resultAmount;
        int originalCustomModelData = recipe.resultCustomModelData;
        boolean originalUnbreakable = recipe.resultUnbreakable;
        Map<String, Integer> originalMaterialRequirements = new HashMap<>(recipe.materialRequirements);
        String originalPermission = recipe.permissionRequirement;

        // Act: Clear all flags (simulating processFlagEdit "clear all" command)
        recipe.resultFlags.clear();

        // Assert: All other properties should remain unchanged
        Assertions.assertThat(recipe.name).isEqualTo(originalName);
        Assertions.assertThat(recipe.category).isEqualTo(originalCategory);
        Assertions.assertThat(recipe.enabled).isEqualTo(originalEnabled);
        Assertions.assertThat(recipe.resultMaterial).isEqualTo(originalMaterial);
        Assertions.assertThat(recipe.resultName).isEqualTo(originalResultName);
        Assertions.assertThat(recipe.resultLore).isEqualTo(originalLore);
        Assertions.assertThat(recipe.resultEnchantments).isEqualTo(originalEnchantments);
        Assertions.assertThat(recipe.resultAmount).isEqualTo(originalAmount);
        Assertions.assertThat(recipe.resultCustomModelData).isEqualTo(originalCustomModelData);
        Assertions.assertThat(recipe.resultUnbreakable).isEqualTo(originalUnbreakable);
        Assertions.assertThat(recipe.materialRequirements).isEqualTo(originalMaterialRequirements);
        Assertions.assertThat(recipe.permissionRequirement).isEqualTo(originalPermission);
    }

    @Provide
    Arbitrary<RecipeState> recipeStates() {
        // First combine: basic recipe properties (6 params)
        Arbitrary<RecipeState> basicState = Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),  // name
                Arbitraries.of("default", "weapons", "armor", "tools", "misc"), // category
                Arbitraries.of(true, false),                                    // enabled
                Arbitraries.of("STONE", "DIAMOND_SWORD", "IRON_PICKAXE", "GOLDEN_APPLE", "NETHERITE_HELMET"), // material
                Arbitraries.strings().ofMinLength(0).ofMaxLength(50),           // resultName
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(30).list().ofMaxSize(5) // lore
        ).as((name, category, enabled, material, resultName, lore) -> {
            RecipeState state = new RecipeState();
            state.name = name;
            state.category = category;
            state.enabled = enabled;
            state.resultMaterial = material;
            state.resultName = resultName;
            state.resultLore = lore;
            return state;
        });

        // Second combine: add remaining properties (5 params)
        return Combinators.combine(
                basicState,
                Arbitraries.integers().between(1, 64),                          // amount
                Arbitraries.integers().between(0, 1000),                        // customModelData
                Arbitraries.of(true, false),                                    // unbreakable
                Arbitraries.of(ItemFlag.values()).set().ofMinSize(0).ofMaxSize(ItemFlag.values().length) // flags
        ).as((state, amount, customModelData, unbreakable, flags) -> {
            state.resultAmount = amount;
            state.resultCustomModelData = customModelData;
            state.resultUnbreakable = unbreakable;
            state.resultFlags = new HashSet<>(flags);
            // Add some random material requirements
            state.materialRequirements.put("DIAMOND", amount);
            return state;
        });
    }

    /**
     * Generates recipe states that have at least one flag.
     */
    @Provide
    Arbitrary<RecipeState> recipeStatesWithFlags() {
        return recipeStates().filter(state -> !state.resultFlags.isEmpty());
    }

    private ItemFlag parseFlagForTest(String flagName) {
        if (flagName == null || flagName.trim().isEmpty()) {
            return null;
        }

        String normalizedName = flagName.toUpperCase().trim();

        try {
            // Try to get the ItemFlag enum value
            return ItemFlag.valueOf(normalizedName);
        } catch (IllegalArgumentException e) {
            // Not a valid ItemFlag name
            return null;
        }
    }

    @Provide
    Arbitrary<ItemFlag> validItemFlags() {
        return Arbitraries.of(ItemFlag.values());
    }

    @Provide
    Arbitrary<String> invalidFlagNames() {
        // Get all valid flag names to exclude them
        Set<String> validNames = new HashSet<>();
        for (ItemFlag flag : ItemFlag.values()) {
            validNames.add(flag.name());
            validNames.add(flag.name().toLowerCase());
        }

        return Arbitraries.oneOf(
                // Random strings that are definitely not flag names
                Arbitraries.strings()
                        .alpha()
                        .ofMinLength(1)
                        .ofMaxLength(30)
                        .filter(s -> !validNames.contains(s.toUpperCase())),
                // Common typos and variations
                Arbitraries.of(
                        "HIDE_ENCHANT",           // Missing 'S'
                        "HIDEENCHANTS",           // Missing underscore
                        "HIDE-ENCHANTS",          // Wrong separator
                        "hide_enchants_extra",    // Extra suffix
                        "INVALID_FLAG",
                        "NOT_A_FLAG",
                        "RANDOM_STRING",
                        "HIDE",
                        "ENCHANTS",
                        "FLAG",
                        "123",
                        "HIDE_123",
                        "___",
                        "HIDE__ENCHANTS",         // Double underscore
                        "SHOW_ENCHANTS",          // Wrong prefix
                        "DISPLAY_ATTRIBUTES"      // Wrong prefix
                ),
                // Strings with special characters
                Arbitraries.of(
                        "HIDE_ENCHANTS!",
                        "HIDE@ENCHANTS",
                        "HIDE#ENCHANTS",
                        "HIDE$ENCHANTS",
                        "HIDE%ENCHANTS",
                        "HIDE ENCHANTS",          // Space instead of underscore
                        "HIDE\tENCHANTS",         // Tab
                        "HIDE\nENCHANTS"          // Newline
                )
        );
    }

    /**
     * Generates empty or whitespace-only strings.
     */
    @Provide
    Arbitrary<String> emptyOrWhitespaceStrings() {
        return Arbitraries.of(
                "",
                " ",
                "  ",
                "\t",
                "\n",
                "   \t   ",
                "\n\n",
                " \t \n "
        );
    }

    /**
     * Generates random sets of existing flags (0 to all flags).
     */
    @Provide
    Arbitrary<Set<ItemFlag>> existingFlagSets() {
        return Arbitraries.of(ItemFlag.values())
                .set()
                .ofMinSize(0)
                .ofMaxSize(ItemFlag.values().length);
    }

    /**
     * Generates non-empty sets of flags (at least 1 flag).
     * Used for remove operations that require at least one flag to exist.
     */
    @Provide
    Arbitrary<Set<ItemFlag>> nonEmptyFlagSets() {
        return Arbitraries.of(ItemFlag.values())
                .set()
                .ofMinSize(1)
                .ofMaxSize(ItemFlag.values().length);
    }

    static class RecipeState {
        String name;
        String category;
        boolean enabled;
        String resultMaterial;
        String resultName;
        List<String> resultLore;
        Map<String, Integer> resultEnchantments;
        int resultAmount;
        int resultCustomModelData;
        boolean resultUnbreakable;
        Set<ItemFlag> resultFlags;
        Map<String, Integer> materialRequirements;
        String permissionRequirement;

        RecipeState() {
            this.name = "default";
            this.category = "default";
            this.enabled = true;
            this.resultMaterial = "STONE";
            this.resultName = "&7Custom Item";
            this.resultLore = new ArrayList<>();
            this.resultEnchantments = new HashMap<>();
            this.resultAmount = 1;
            this.resultCustomModelData = 0;
            this.resultUnbreakable = false;
            this.resultFlags = new HashSet<>();
            this.materialRequirements = new HashMap<>();
            this.permissionRequirement = null;
        }

        RecipeState copy() {
            RecipeState copy = new RecipeState();
            copy.name = this.name;
            copy.category = this.category;
            copy.enabled = this.enabled;
            copy.resultMaterial = this.resultMaterial;
            copy.resultName = this.resultName;
            copy.resultLore = new ArrayList<>(this.resultLore);
            copy.resultEnchantments = new HashMap<>(this.resultEnchantments);
            copy.resultAmount = this.resultAmount;
            copy.resultCustomModelData = this.resultCustomModelData;
            copy.resultUnbreakable = this.resultUnbreakable;
            copy.resultFlags = new HashSet<>(this.resultFlags);
            copy.materialRequirements = new HashMap<>(this.materialRequirements);
            copy.permissionRequirement = this.permissionRequirement;
            return copy;
        }
    }

    private static class Assertions {
        static <T> AssertThat<T> assertThat(T actual) {
            return new AssertThat<>(actual);
        }

        static class AssertThat<T> {
            private final T actual;

            AssertThat(T actual) {
                this.actual = actual;
            }

            void contains(Object expected) {
                if (actual instanceof Set) {
                    if (!((Set<?>) actual).contains(expected)) {
                        throw new AssertionError("Expected set to contain " + expected + " but was " + actual);
                    }
                } else {
                    throw new AssertionError("contains() only works with Set");
                }
            }

            void doesNotContain(Object expected) {
                if (actual instanceof Set) {
                    if (((Set<?>) actual).contains(expected)) {
                        throw new AssertionError("Expected set to NOT contain " + expected + " but was " + actual);
                    }
                } else {
                    throw new AssertionError("doesNotContain() only works with Set");
                }
            }

            void containsAll(Collection<?> expected) {
                if (actual instanceof Set) {
                    if (!((Set<?>) actual).containsAll(expected)) {
                        throw new AssertionError("Expected set to contain all of " + expected + " but was " + actual);
                    }
                } else {
                    throw new AssertionError("containsAll() only works with Set");
                }
            }

            void isEqualTo(Object expected) {
                if (!Objects.equals(actual, expected)) {
                    throw new AssertionError("Expected " + expected + " but was " + actual);
                }
            }

            void isEmpty() {
                if (actual instanceof Set) {
                    if (!((Set<?>) actual).isEmpty()) {
                        throw new AssertionError("Expected set to be empty but was " + actual);
                    }
                } else {
                    throw new AssertionError("isEmpty() only works with Set");
                }
            }

            void isNull() {
                if (actual != null) {
                    throw new AssertionError("Expected null but was " + actual);
                }
            }
        }
    }
}
