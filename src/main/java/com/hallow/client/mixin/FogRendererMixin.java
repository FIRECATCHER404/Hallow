package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hallow.client.HallowRuntimeState;
import com.hallow.client.config.HallowConfigManager;

@Mixin(targets = "net.minecraft.client.renderer.fog.FogRenderer")
abstract class FogRendererMixin {
    @Inject(method = "toggleFog", at = @At("HEAD"), cancellable = true)
    private static void hallow$disableFog(CallbackInfoReturnable<Boolean> cir) {
        if (HallowRuntimeState.isNoRenderEnabled() && HallowConfigManager.get().noRender.hideFog) {
            cir.setReturnValue(false);
        }
    }
}
