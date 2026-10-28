package net.parkabird.changedsynergy.ai;

import net.ltxprogrammer.changed.ability.AbstractAbility;
import net.ltxprogrammer.changed.ability.GrabEntityAbilityInstance;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.LivingEntityDataExtension;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Player escape must not depend on an NPC's current AI goal owning its idle tick. */
@Mod.EventBusSubscriber(modid = "changed_synergy")
public final class GrabEscapeService {
    private GrabEscapeService() {}

    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        var carrier = event.getEntity();
        if (!(carrier instanceof ChangedEntity || carrier instanceof Player)) return;
        var grab = AbstractAbility.getAbilityInstance(carrier, ChangedAbilities.GRAB_ENTITY_ABILITY.get());
        if (grab == null) return;
        tick(grab);
    }

    public static void tick(GrabEntityAbilityInstance grab) {
        var player = PlayerHugService.escapePlayer(grab);
        var carrier = grab.entity.getEntity();
        if (player == null || !player.isAlive() || player.isRemoved() || player.isSpectator()
                || !carrier.isAlive() || carrier.isRemoved() || player.level() != carrier.level()
                || grab.suited && grab.grabbedHasControl || TakeoverService.active(player)
                || carrier.getPersistentData().getBoolean("SynergyTakeoverClient")) return;
        if (!grab.suited) grab.grabbedHasControl = false;
        if (player instanceof LivingEntityDataExtension extension) {
            var linked = extension.getGrabbedBy();
            if (linked != null && linked != carrier) {
                var previous = AbstractAbility.getAbilityInstance(linked, ChangedAbilities.GRAB_ENTITY_ABILITY.get());
                if (previous != null && PlayerHugService.escapePlayer(previous) == player) return;
            }
            if (linked != carrier) extension.setGrabbedBy(carrier);
        }
        grab.handleEscape();
        if (!carrier.level().isClientSide() && grab.grabStrength <= 0) grab.releaseEntity(true);
    }
}
