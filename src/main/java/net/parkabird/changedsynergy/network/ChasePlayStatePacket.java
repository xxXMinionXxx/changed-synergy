package net.parkabird.changedsynergy.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.parkabird.changedsynergy.client.ChasePlayClient;

public record ChasePlayStatePacket(int sessionId, int stage, int ticksRemaining, boolean playerChases, int partnerId) {
    public static final int CLEAR = 0, INVITED = 1, READY = 2, HEAD_START = 3, CHASING = 4, HUG = 5, COMFORT = 6, FALSE_START = 7;
    public static final int HEAD_START_TICKS = 60;
    public static void encode(ChasePlayStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sessionId);
        buffer.writeByte(packet.stage);
        buffer.writeVarInt(packet.ticksRemaining);
        buffer.writeBoolean(packet.playerChases);
        buffer.writeVarInt(packet.partnerId);
    }
    public static ChasePlayStatePacket decode(FriendlyByteBuf buffer) {
        return new ChasePlayStatePacket(buffer.readVarInt(), buffer.readUnsignedByte(), buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt());
    }
    public static void handle(ChasePlayStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChasePlayClient.receive(packet)));
        context.setPacketHandled(true);
    }
}
