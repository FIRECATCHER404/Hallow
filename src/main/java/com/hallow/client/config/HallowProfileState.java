package com.hallow.client.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class HallowProfileState {
    public transient boolean loadedFromDisk;
    public String profileKey = "unknown";
    public String profileLabel = "Unknown";
    public String source = "unknown";
    public long updatedAtEpochMillis = System.currentTimeMillis();
    public LinkedHashMap<String, Boolean> enabledModules = new LinkedHashMap<>();
    public boolean minimapVisible;
    public List<SavedCamera> savedCameras = new ArrayList<>();
    public SavedAnchor anchor = new SavedAnchor();

    public void normalize() {
        if (profileKey == null || profileKey.isBlank()) {
            profileKey = "unknown";
        }
        if (profileLabel == null || profileLabel.isBlank()) {
            profileLabel = "Unknown";
        }
        if (source == null || source.isBlank()) {
            source = "unknown";
        }
        if (enabledModules == null) {
            enabledModules = new LinkedHashMap<>();
        }
        if (savedCameras == null) {
            savedCameras = new ArrayList<>();
        }
        if (anchor == null) {
            anchor = new SavedAnchor();
        }
        for (SavedCamera camera : savedCameras) {
            if (camera != null) {
                camera.normalize();
            }
        }
        anchor.normalize();
    }

    public static final class SavedCamera {
        public String dimension = "minecraft:overworld";
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;

        private void normalize() {
            if (dimension == null || dimension.isBlank()) {
                dimension = "minecraft:overworld";
            }
        }
    }

    public static final class SavedAnchor {
        public boolean present;
        public String dimension = "minecraft:overworld";
        public double x;
        public double y;
        public double z;

        private void normalize() {
            if (dimension == null || dimension.isBlank()) {
                dimension = "minecraft:overworld";
            }
        }
    }
}
