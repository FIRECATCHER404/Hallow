package com.hallow.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.hallow.client.HallowRuntimeState;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

@Mixin(LightTexture.class)
abstract class LightTextureMixin {
    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean hallow$treatFullbrightAsNightVision(LocalPlayer player, Holder<MobEffect> effect) {
        if (HallowRuntimeState.isFullbrightEnabled() && effect == MobEffects.NIGHT_VISION) {
            return true;
        }

        return player.hasEffect(effect);
    }
}
