package net.parkabird.changedsynergy.client;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.ChangedSynergyMod;
import net.parkabird.changedsynergy.world.inventory.CentaurMountService;

@Mod.EventBusSubscriber(modid = ChangedSynergyMod.MOD_ID, value = Dist.CLIENT)
public final class CentaurMountClient {
    @SubscribeEvent public static void onMount(EntityMountEvent event) {
        var mc = Minecraft.getInstance();
        if (event.isMounting() && event.getEntityMounting() == mc.player
                && event.getEntityBeingMounted() instanceof ChangedEntity taur
                && CentaurMountService.isCentaur(taur) && mc.options.toggleCrouch().get()) {
            if (mc.options.keyShift instanceof ToggleKeyMapping toggle && toggle.isDown()) toggle.setDown(true);
            mc.player.input.shiftKeyDown = false;
            mc.player.setShiftKeyDown(false);
        }
    }
}
