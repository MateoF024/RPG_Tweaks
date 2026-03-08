package org.mateof24.rpg_tweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mateof24.rpg_tweaks.RPG_Tweaks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record S2CUnlockNotificationPacket(String skill, int level, List<ResourceLocation> items)
        implements CustomPacketPayload {

    public static final Type<S2CUnlockNotificationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RPG_Tweaks.MODID, "unlock_notification"));

    public static final StreamCodec<FriendlyByteBuf, S2CUnlockNotificationPacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUtf(pkt.skill);
                buf.writeVarInt(pkt.level);
                buf.writeCollection(pkt.items, FriendlyByteBuf::writeResourceLocation);
            },
            buf -> new S2CUnlockNotificationPacket(
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readList(FriendlyByteBuf::readResourceLocation)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, String skill, int level, List<String> itemIds) {
        List<ResourceLocation> items = itemIds.stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .toList();
        if (!items.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new S2CUnlockNotificationPacket(skill, level, items));
        }
    }

    public static void handle(S2CUnlockNotificationPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                org.mateof24.rpg_tweaks.client.ReskillableUnlockToast.show(
                        packet.skill(), packet.level(), packet.items()));
    }
}