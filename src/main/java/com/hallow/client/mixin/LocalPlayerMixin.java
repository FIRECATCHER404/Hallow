package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hallow.client.HallowRuntimeState;

import net.minecraft.client.player.LocalPlayer;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    @Inject(method = "isSlowDueToUsingItem", at = @At("HEAD"), cancellable = true)
    private void hallow$disableItemUseSlowdown(CallbackInfoReturnable<Boolean> cir) {
        if (HallowRuntimeState.isNoSlowEnabled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "itemUseSpeedMultiplier", at = @At("HEAD"), cancellable = true)
    private void hallow$restoreFullUseSpeed(CallbackInfoReturnable<Float> cir) {
        if (HallowRuntimeState.isNoSlowEnabled()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean hallow$spoofGroundStateForNoFall(LocalPlayer player) {
        if (player == (Object) this && HallowRuntimeState.shouldSpoofNoFall(player)) {
            return true;
        }

        return player.onGround();
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void hallow$clearClientFallDistance(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (HallowRuntimeState.shouldSpoofNoFall(player)) {
            player.resetFallDistance();
        }
    }
}
