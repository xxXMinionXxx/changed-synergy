package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.entity.beast.AbstractAquaticEntity;
import net.parkabird.changedsynergy.ai.CreaturePeerInteractionService;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Equal-priority idle depth adjustments yield while a social session owns movement. */
@Mixin(targets = {
        "net.ltxprogrammer.changed.entity.beast.AbstractAquaticEntity$SinkFromSurfaceGoal",
        "net.ltxprogrammer.changed.entity.beast.AbstractAquaticEntity$RiseToSurfaceGoal"})
public abstract class AquaticPeerIdleMixin {
    @Shadow(remap = false) @Final private AbstractAquaticEntity mob;

    @Inject(method = {"canUse", "m_8036_", "canContinueToUse", "m_8045_"},
            at = @At("HEAD"), cancellable = true, remap = false)
    private void changedSynergy$yieldDuringPlay(CallbackInfoReturnable<Boolean> callback) {
        if (CreaturePeerInteractionService.active(mob)) callback.setReturnValue(false);
    }
}
