package com.hallow.client.config;

import com.google.gson.Gson;

public final class HallowConfigManager {
    static final Gson GSON = HallowStorage.GSON;

    private static volatile HallowConfig config = new HallowConfig();

    private HallowConfigManager() {
    }

    public static synchronized void load() {
        HallowConfig loaded = HallowStorage.loadConfig();
        loaded.normalize();
        config = loaded;
        save();
    }

    public static HallowConfig get() {
        return config;
    }

    public static synchronized void applyAndSave(HallowConfig next) {
        next.normalize();
        config = next;
        save();
    }

    public static synchronized void save() {
        HallowStorage.saveConfig(config);
    }
}
