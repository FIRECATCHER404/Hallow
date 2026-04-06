package com.hallow.client.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hallow.client.HallowCameraController;
import com.hallow.client.HallowRuntimeState;
import com.hallow.client.config.HallowConfigManager;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void hallow$forceNightVisionBrightness(LivingEntity entity, float tickProgress, CallbackInfoReturnable<Float> cir) {
        if (HallowRuntimeState.isFullbrightEnabled()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void hallow$hideDamageTilt(PoseStack matrices, float tickProgress, CallbackInfo ci) {
        if (HallowCameraController.isFixedViewActive() || (HallowRuntimeState.isNoRenderEnabled() && HallowConfigManager.get().noRender.hideDamageTilt)) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void hallow$hideViewBobbing(PoseStack matrices, float tickProgress, CallbackInfo ci) {
        if (HallowCameraController.isFixedViewActive() || (HallowRuntimeState.isNoRenderEnabled() && HallowConfigManager.get().noRender.hideViewBobbing)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void hallow$hideHandsForCameraViews(float tickProgress, boolean renderLevel, Matrix4f matrix4f, CallbackInfo ci) {
        if (HallowCameraController.isCameraDetached()) {
            ci.cancel();
        }
    }
}
