package net.parkabird.changedsynergy.client;

import net.ltxprogrammer.changed.client.gui.AbilityRadialScreen;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.PlayerHugService;
import net.parkabird.changedsynergy.network.ChangedSynergyNetwork;
import net.parkabird.changedsynergy.network.PlayerHugActionPacket;

@Mod.EventBusSubscriber(modid = "changed_synergy", value = Dist.CLIENT)
public final class PlayerHugClient {
    @SubscribeEvent public static void open(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof AbilityRadialScreen screen) {
            if (PlayerHugService.unsupported(screen.variant.getChangedEntity())) screen.abilities.remove(ChangedAbilities.GRAB_ENTITY_ABILITY.get());
            else if (screen.abilities.contains(ChangedAbilities.GRAB_ENTITY_ABILITY.get()))
                ChangedSynergyNetwork.CHANNEL.sendToServer(new PlayerHugActionPacket(PlayerHugActionPacket.SYNC, -1));
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 1 || !(event.getScreen() instanceof AbilityRadialScreen screen)
                || screen instanceof RadialWheelAnimationAccess animation && !animation.changedSynergy$canConfirm()) return;
        var section = screen.getSectionAt((int)event.getMouseX(), (int)event.getMouseY());
        if (section.isEmpty() || section.get() >= screen.abilities.size()
                || screen.abilities.get(section.get()) != ChangedAbilities.GRAB_ENTITY_ABILITY.get()) return;
        event.setCanceled(true);
        if (ServerConfigClient.value(ChangedSynergyConfig.COMMON.playerFriendlyHugs))
            ChangedSynergyNetwork.CHANNEL.sendToServer(new PlayerHugActionPacket(PlayerHugActionPacket.TOGGLE, -1));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { PlayerHugService.clearClient(); }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) PlayerHugService.updateClient();
    }
}
