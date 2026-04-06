package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hallow.client.HallowCameraController;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void hallow$keepSavedCameraEntitiesVisible(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (HallowCameraController.isFixedViewActive()) {
            cir.setReturnValue(entity.shouldRender(x, y, z));
        }
    }
}
