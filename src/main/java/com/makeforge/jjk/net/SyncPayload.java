package com.makeforge.jjk.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> Client: current technique ordinal + CE for the HUD. */
public record SyncPayload(int techniqueOrdinal, float ce, float maxCe) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("jjk", "sync");
    public static final CustomPacketPayload.Type<SyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,   SyncPayload::techniqueOrdinal,
                    ByteBufCodecs.FLOAT,     SyncPayload::ce,
                    ByteBufCodecs.FLOAT,     SyncPayload::maxCe,
                    SyncPayload::new
            );

    @Override
    public CustomPacketPayload.Type<SyncPayload> type() {
        return TYPE;
    }
}
