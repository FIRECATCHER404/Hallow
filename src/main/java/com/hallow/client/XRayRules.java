package com.hallow.client;

import java.util.List;
import java.util.Map;

import com.hallow.client.config.HallowConfig;
import com.hallow.client.config.HallowConfigManager;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public final class XRayRules {
    private static final Map<String, OreStyle> DEFAULT_STYLES = Map.ofEntries(
        Map.entry("minecraft:coal_ore", new OreStyle("Coal Ore", 0.18F, 0.18F, 0.18F)),
        Map.entry("minecraft:deepslate_coal_ore", new OreStyle("Deepslate Coal Ore", 0.14F, 0.14F, 0.14F)),
        Map.entry("minecraft:copper_ore", new OreStyle("Copper Ore", 0.78F, 0.46F, 0.24F)),
        Map.entry("minecraft:deepslate_copper_ore", new OreStyle("Deepslate Copper Ore", 0.62F, 0.36F, 0.18F)),
        Map.entry("minecraft:diamond_ore", new OreStyle("Diamond Ore", 0.20F, 0.95F, 0.95F)),
        Map.entry("minecraft:deepslate_diamond_ore", new OreStyle("Deepslate Diamond Ore", 0.16F, 0.82F, 0.82F)),
        Map.entry("minecraft:emerald_ore", new OreStyle("Emerald Ore", 0.15F, 0.92F, 0.25F)),
        Map.entry("minecraft:deepslate_emerald_ore", new OreStyle("Deepslate Emerald Ore", 0.10F, 0.75F, 0.18F)),
        Map.entry("minecraft:redstone_ore", new OreStyle("Redstone Ore", 0.92F, 0.18F, 0.18F)),
        Map.entry("minecraft:deepslate_redstone_ore", new OreStyle("Deepslate Redstone Ore", 0.78F, 0.12F, 0.12F)),
        Map.entry("minecraft:lapis_ore", new OreStyle("Lapis Ore", 0.18F, 0.35F, 0.94F)),
        Map.entry("minecraft:deepslate_lapis_ore", new OreStyle("Deepslate Lapis Ore", 0.12F, 0.24F, 0.76F)),
        Map.entry("minecraft:gold_ore", new OreStyle("Gold Ore", 0.94F, 0.72F, 0.16F)),
        Map.entry("minecraft:deepslate_gold_ore", new OreStyle("Deepslate Gold Ore", 0.85F, 0.62F, 0.14F)),
        Map.entry("minecraft:iron_ore", new OreStyle("Iron Ore", 0.85F, 0.60F, 0.48F)),
        Map.entry("minecraft:deepslate_iron_ore", new OreStyle("Deepslate Iron Ore", 0.70F, 0.47F, 0.38F)),
        Map.entry("minecraft:nether_gold_ore", new OreStyle("Nether Gold Ore", 0.94F, 0.76F, 0.24F)),
        Map.entry("minecraft:nether_quartz_ore", new OreStyle("Nether Quartz Ore", 0.95F, 0.95F, 0.95F)),
        Map.entry("minecraft:ancient_debris", new OreStyle("Ancient Debris", 0.64F, 0.36F, 0.26F))
    );

    private static volatile Map<Block, OreStyle> trackedBlocks = Map.of();
    private static volatile List<String> trackedBlockIds = HallowConfig.defaultXRayBlockIds();

    private XRayRules() {
    }

    public static void reloadFromConfig() {
        List<String> configuredIds = HallowConfigManager.get().xray.trackedBlocks;
        trackedBlockIds = List.copyOf(HallowConfig.normalizeBlockIds(configuredIds));

        java.util.HashMap<Block, OreStyle> resolved = new java.util.HashMap<>();
        for (String id : trackedBlockIds) {
            Identifier identifier = Identifier.tryParse(id);
            if (identifier == null) {
                continue;
            }

            Block block = BuiltInRegistries.BLOCK.getOptional(identifier).orElse(null);
            if (block == null || block == Blocks.AIR) {
                continue;
            }

            resolved.put(block, styleForBlock(block, identifier));
        }

        trackedBlocks = Map.copyOf(resolved);
    }

    public static List<String> trackedBlockIds() {
        return trackedBlockIds;
    }

    public static int trackedBlockCount() {
        return trackedBlocks.size();
    }

    public static boolean isTrackedBlock(BlockState state) {
        return trackedBlocks.containsKey(state.getBlock());
    }

    public static OreStyle styleFor(BlockState state) {
        return trackedBlocks.get(state.getBlock());
    }

    public static boolean shouldKeepVisibleForOreMode(BlockState state) {
        if (state.isAir()) {
            return true;
        }

        if (isTrackedBlock(state)) {
            return true;
        }

        FluidState fluidState = state.getFluidState();
        return !fluidState.isEmpty() && (fluidState.is(Fluids.WATER) || fluidState.is(Fluids.LAVA));
    }

    private static OreStyle styleForBlock(Block block, Identifier identifier) {
        OreStyle predefined = DEFAULT_STYLES.get(identifier.toString());
        if (predefined != null) {
            return predefined;
        }

        int hash = Math.abs(identifier.hashCode());
        float red = 0.30F + (((hash >> 16) & 0x7F) / 127.0F) * 0.60F;
        float green = 0.30F + (((hash >> 8) & 0x7F) / 127.0F) * 0.60F;
        float blue = 0.30F + ((hash & 0x7F) / 127.0F) * 0.60F;
        return new OreStyle(block.getName().getString(), red, green, blue);
    }

    public record OreStyle(String label, float red, float green, float blue) {
    }
}
