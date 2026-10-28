package net.parkabird.changedsynergy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.compat.CompanionSeats;
import net.parkabird.changedsynergy.network.ChangedSynergyNetwork;
import net.parkabird.changedsynergy.network.CompanionSeatInputPacket;
import net.parkabird.changedsynergy.network.CompanionSeatStatePacket;

@Mod.EventBusSubscriber(modid = "changed_synergy", value = Dist.CLIENT)
public final class CompanionSeatClient {
    private static CompanionSeatStatePacket selection;
    private static Object level;
    private static boolean armed;
    private static int remaining;
    private CompanionSeatClient() {}

    public static void receive(CompanionSeatStatePacket state) {
        var mc = Minecraft.getInstance();
        selection = state.token() == 0 ? null : state;
        level = mc.level; armed = !mc.options.keyUse.isDown(); remaining = 300;
    }
    private static void cancel() {
        if (selection != null && Minecraft.getInstance().getConnection() != null)
            ChangedSynergyNetwork.CHANNEL.sendToServer(new CompanionSeatInputPacket(selection.token(), -1, null, true));
        selection = null; level = null;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || selection == null) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level != level || !mc.player.isAlive() || --remaining <= 0) { cancel(); return; }
        if (!mc.options.keyUse.isDown()) armed = true;
    }
    @SubscribeEvent public static void screen(ScreenEvent.Opening event) {
        if (selection == null || event.getNewScreen() == null) return;
        if (event.getNewScreen() instanceof PauseScreen) event.setCanceled(true);
        cancel();
    }
    @SubscribeEvent public static void use(InputEvent.InteractionKeyMappingTriggered event) {
        if (selection == null || !event.isUseItem()) return;
        event.setCanceled(true); event.setSwingHand(false);
        var mc = Minecraft.getInstance();
        if (!armed || mc.screen != null) return;
        armed = false;
        int entityId = mc.hitResult instanceof EntityHitResult hit ? hit.getEntity().getId() : -1;
        var block = mc.hitResult instanceof BlockHitResult hit ? hit.getBlockPos() : null;
        ChangedSynergyNetwork.CHANNEL.sendToServer(new CompanionSeatInputPacket(selection.token(), entityId, block, false));
    }
    static Component hint() {
        var mc = Minecraft.getInstance();
        return selection == null ? Component.empty() : Component.translatable("hud.changed_synergy.seating.choose",
                selection.name(), mc.options.keyUse.getTranslatedKeyMessage());
    }
    static Component target() {
        var mc = Minecraft.getInstance();
        if (selection == null || mc.level == null) return Component.empty();
        if (mc.hitResult instanceof EntityHitResult hit && CompanionSeats.supported(hit.getEntity(), false)) {
            int free = CompanionSeats.freeSeats(hit.getEntity());
            return free < 0 ? Component.translatable("hud.changed_synergy.seating.try", hit.getEntity().getDisplayName())
                    : Component.translatable("hud.changed_synergy.seating.free", hit.getEntity().getDisplayName(),
                            free);
        }
        if (mc.hitResult instanceof BlockHitResult hit) {
            var pos = CompanionSeats.blockSeat(mc.level, hit.getBlockPos());
            if (pos != null) return Component.translatable("hud.changed_synergy.seating.free",
                    mc.level.getBlockState(pos).getBlock().getName(), CompanionSeats.blockAvailable(mc.level, pos) ? 1 : 0);
        }
        return Component.translatable("message.changed_synergy.seating.invalid");
    }
    static boolean active() { return selection != null; }
}
