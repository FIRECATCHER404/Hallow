package com.hallow.client.config;

public enum XRayMode {
    ESP("ESP"),
    SPECTATOR_VIEW("Spectator View"),
    ORE_MODE("Ore Mode");

    private final String label;

    XRayMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
