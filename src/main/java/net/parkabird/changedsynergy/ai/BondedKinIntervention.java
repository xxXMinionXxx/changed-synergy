package net.parkabird.changedsynergy.ai;

import javax.annotation.Nullable;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.compat.TrueTransfurCompat;
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;

/** One pending intervention, routed by the owner's real form rather than a temporary suit. */
public final class BondedKinIntervention {
    private static final String OWNER = "ChangedSynergyBondConfinementOwner";
    private static final String UNTIL = "ChangedSynergyBondConfinementUntil";
    private static final String COOLDOWN = "ChangedSynergyBondConfinementCooldown";

    private BondedKinIntervention() {}

    public static boolean usesEvacuation(ChangedEntity bond, ServerPlayer owner) {
        return LatexSocialMemory.isOrganic(bond) || ProcessTransfur.getPlayerTransfurVariantSafe(owner)
                .filter(form -> !form.isTemporaryFromSuit()).isPresent();
    }

    public static boolean canIntervene(ChangedEntity bond, ServerPlayer owner) {
        return owner != null && bond.isAlive() && owner.isAlive()
                && !bond.isRemoved() && !owner.isRemoved() && bond.level() == owner.level()
                && !owner.isCreative() && !owner.isSpectator()
                && bond.getUnderlyingPlayer() == null && !TrueTransfurCompat.isInstinctBody(bond)
                && ChangedSynergyConfig.COMMON.bondedKinConfinement.get()
                && ChangedSynergyGameRules.enabled(bond.level(), ChangedSynergyGameRules.BOND_SYSTEM)
                && Changed.config.server.isGrabEnabled.get() && LatexSocialMemory.isBonded(bond, owner)
                && !CreatureSocialProfile.isJuvenile(bond) && !CreatureSocialProfile.isGrabMechanicExcluded(bond)
                && (usesEvacuation(bond, owner) || ChangedSynergyConfig.COMMON.takeoverEnabled.get()
                    && TransfurConsciousness.isKept(owner));
    }

    public static boolean request(ChangedEntity bond, ServerPlayer owner) {
        long now = bond.level().getGameTime();
        var data = bond.getPersistentData();
        if (!canIntervene(bond, owner) || data.getLong(COOLDOWN) > now || data.getLong(UNTIL) > now) return false;
        data.putUUID(OWNER, owner.getUUID());
        data.putLong(UNTIL, now + 2400L);
        return true;
    }

    @Nullable
    public static ServerPlayer pendingOwner(ChangedEntity bond) {
        var data = bond.getPersistentData();
        if (!(bond.level() instanceof ServerLevel level) || !data.hasUUID(OWNER)) return null;
        var owner = level.getServer().getPlayerList().getPlayer(data.getUUID(OWNER));
        if (data.getLong(UNTIL) <= level.getGameTime() || !canIntervene(bond, owner)) {
            clear(bond);
            return null;
        }
        return owner;
    }

    public static void complete(ChangedEntity bond) {
        bond.getPersistentData().putLong(COOLDOWN, bond.level().getGameTime() + 24000L);
        clear(bond);
    }

    private static void clear(ChangedEntity bond) {
        bond.getPersistentData().remove(OWNER);
        bond.getPersistentData().remove(UNTIL);
    }

    public static void tick(ChangedEntity bond) {
        if (bond.tickCount % 5 != 0) return;
        ServerPlayer owner = pendingOwner(bond);
        if (owner == null || usesEvacuation(bond, owner) || TakeoverService.active(owner)
                || TrueTransfurCompat.isRebirthing(owner)) return;
        if (TakeoverService.beginBondedConfinement(bond, owner)) complete(bond);
        else bond.getNavigation().moveTo(owner, 1.0D);
    }
}
