package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("position")
    Vec3 hallow$getPosition();

    @Accessor("detached")
    void hallow$setDetached(boolean detached);

    @Accessor("xRot")
    float hallow$getXRot();

    @Accessor("yRot")
    float hallow$getYRot();
}
