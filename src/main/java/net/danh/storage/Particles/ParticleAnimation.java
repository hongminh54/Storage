package net.danh.storage.Particles;

public enum ParticleAnimation {
    NONE("none"),
    CIRCLE("circle"),
    SPIRAL("spiral"),
    LINE("line"),
    BURST("burst"),
    HELIX("helix"),
    WAVE("wave");

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

    public String getConfigName() {
        return configName;
    }
}
