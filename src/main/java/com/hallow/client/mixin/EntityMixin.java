package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hallow.client.HallowRuntimeState;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

@Mixin(Entity.class)
abstract class EntityMixin {
    @Inject(method = "maxUpStep", at = @At("HEAD"), cancellable = true)
    private void hallow$boostStepHeight(CallbackInfoReturnable<Float> cir) {
        if (HallowRuntimeState.isStepAssistEnabled() && (Object) this instanceof LocalPlayer) {
            cir.setReturnValue(HallowRuntimeState.activeStepHeight());
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void hallow$disableEntityPushes(CallbackInfoReturnable<Boolean> cir) {
        if (HallowRuntimeState.isNoPushEnabled() && (Object) this instanceof LocalPlayer) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void hallow$disableFluidPushes(CallbackInfoReturnable<Boolean> cir) {
        if (HallowRuntimeState.isNoPushEnabled() && (Object) this instanceof LocalPlayer) {
            cir.setReturnValue(false);
        }
    }
}
