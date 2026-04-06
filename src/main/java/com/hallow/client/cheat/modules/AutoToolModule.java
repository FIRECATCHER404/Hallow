package com.hallow.client.cheat.modules;

import java.util.List;

import com.hallow.client.HallowInputCapture;
import com.hallow.client.cheat.CheatModule;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class AutoToolModule extends CheatModule {
    public AutoToolModule(int slot) {
        super("AutoTool", slot, true);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled() || client.player == null || client.level == null || HallowInputCapture.isChordCaptureActive(client)) {
            return;
        }

        if (!client.options.keyAttack.isDown() || !(client.hitResult instanceof BlockHitResult blockHit)) {
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        int selectedSlot = client.player.getInventory().getSelectedSlot();
        int bestSlot = selectedSlot;
        float bestScore = toolScore(client.player.getInventory().getItem(selectedSlot), state);

        for (int slot = 0; slot < 9; slot++) {
            float score = toolScore(client.player.getInventory().getItem(slot), state);
            if (score > bestScore + 1.0E-3F) {
                bestScore = score;
                bestSlot = slot;
            }
        }

        if (bestSlot != selectedSlot) {
            client.player.getInventory().setSelectedSlot(bestSlot);
        }
    }

    @Override
    public List<String> hudLines(Minecraft client) {
        return isEnabled() ? List.of("AutoTool: hotbar") : List.of();
    }

    private static float toolScore(ItemStack stack, BlockState state) {
        if (stack.isEmpty()) {
            return 0.0F;
        }

        float score = stack.getDestroySpeed(state);
        if (stack.isCorrectToolForDrops(state)) {
            score += 100.0F;
        }
        return score;
    }
}
