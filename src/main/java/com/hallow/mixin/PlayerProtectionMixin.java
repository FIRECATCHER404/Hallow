package com.hallow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hallow.protection.HallowProtectionManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

@Mixin(Player.class)
abstract class PlayerProtectionMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void hallow$cancelProtectedDamage(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide() && HallowProtectionManager.blocksDamage(player, source)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void hallow$blockConfiguredDamage(ServerLevel level, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide() && HallowProtectionManager.blocksDamage(player, source)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void hallow$cancelProtectedFallDamage(double fallDistance, float damageMultiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide() && HallowProtectionManager.blocksFallDamage(player)) {
            player.resetFallDistance();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canHarmPlayer", at = @At("HEAD"), cancellable = true)
    private void hallow$blockPvpAgainstProtectedTarget(Player target, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide() && HallowProtectionManager.blocksPvp(target)) {
            cir.setReturnValue(false);
        }
    }
}
