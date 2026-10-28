package net.parkabird.changedsynergy.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ltxprogrammer.changed.client.gui.AbstractRadialScreen;
import net.ltxprogrammer.changed.client.gui.GrabOverlay;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.util.Color3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.ChangedSynergyMod;
import static net.parkabird.changedsynergy.network.ChasePlayStatePacket.*;

/** Passive native HUD: countdown, distance and time have separate meanings. */
@Mod.EventBusSubscriber(modid = ChangedSynergyMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ChasePlayOverlay {
    private static final ResourceLocation BAR = net.ltxprogrammer.changed.Changed.modResource("textures/gui/grab_progress_bar_latex.png");
    private static final int TEXT = 0xFFFFFF, MUTED = 0xD3DFE4;
    private ChasePlayOverlay() {}

    @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("chase_play", (gui, graphics, partial, width, height) -> render(graphics, width, height));
    }

    private static void render(GuiGraphics g, int width, int height) {
        var mc = Minecraft.getInstance();
        var state = ChasePlayClient.current();
        if (state == null || state.stage() == HUG || mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui) return;
        float center = width / 2F;
        int top = Math.max(24, Math.min(32, height / 12));
        int countdown = ChasePlayClient.countdownTicks();
        boolean go = ChasePlayClient.justStarted() && state.stage() == CHASING;
        if (go || state.stage() == HEAD_START) {
            String label = state.stage() == HEAD_START && !state.playerChases() ? "chaser_starts"
                    : state.playerChases() ? "chaser" : "runner";
            line(g, Component.translatable("hud.changed_synergy.chase." + label), center, top - 15, TEXT);
            Component numeral = go ? Component.translatable("hud.changed_synergy.chase.go")
                    : Component.literal(Integer.toString(Math.max(1, (countdown + 19) / 20)));
            centered(g, numeral.getVisualOrderText(), center, top + 2, TEXT, 3);
            hint(g, center, top + 34, width);
            return;
        }
        boolean running = state.stage() == CHASING;
        if (running && mc.level.getEntity(state.partnerId()) instanceof ChangedEntity partner) {
            int seconds = (ChasePlayClient.phaseTicks() + 19) / 20;
            String role = state.playerChases() ? "chaser" : "runner";
            Component title = Component.translatable("hud.changed_synergy.chase.time", Component.translatable("hud.changed_synergy.chase." + role), seconds);
            line(g, title, center, top - 13, TEXT);
            double distance = mc.player.distanceTo(partner);
            var colors = ChangedEntities.getEntityColor(partner);
            Color3 color = new AbstractRadialScreen.ColorScheme(colors.getFirst(), colors.getSecond()).setForegroundToBright().foreground();
            int left = width / 2 - 91;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.pose().pushPose();
            g.pose().translate(center - width / 2 + 0.5F, 0, 0);
            GrabOverlay.renderBackground(g, BAR, left, top, 182, 10, Color3.WHITE);
            GrabOverlay.renderForeground(g, BAR, left, top, 182, 10, distanceProgress(distance), color);
            GrabOverlay.renderSuit(g, BAR, left, top, 182, 10, 1, color);
            g.pose().popPose();
            g.setColor(1, 1, 1, 1);
            line(g, Component.translatable("hud.changed_synergy.chase.distance", String.format(java.util.Locale.ROOT, "%.1f", distance)), center, top + 14, TEXT);
            hint(g, center, top + 28, width);
        } else {
            line(g, Component.translatable("hud.changed_synergy.chase.title"), center, top - 13, TEXT);
            hint(g, center, top + 1, width);
        }
    }

    // Visual distance only; the server still validates contact and player input.
    static float distanceProgress(double distance) {
        return Double.isFinite(distance) ? Mth.clamp((float)((16 - distance) / 15), 0, 1) : 0;
    }

    private static void hint(GuiGraphics g, float center, int top, int screenWidth) {
        var font = Minecraft.getInstance().font;
        for (var text : font.split(ChasePlayClient.hint(), Math.min(280, Math.max(80, screenWidth - 24)))) {
            centered(g, text, center, top, MUTED, 1);
            top += font.lineHeight + 2;
        }
    }

    private static void line(GuiGraphics g, Component text, float center, int top, int color) {
        centered(g, text.getVisualOrderText(), center, top, color, 1);
    }

    private static void centered(GuiGraphics g, FormattedCharSequence text, float center, int top, int color, int scale) {
        var font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(center, top, 0);
        g.pose().scale(scale, scale, 1);
        // Font width includes the last glyph's spacing pixel. Centre the visible
        // glyphs instead, keeping odd-width text and enlarged digits on the bar axis.
        g.pose().translate(-(font.width(text) - 1) / 2F, 0, 0);
        g.drawString(font, text, 0, 0, color, true);
        g.pose().popPose();
    }
}
