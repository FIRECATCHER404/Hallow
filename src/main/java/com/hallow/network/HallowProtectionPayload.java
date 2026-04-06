package com.hallow.network;

import com.hallow.protection.HallowProtectionManager.ProtectionState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HallowProtectionPayload(
    boolean invulnerable,
    boolean blockDrowningDamage,
    boolean blockFallDamage,
    boolean blockFreezeDamage,
    boolean blockFireDamage,
    boolean keepInventory,
    boolean blockPvpDamage
) implements CustomPacketPayload {
    public static final Type<HallowProtectionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hallow", "protection_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HallowProtectionPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        HallowProtectionPayload::invulnerable,
        ByteBufCodecs.BOOL,
        HallowProtectionPayload::blockDrowningDamage,
        ByteBufCodecs.BOOL,
        HallowProtectionPayload::blockFallDamage,
        ByteBufCodecs.BOOL,
        HallowProtectionPayload::blockFreezeDamage,
        ByteBufCodecs.BOOL,
        HallowProtectionPayload::blockFireDamage,
        ByteBufCodecs.BOOL,
        HallowProtectionPayload::keepInventory,
        ByteBufCodecs.BOOL,
        HallowProtectionPayload::blockPvpDamage,
        HallowProtectionPayload::new
    );

    public ProtectionState asState() {
        return new ProtectionState(
            invulnerable,
            blockDrowningDamage,
            blockFallDamage,
            blockFreezeDamage,
            blockFireDamage,
            keepInventory,
            blockPvpDamage
        );
    }

    @Override
    public Type<HallowProtectionPayload> type() {
        return TYPE;
    }
}
