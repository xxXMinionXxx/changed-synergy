package net.parkabird.changedsynergy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.ChangedSynergyMod;
import net.parkabird.changedsynergy.network.ChasePlayInputPacket;
import net.parkabird.changedsynergy.network.ChasePlayStatePacket;
import net.parkabird.changedsynergy.network.ChangedSynergyNetwork;

/** Observes normal controls without consuming movement or changing toggle-crouch settings. */
@Mod.EventBusSubscriber(modid = ChangedSynergyMod.MOD_ID, value = Dist.CLIENT)
public final class ChasePlayClient {
    private static ChasePlayStatePacket current;
    private static Level level;
    private static boolean crouched, jumped;
    private static int remaining, phaseTicks, phaseAge;

    private ChasePlayClient() {}

    @SubscribeEvent public static void onKey(InputEvent.Key event) {
        var mc = Minecraft.getInstance();
        if (event.getAction() == InputConstants.PRESS && mc.options.keyShift.matches(event.getKey(), event.getScanCode())) crouchPress();
    }

    @SubscribeEvent public static void onMouse(InputEvent.MouseButton.Post event) {
        if (event.getAction() == InputConstants.PRESS && Minecraft.getInstance().options.keyShift.matchesMouse(event.getButton())) crouchPress();
    }

    private static void crouchPress() {
        var mc = Minecraft.getInstance();
        if (current != null && current.stage() >= ChasePlayStatePacket.READY && mc.screen == null
                && mc.player != null && mc.player.isAlive()) send(ChasePlayInputPacket.CROUCH);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) { consumeTouch(event, event.getTarget().getId()); }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onInteractAt(PlayerInteractEvent.EntityInteractSpecific event) { consumeTouch(event, event.getTarget().getId()); }

    private static void consumeTouch(PlayerInteractEvent event, int targetId) {
        // Vanilla has already sent its interaction packet; consume only local menu/item use.
        if (event.getEntity() == Minecraft.getInstance().player && current != null
                && current.partnerId() == targetId && event.getHand() == InteractionHand.MAIN_HAND) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void receive(ChasePlayStatePacket packet) {
        var mc = Minecraft.getInstance();
        if (packet.stage() == ChasePlayStatePacket.CLEAR) {
            if (current != null && current.sessionId() == packet.sessionId()) clear();
            return;
        }
        if (current == null || current.sessionId() != packet.sessionId()) {
            crouched = mc.options.keyShift.isDown();
            jumped = mc.options.keyJump.isDown();
            level = mc.level;
        }
        if (current == null || current.sessionId() != packet.sessionId() || current.stage() != packet.stage()) phaseAge = 0;
        current = packet;
        phaseTicks = packet.ticksRemaining();
        remaining = packet.ticksRemaining() + 40;
    }

    @SubscribeEvent public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || current == null) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level != level || !mc.player.isAlive() || --remaining <= 0) {
            clear();
            return;
        }
        boolean crouch = mc.options.keyShift.isDown(), jump = mc.options.keyJump.isDown();
        phaseTicks = Math.max(0, phaseTicks - 1);
        phaseAge++;
        int gesture = freshGesture(crouch, jump);
        if (mc.screen == null && gesture >= 0) send(gesture);
    }

    static ChasePlayStatePacket current() { return current; }
    static int phaseTicks() { return phaseTicks; }
    static boolean justStarted() { return phaseAge < 12; }

    static int countdownTicks() {
        if (current == null) return 0;
        return current.stage() == ChasePlayStatePacket.HEAD_START ? phaseTicks : 0;
    }

    static Component hint() {
        var mc = Minecraft.getInstance();
        String key = switch (current.stage()) {
            case ChasePlayStatePacket.INVITED -> "choose";
            case ChasePlayStatePacket.READY -> "ready";
            case ChasePlayStatePacket.HEAD_START -> current.playerChases() ? "wait" : "head_start_run";
            case ChasePlayStatePacket.CHASING -> current.playerChases() ? "chase" : "run";
            case ChasePlayStatePacket.HUG -> "hug";
            case ChasePlayStatePacket.FALSE_START -> "false_start";
            default -> "comfort";
        };
        return Component.translatable("message.changed_synergy.chase." + key,
                mc.options.keyShift.getTranslatedKeyMessage(), mc.options.keyJump.getTranslatedKeyMessage());
    }

    private static int freshGesture(boolean crouch, boolean jump) {
        int gesture = current.stage() != ChasePlayStatePacket.INVITED ? -1
                : crouch && !crouched ? ChasePlayInputPacket.CROUCH
                : jump && !jumped ? ChasePlayInputPacket.JUMP : -1;
        crouched = crouch;
        jumped = jump;
        return gesture;
    }

    private static void send(int gesture) {
        ChangedSynergyNetwork.CHANNEL.sendToServer(new ChasePlayInputPacket(current.sessionId(), gesture));
    }

    private static void clear() {
        current = null;
        level = null;
        remaining = phaseTicks = phaseAge = 0;
    }
}
