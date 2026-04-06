package com.hallow.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record HallowInvSetPayload(int inventorySlot, ItemStack stack) implements CustomPacketPayload {
    public static final Type<HallowInvSetPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hallow", "hallowinv_set"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HallowInvSetPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        HallowInvSetPayload::inventorySlot,
        ItemStack.OPTIONAL_STREAM_CODEC,
        HallowInvSetPayload::stack,
        HallowInvSetPayload::new
    );

    @Override
    public Type<HallowInvSetPayload> type() {
        return TYPE;
    }
}
