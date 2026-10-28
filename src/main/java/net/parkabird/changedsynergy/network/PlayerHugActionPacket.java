package net.parkabird.changedsynergy.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.parkabird.changedsynergy.ai.PlayerHugService;

public record PlayerHugActionPacket(int action, int target) {
    public static final int TOGGLE = 0, GRAB = 1, RELEASE = 2, SYNC = 3;
    public static void encode(PlayerHugActionPacket p, FriendlyByteBuf b) { b.writeVarInt(p.action); b.writeVarInt(p.target); }
    public static PlayerHugActionPacket decode(FriendlyByteBuf b) { return new PlayerHugActionPacket(b.readVarInt(), b.readVarInt()); }
    public static void handle(PlayerHugActionPacket p, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        var sender = context.getSender();
        if (sender != null) context.enqueueWork(() -> PlayerHugService.action(sender, p.action, p.target));
        context.setPacketHandled(true);
    }
}
