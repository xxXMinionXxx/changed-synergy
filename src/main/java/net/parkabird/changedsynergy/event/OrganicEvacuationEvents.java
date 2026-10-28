package net.parkabird.changedsynergy.event;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.ChangedSynergyMod;
import net.parkabird.changedsynergy.ai.LatexSocialMemory;

/** Redirects combat hits from an evacuated owner into the organic carrier. */
@Mod.EventBusSubscriber(
        modid = ChangedSynergyMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OrganicEvacuationEvents {
    private OrganicEvacuationEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEvacuatedOwnerHurt(LivingHurtEvent event) {
        ServerPlayer owner = event.getEntity() instanceof ServerPlayer direct ? direct
                : event.getEntity() instanceof ChangedEntity body
                    ? net.parkabird.changedsynergy.compat.TrueTransfurCompat.bodyOwner(body) : null;
        if (event.isCanceled()
                || event.getAmount() <= 0.0F
                || owner == null
                || owner.level().isClientSide
                || event.getSource().getEntity() == null) {
            return;
        }
        ChangedEntity carrier = LatexSocialMemory.loadedBondedCreatures(owner)
                .stream()
                .filter(candidate -> candidate.isAlive()
                        && candidate.level() == owner.level()
                        && LatexSocialMemory.isOrganicEvacuationActive(
                                candidate, owner))
                .findFirst()
                .orElse(null);
        if (carrier == null) {
            return;
        }
        float damage = event.getAmount();
        event.setCanceled(true);
        carrier.hurt(event.getSource(), damage);
    }
}
