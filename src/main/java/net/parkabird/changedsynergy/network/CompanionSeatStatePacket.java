package net.parkabird.changedsynergy.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.parkabird.changedsynergy.client.CompanionSeatClient;

public record CompanionSeatStatePacket(int token, int creatureId, Component name) {
    public static void encode(CompanionSeatStatePacket value, FriendlyByteBuf buf) {
        buf.writeVarInt(value.token); buf.writeVarInt(value.creatureId); buf.writeComponent(value.name);
    }
    public static CompanionSeatStatePacket decode(FriendlyByteBuf buf) {
        return new CompanionSeatStatePacket(buf.readVarInt(), buf.readVarInt(), buf.readComponent());
    }
    public static void handle(CompanionSeatStatePacket value, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CompanionSeatClient.receive(value)));
        context.setPacketHandled(true);
    }
}
