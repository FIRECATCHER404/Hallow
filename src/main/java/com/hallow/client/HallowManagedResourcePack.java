package com.hallow.client;

import java.io.IOException;
import java.io.InputStream;
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
    private static final String BUILTIN_XRAY_PACK_RESOURCE = "/assets/hallow/builtin/" + XRAY_PACK_FILE_NAME;

    private HallowManagedResourcePack() {
    }

    public static PackToggleResult enableXRayPack(Minecraft client, String sourcePath) {
        if (client == null) {
            return PackToggleResult.failure("Client is unavailable.");
        }

        ManagedPackInstall installedPack = installManagedPack(client, sourcePath, XRAY_PACK_FILE_NAME);
        if (installedPack == null) {
            return PackToggleResult.failure("Managed X-Ray resource pack is not available.");
        }

        PackToggleResult toggleResult = togglePack(client, XRAY_PACK_FILE_NAME, true);
        if (!toggleResult.succeeded()) {
            return toggleResult;
        }

        return PackToggleResult.success(
            toggleResult.packId(),
            toggleResult.changed(),
            toggleResult.reloadFuture(),
            installedPack.sourceLabel()
        );
    }

    public static PackToggleResult disableXRayPack(Minecraft client) {
        if (client == null) {
            return PackToggleResult.success(null, false, CompletableFuture.completedFuture(null));
        }

        return togglePack(client, XRAY_PACK_FILE_NAME, false);
    }

    private static ManagedPackInstall installManagedPack(Minecraft client, String sourcePath, String managedFileName) {
        Path target = client.getResourcePackDirectory().resolve(managedFileName);
        try {
            Files.createDirectories(target.getParent());
        } catch (IOException ignored) {
            return Files.exists(target) ? new ManagedPackInstall(target, "cached") : null;
        }

        Path source = resolveSourcePath(sourcePath);
        if (source != null) {
            try {
                if (shouldCopy(source, target)) {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return new ManagedPackInstall(target, source.getFileName().toString());
            } catch (IOException ignored) {
                return Files.exists(target) ? new ManagedPackInstall(target, "cached") : null;
            }
        }

        try (InputStream stream = HallowManagedResourcePack.class.getResourceAsStream(BUILTIN_XRAY_PACK_RESOURCE)) {
            if (stream == null) {
                return Files.exists(target) ? new ManagedPackInstall(target, "cached") : null;
            }

            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
            return new ManagedPackInstall(target, "built-in");
        } catch (IOException ignored) {
            return Files.exists(target) ? new ManagedPackInstall(target, "cached") : null;
        }
    }

    private static Path resolveSourcePath(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return null;
        }

        try {
            Path source = Path.of(sourcePath.trim());
            return Files.exists(source) ? source : null;
        } catch (InvalidPathException ignored) {
            return null;
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

    private record ManagedPackInstall(Path target, String sourceLabel) {
    }

    public record PackToggleResult(String packId, boolean changed, CompletableFuture<?> reloadFuture, String error, String sourceLabel) {
        public static PackToggleResult success(String packId, boolean changed, CompletableFuture<?> reloadFuture) {
            return new PackToggleResult(packId, changed, reloadFuture, null, null);
        }

        public static PackToggleResult success(String packId, boolean changed, CompletableFuture<?> reloadFuture, String sourceLabel) {
            return new PackToggleResult(packId, changed, reloadFuture, null, sourceLabel);
        }

        public static PackToggleResult failure(String error) {
            return new PackToggleResult(null, false, CompletableFuture.completedFuture(null), error, null);
        }

        public boolean succeeded() {
            return error == null;
        }
    }
}
