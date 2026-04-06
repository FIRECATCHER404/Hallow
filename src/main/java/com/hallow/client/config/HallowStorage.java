package com.hallow.client.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

public final class HallowStorage {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Path HALLOW_DIR = FabricLoader.getInstance().getGameDir().resolve(".hallow");
    private static final Path CONFIG_PATH = HALLOW_DIR.resolve("config.json");
    private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("hallow.json");
    private static final Path PROFILES_DIR = HALLOW_DIR.resolve("profiles");

    private static volatile ProfileIdentity activeProfile;
    private static volatile boolean dirty;

    private HallowStorage() {
    }

    public static Path configPath() {
        return CONFIG_PATH;
    }

    public static Path rootDirectory() {
        return HALLOW_DIR;
    }

    public static synchronized HallowConfig loadConfig() {
        HallowConfig loaded = readJson(CONFIG_PATH, HallowConfig.class);
        if (loaded == null) {
            loaded = readJson(LEGACY_CONFIG_PATH, HallowConfig.class);
        }
        if (loaded == null) {
            loaded = new HallowConfig();
        }
        loaded.normalize();
        return loaded;
    }

    public static synchronized void saveConfig(HallowConfig config) {
        config.normalize();
        writeJson(CONFIG_PATH, config);
    }

    public static synchronized void activateProfile(ProfileIdentity identity) {
        activeProfile = identity;
        dirty = false;
    }

    public static ProfileIdentity activeProfile() {
        return activeProfile;
    }

    public static synchronized void clearActiveProfile() {
        activeProfile = null;
        dirty = false;
    }

    public static synchronized HallowProfileState loadActiveProfile() {
        if (activeProfile == null) {
            HallowProfileState empty = new HallowProfileState();
            empty.normalize();
            return empty;
        }

        HallowProfileState loaded = readJson(profilePath(activeProfile.key()), HallowProfileState.class);
        if (loaded == null) {
            loaded = new HallowProfileState();
            loaded.loadedFromDisk = false;
        } else {
            loaded.loadedFromDisk = true;
        }

        loaded.profileKey = activeProfile.key();
        loaded.profileLabel = activeProfile.label();
        loaded.source = activeProfile.source();
        loaded.normalize();
        return loaded;
    }

    public static synchronized void saveActiveProfile(HallowProfileState state) {
        if (activeProfile == null || state == null) {
            return;
        }

        state.profileKey = activeProfile.key();
        state.profileLabel = activeProfile.label();
        state.source = activeProfile.source();
        state.updatedAtEpochMillis = System.currentTimeMillis();
        state.normalize();
        writeJson(profilePath(activeProfile.key()), state);
        dirty = false;
    }

    public static synchronized void markDirty() {
        dirty = true;
    }

    public static boolean isDirty() {
        return dirty;
    }

    public static ProfileIdentity resolveProfileIdentity(Minecraft client) {
        if (client != null && client.hasSingleplayerServer()) {
            IntegratedServer server = client.getSingleplayerServer();
            if (server != null) {
                String levelName = server.getWorldData().getLevelName();
                String worldName = (levelName == null || levelName.isBlank()) ? "singleplayer" : levelName;
                return new ProfileIdentity("singleplayer:" + worldName, worldName, "singleplayer");
            }
        }

        if (client != null) {
            ServerData server = client.getCurrentServer();
            if (server != null) {
                String address = (server.ip == null || server.ip.isBlank()) ? server.name : server.ip;
                String label = server.name == null || server.name.isBlank() ? address : server.name + " [" + address + "]";
                return new ProfileIdentity("server:" + address.toLowerCase(Locale.ROOT), label, "multiplayer");
            }
        }

        return new ProfileIdentity("unknown", "Unknown", "unknown");
    }

    private static <T> T readJson(Path path, Class<T> type) {
        if (!Files.exists(path)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, type);
        } catch (IOException | JsonParseException ignored) {
            return null;
        }
    }

    private static void writeJson(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(value, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static Path profilePath(String rawKey) {
        String normalized = rawKey == null ? "unknown" : rawKey.toLowerCase(Locale.ROOT);
        String slug = normalized.replaceAll("[^a-z0-9._-]+", "_");
        if (slug.isBlank()) {
            slug = "unknown";
        }
        if (slug.length() > 48) {
            slug = slug.substring(0, 48);
        }
        String fileName = slug + "_" + Integer.toHexString(normalized.hashCode()) + ".json";
        return PROFILES_DIR.resolve(fileName);
    }

    public record ProfileIdentity(String key, String label, String source) {
    }
}
