package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hallow.client.HallowCameraController;
import com.hallow.client.HallowRuntimeState;
import com.hallow.client.config.HallowConfigManager;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("TAIL"))
    private void hallow$applySpectatorView(Level level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        HallowCameraController.CameraPoint fixedPoint = HallowCameraController.currentFixedPoint(client);
        if (fixedPoint != null) {
            this.setPosition(fixedPoint.position());
            this.setRotation(fixedPoint.yaw(), fixedPoint.pitch());
            ((CameraAccessor) this).hallow$setDetached(true);
            return;
        }

        HallowCameraController.CameraPoint browsePoint = HallowCameraController.currentBrowsePoint(client);
        if (browsePoint != null) {
            this.setPosition(browsePoint.position());
            this.setRotation(browsePoint.yaw(), browsePoint.pitch());
            return;
        }

        if (detached || !HallowRuntimeState.isSpectatorViewActive()) {
            return;
        }

        if (client.player == null || entity != client.player) {
            return;
        }

        HitResult hit = entity.pick(HallowConfigManager.get().xray.spectatorPeekDistance, partialTick, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockState state = level.getBlockState(blockHit.getBlockPos());
        if (state.isAir()) {
            return;
        }

        Vec3 look = entity.getLookAngle();
        Vec3 pushedPosition = hit.getLocation().add(look.scale(HallowConfigManager.get().xray.spectatorPush));
        this.setPosition(pushedPosition);
    }
}
