package com.hallow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hallow.protection.HallowProtectionManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@Mixin(LivingEntity.class)
abstract class LivingEntityProtectionMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void hallow$keepProtectedInventory(ServerLevel level, DamageSource source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide() && entity instanceof Player player && HallowProtectionManager.keepInventory(player)) {
            ci.cancel();
        }
    }
}
