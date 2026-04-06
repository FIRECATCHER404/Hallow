package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hallow.client.HallowInputCapture;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

@Mixin(KeyMapping.class)
abstract class KeyMappingMixin {
    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void hallow$blockClickQueue(InputConstants.Key key, CallbackInfo ci) {
        if (HallowInputCapture.isChordCaptureActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private static void hallow$blockHeldState(InputConstants.Key key, boolean pressed, CallbackInfo ci) {
        if (HallowInputCapture.isChordCaptureActive()) {
            ci.cancel();
        }
    }
}
