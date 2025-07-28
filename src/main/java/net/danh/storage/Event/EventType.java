package net.danh.storage.Event;

public enum EventType {
    MINING_CONTEST("mining_contest", "Mining Contest"),
    DOUBLE_DROP("double_drop", "Double Drop"),
    COMMUNITY_EVENT("community_event", "Community Event");

    private final String configKey;
    private final String displayName;

    EventType(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    public static EventType fromConfigKey(String configKey) {
        for (EventType type : values()) {
            if (type.getConfigKey().equals(configKey)) {
                return type;
            }
        }
        return null;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDisplayName() {
        return displayName;
    }
}
