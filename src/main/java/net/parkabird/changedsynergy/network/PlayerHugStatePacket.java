package net.parkabird.changedsynergy.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.parkabird.changedsynergy.ai.PlayerHugService;

public record PlayerHugStatePacket(UUID carrier, int carrierId, UUID owner, UUID target, int targetId, UUID targetOwner, boolean friendly, boolean uprightCarry) {
    public static void encode(PlayerHugStatePacket p, FriendlyByteBuf b) {
        b.writeUUID(p.carrier); b.writeVarInt(p.carrierId); b.writeUUID(p.owner); b.writeBoolean(p.target != null);
        if (p.target != null) { b.writeUUID(p.target); b.writeVarInt(p.targetId); }
        b.writeBoolean(p.targetOwner != null);
        if (p.targetOwner != null) b.writeUUID(p.targetOwner);
        b.writeBoolean(p.friendly);
        b.writeBoolean(p.uprightCarry);
    }
    public static PlayerHugStatePacket decode(FriendlyByteBuf b) {
        UUID carrier = b.readUUID(); int carrierId = b.readVarInt(); UUID owner = b.readUUID();
        boolean hasTarget = b.readBoolean(); UUID target = hasTarget ? b.readUUID() : null; int targetId = hasTarget ? b.readVarInt() : -1;
        return new PlayerHugStatePacket(carrier, carrierId, owner, target, targetId, b.readBoolean() ? b.readUUID() : null, b.readBoolean(), b.readBoolean());
    }
    public static void handle(PlayerHugStatePacket p, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get(); context.enqueueWork(() -> PlayerHugService.receive(p)); context.setPacketHandled(true);
    }
}
