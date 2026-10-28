package net.parkabird.changedsynergy.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerPlayer;
import net.parkabird.changedsynergy.ai.HumanBoundaryService;

/**
 * Some target-event combinations can clear a mob's revenge target between
 * HurtByTargetGoal.canUse and start. Vanilla's ally alert assumes it survived.
 */
@Mixin(HurtByTargetGoal.class)
public abstract class HurtByTargetGoalNullGuardMixin
        extends TargetGoal {
    protected HurtByTargetGoalNullGuardMixin(
            Mob mob,
            boolean mustSee) {
        super(mob, mustSee);
    }

    @Inject(method = {"canUse", "m_8036_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void changedSynergy$acceptedBoundary(CallbackInfoReturnable<Boolean> callback) {
        if (mob instanceof ChangedEntity creature && mob.getLastHurtByMob() instanceof ServerPlayer player
                && HumanBoundaryService.isBackingOff(creature, player)) callback.setReturnValue(false);
    }

    @Inject(
            method = {"alertOthers", "m_26047_"},
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void changedSynergy$skipAlertWithoutTarget(
            CallbackInfo callback) {
        if (mob.getTarget() == null) {
            callback.cancel();
        }
    }
}
