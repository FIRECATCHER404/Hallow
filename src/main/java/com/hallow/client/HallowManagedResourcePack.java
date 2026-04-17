package com.hallow.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

public final class HallowManagedResourcePack {
    private static final String XRAY_PACK_FILE_NAME = "hallow_xray_pack.zip";

    private HallowManagedResourcePack() {
    }

    public static PackToggleResult enableXRayPack(Minecraft client, String sourcePath) {
        if (client == null) {
            return PackToggleResult.failure("Client is unavailable.");
        }

        Path installedPack = installManagedPack(client, sourcePath, XRAY_PACK_FILE_NAME);
        if (installedPack == null) {
            if (sourcePath == null || sourcePath.isBlank()) {
                return PackToggleResult.failure("X-Ray resource pack source is not configured.");
            }

            return PackToggleResult.failure("X-Ray resource pack source was not found.");
        }

        return togglePack(client, XRAY_PACK_FILE_NAME, true);
    }

    public static PackToggleResult disableXRayPack(Minecraft client) {
        if (client == null) {
            return PackToggleResult.success(null, false, CompletableFuture.completedFuture(null));
        }

        return togglePack(client, XRAY_PACK_FILE_NAME, false);
    }

    private static Path installManagedPack(Minecraft client, String sourcePath, String managedFileName) {
        Path target = client.getResourcePackDirectory().resolve(managedFileName);
        if (sourcePath == null || sourcePath.isBlank()) {
            return Files.exists(target) ? target : null;
        }

        Path source;
        try {
            source = Path.of(sourcePath.trim());
        } catch (InvalidPathException ignored) {
            return Files.exists(target) ? target : null;
        }

        if (!Files.exists(source)) {
            return Files.exists(target) ? target : null;
        }

        try {
            Files.createDirectories(target.getParent());
            if (shouldCopy(source, target)) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (IOException ignored) {
            return Files.exists(target) ? target : null;
        }
    }

    private static boolean shouldCopy(Path source, Path target) throws IOException {
        if (!Files.exists(target)) {
            return true;
        }

        if (Files.size(source) != Files.size(target)) {
            return true;
        }

        return Files.getLastModifiedTime(source).compareTo(Files.getLastModifiedTime(target)) > 0;
    }

    private static PackToggleResult togglePack(Minecraft client, String managedFileName, boolean enabled) {
        PackRepository repository = client.getResourcePackRepository();
        repository.reload();

        String packId = resolvePackId(repository, managedFileName);
        if (packId == null) {
            boolean optionsChanged = !enabled && removeManagedIds(client, managedFileName);
            if (optionsChanged) {
                client.options.save();
            }

            if (enabled) {
                return PackToggleResult.failure("Managed resource pack is not available.");
            }

            return PackToggleResult.success(null, optionsChanged, CompletableFuture.completedFuture(null));
        }

        List<String> selectedIds = new ArrayList<>(repository.getSelectedIds());
        boolean changed;
        if (enabled) {
            changed = !selectedIds.contains(packId);
            if (changed) {
                selectedIds.add(packId);
            }
        } else {
            changed = selectedIds.removeIf(id -> id.equals(packId));
        }

        repository.setSelected(selectedIds);
        syncOptions(client, selectedIds, packId, enabled);

        CompletableFuture<?> reloadFuture = changed
            ? client.reloadResourcePacks()
            : CompletableFuture.completedFuture(null);
        return PackToggleResult.success(packId, changed, reloadFuture);
    }

    private static void syncOptions(Minecraft client, List<String> selectedIds, String packId, boolean enabled) {
        client.options.resourcePacks.clear();
        client.options.resourcePacks.addAll(selectedIds);
        if (!enabled) {
            client.options.incompatibleResourcePacks.removeIf(id -> id.equals(packId));
        }
        client.options.save();
    }

    private static boolean removeManagedIds(Minecraft client, String managedFileName) {
        boolean removed = client.options.resourcePacks.removeIf(id -> matchesManagedId(id, managedFileName));
        removed |= client.options.incompatibleResourcePacks.removeIf(id -> matchesManagedId(id, managedFileName));
        return removed;
    }

    private static String resolvePackId(PackRepository repository, String managedFileName) {
        for (String id : repository.getAvailableIds()) {
            if (matchesManagedId(id, managedFileName)) {
                return id;
            }
        }
        return null;
    }

    private static boolean matchesManagedId(String id, String managedFileName) {
        return id != null
            && (id.equalsIgnoreCase(managedFileName)
            || id.endsWith("/" + managedFileName)
            || id.endsWith("\\" + managedFileName));
    }

    public record PackToggleResult(String packId, boolean changed, CompletableFuture<?> reloadFuture, String error) {
        public static PackToggleResult success(String packId, boolean changed, CompletableFuture<?> reloadFuture) {
            return new PackToggleResult(packId, changed, reloadFuture, null);
        }

        public static PackToggleResult failure(String error) {
            return new PackToggleResult(null, false, CompletableFuture.completedFuture(null), error);
        }

        public boolean succeeded() {
            return error == null;
        }
    }
}
