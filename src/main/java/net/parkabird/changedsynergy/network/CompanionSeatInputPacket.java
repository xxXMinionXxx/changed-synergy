package net.parkabird.changedsynergy.network;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.parkabird.changedsynergy.ai.CompanionSeatingService;

/** Target selection is an intent; ownership, reach and seat admission remain on the server. */
public record CompanionSeatInputPacket(int token, int entityId, @Nullable BlockPos block, boolean cancel) {
    public static void encode(CompanionSeatInputPacket value, FriendlyByteBuf buf) {
        buf.writeVarInt(value.token); buf.writeVarInt(value.entityId); buf.writeBoolean(value.block != null);
        if (value.block != null) buf.writeBlockPos(value.block);
        buf.writeBoolean(value.cancel);
    }
    public static CompanionSeatInputPacket decode(FriendlyByteBuf buf) {
        return new CompanionSeatInputPacket(buf.readVarInt(), buf.readVarInt(), buf.readBoolean() ? buf.readBlockPos() : null, buf.readBoolean());
    }
    public static void handle(CompanionSeatInputPacket value, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> { if (context.getSender() != null) CompanionSeatingService.select(
                context.getSender(), value.token, value.entityId, value.block, value.cancel); });
        context.setPacketHandled(true);
    }
}
