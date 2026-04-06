package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hallow.client.HallowRuntimeState;
import com.hallow.client.XRayRules;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase")
abstract class BlockStateBaseMixin {
    @Inject(
        method = "getRenderShape",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hallow$hideNonOreBlocks(CallbackInfoReturnable<RenderShape> cir) {
        BlockState state = (BlockState) (Object) this;
        if (HallowRuntimeState.isOreModeActive() && !XRayRules.shouldKeepVisibleForOreMode(state)) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}
