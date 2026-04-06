package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hallow.client.HallowRuntimeState;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

@Mixin(Player.class)
abstract class PlayerMixin {
    @Inject(method = "hasInfiniteMaterials", at = @At("HEAD"), cancellable = true)
    private void hallow$allowCreativeAccess(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide() && HallowRuntimeState.hasCreativeAccess(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canUseGameMasterBlocks", at = @At("HEAD"), cancellable = true)
    private void hallow$allowOperatorTab(CallbackInfoReturnable<Boolean> cir) {
        if (HallowRuntimeState.hasCreativeAccess((Player) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    private void hallow$enableSafeWalk(CallbackInfoReturnable<Boolean> cir) {
        if (HallowRuntimeState.isSafeWalkEnabled() && (Object) this instanceof LocalPlayer) {
            cir.setReturnValue(true);
        }
    }
}
