package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hallow.client.HallowCameraController;
import com.hallow.client.HallowInputCapture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Unique
    private final Minecraft hallow$client = Minecraft.getInstance();

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void hallow$blockMouseButtonsDuringChord(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        if (window == hallow$client.getWindow().handle() && HallowInputCapture.isChordCaptureActive(hallow$client)) {
            ci.cancel();
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void hallow$blockMouseLookDuringChord(long window, double mouseX, double mouseY, CallbackInfo ci) {
        if (window == hallow$client.getWindow().handle() && HallowInputCapture.isChordCaptureActive(hallow$client)) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void hallow$useScrollForPlayerCamera(long window, double horizontalAmount, double verticalAmount, CallbackInfo ci) {
        if (window != hallow$client.getWindow().handle()) {
            return;
        }

        if (HallowInputCapture.isChordCaptureActive(hallow$client)) {
            ci.cancel();
            return;
        }

        if (HallowCameraController.handleScroll(hallow$client, verticalAmount)) {
            ci.cancel();
        }
    }
}
