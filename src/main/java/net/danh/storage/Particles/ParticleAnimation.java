package net.danh.storage.Particles;

public enum ParticleAnimation {
    NONE("none"),
    CIRCLE("circle"),
    SPIRAL("spiral"),
    LINE("line"),
    BURST("burst"),
    HELIX("helix"),
    WAVE("wave"),
    // Advanced Geometric Patterns
    DNA_HELIX("dna_helix"),
    GALAXY("galaxy"),
    TORNADO("tornado"),
    LIGHTNING("lightning"),
    GEOMETRIC_STAR("geometric_star");

    private final String configName;

    ParticleAnimation(String configName) {
        this.configName = configName;
    }

    public static ParticleAnimation fromString(String name) {
        if (name == null) return NONE;

        for (ParticleAnimation animation : values()) {
            if (animation.configName.equalsIgnoreCase(name)) {
                return animation;
            }
        }
        return NONE;
    }
}
