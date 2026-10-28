package net.parkabird.changedsynergy.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.parkabird.changedsynergy.ai.CreaturePeerInteractionService;

/** A fresh gesture, never a client claim about a catch or a reward. */
public record ChasePlayInputPacket(int sessionId, int gesture) {
    public static final int CROUCH = 0, JUMP = 1;
    public static void encode(ChasePlayInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sessionId);
        buffer.writeByte(packet.gesture);
    }
    public static ChasePlayInputPacket decode(FriendlyByteBuf buffer) {
        return new ChasePlayInputPacket(buffer.readVarInt(), buffer.readUnsignedByte());
    }
    public static void handle(ChasePlayInputPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) CreaturePeerInteractionService.gesture(
                    context.getSender(), packet.sessionId, packet.gesture);
        });
        context.setPacketHandled(true);
    }
}
