package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hallow.client.HallowCameraController;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Unique
    private final Minecraft hallow$client = Minecraft.getInstance();

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void hallow$useScrollForPlayerCamera(long window, double horizontalAmount, double verticalAmount, CallbackInfo ci) {
        if (window != hallow$client.getWindow().handle()) {
            return;
        }

        if (HallowCameraController.handleScroll(hallow$client, verticalAmount)) {
            ci.cancel();
        }
    }
}
