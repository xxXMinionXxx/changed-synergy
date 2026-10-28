package net.parkabird.changedsynergy.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "changed_synergy", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CompanionSeatOverlay {
    @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("companion_seat", (gui, graphics, partial, width, height) -> {
            var mc = Minecraft.getInstance();
            if (!CompanionSeatClient.active() || mc.screen != null || mc.options.hideGui) return;
            int y = height - 84;
            for (var line : mc.font.split(CompanionSeatClient.hint(), Math.max(80, width - 32))) {
                graphics.drawString(mc.font, line, (width - mc.font.width(line)) / 2, y, 0xFFFFFF, true); y += 10;
            }
            for (var line : mc.font.split(CompanionSeatClient.target(), Math.max(80, width - 32))) {
                graphics.drawString(mc.font, line, (width - mc.font.width(line)) / 2, y, 0xD3DFE4, true); y += 10;
            }
        });
    }
}
