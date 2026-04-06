package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hallow.client.HallowCameraController;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;

@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    @Shadow
    protected abstract boolean shouldShowEntityOutlines();

    @Shadow
    protected abstract boolean isSectionCompiledAndVisible(BlockPos blockPos);

    @Inject(method = "extractVisibleEntities", at = @At("TAIL"))
    private void hallow$restoreLocalPlayerForPlayerCamera(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState levelRenderState, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (!HallowCameraController.isCameraDetached() || player == null || client.level == null) {
            return;
        }

        if (client.getCameraEntity() == player) {
            return;
        }

        BlockPos blockPos = player.blockPosition();
        if (!client.level.isOutsideBuildHeight(blockPos.getY()) && !this.isSectionCompiledAndVisible(blockPos)) {
            return;
        }

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(!client.level.tickRateManager().isEntityFrozen(player));
        EntityRenderState renderState = client.getEntityRenderDispatcher().extractEntity(player, partialTick);
        levelRenderState.entityRenderStates.add(renderState);
        if (renderState.appearsGlowing() && this.shouldShowEntityOutlines()) {
            levelRenderState.haveGlowingEntities = true;
        }
    }
}
