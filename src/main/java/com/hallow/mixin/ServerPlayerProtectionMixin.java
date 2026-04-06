package com.hallow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hallow.protection.HallowProtectionManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerProtectionMixin {
    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void hallow$restoreProtectedInventory(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!HallowProtectionManager.keepInventory(player)) {
            return;
        }

        player.getInventory().replaceWith(oldPlayer.getInventory());
        player.experienceLevel = oldPlayer.experienceLevel;
        player.totalExperience = oldPlayer.totalExperience;
        player.experienceProgress = oldPlayer.experienceProgress;
        player.setScore(oldPlayer.getScore());
        player.inventoryMenu.broadcastChanges();

        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    @Inject(method = "checkFallDamage", at = @At("HEAD"), cancellable = true)
    private void hallow$clearProtectedFallDistance(double heightDifference, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (HallowProtectionManager.blocksFallDamage(player)) {
            player.resetFallDistance();
            ci.cancel();
        }
    }
}
