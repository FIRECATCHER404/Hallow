package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hallow.client.HallowRuntimeState;
import com.hallow.client.config.HallowConfigManager;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

@Mixin(Gui.class)
abstract class GuiMixin {
    @Inject(method = "renderCameraOverlays", at = @At("HEAD"), cancellable = true)
    private void hallow$hideCameraOverlays(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HallowRuntimeState.isNoRenderEnabled() && HallowConfigManager.get().noRender.hideCameraOverlays) {
            ci.cancel();
        }
    }
}
