package net.parkabird.changedsynergy.client;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.parkabird.changedsynergy.ChangedSynergyClientConfig;
import net.parkabird.changedsynergy.origin.NativeOriginPolicy;
import net.minecraftforge.eventbus.api.EventPriority;
import net.parkabird.changedsynergy.ChangedSynergyMod;

@Mod.EventBusSubscriber(modid = ChangedSynergyMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NameVisibilityClientEvents {
    private static final double NAME_RENDER_DISTANCE_SQR = 10.0D * 10.0D;

    private NameVisibilityClientEvents() {
    }

    public static boolean hidesNativePlayerName(Entity entity) {
        Player player = entity instanceof Player direct ? direct
                : entity instanceof ChangedEntity form ? form.getUnderlyingPlayer() : null;
        return player != null && !ChangedSynergyClientConfig.CLIENT.showNativePlayerNames.get()
                && net.minecraftforge.fml.ModList.get().isLoaded("true_transfur")
                && NativeOriginPolicy.clientNatives.contains(player.getUUID())
                && ProcessTransfur.isPlayerTransfurred(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (hidesNativePlayerName(event.getEntity())) {event.setResult(Result.DENY);return;}
        if (!(event.getEntity() instanceof ChangedEntity mob)) {
            return;
        }

        // Keep the profile for social interaction without rendering a floating NPC label.
        if (PlayerSocialClient.isPlayerBody(mob)) {
            event.setResult(Result.DENY);
            return;
        }
        if (!mob.hasCustomName()) return;

        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity != null
                && cameraEntity.distanceToSqr(mob) > NAME_RENDER_DISTANCE_SQR) {
            event.setResult(Result.DENY);
        }
    }
}
