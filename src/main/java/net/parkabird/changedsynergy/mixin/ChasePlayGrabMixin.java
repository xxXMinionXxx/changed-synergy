package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.ability.GrabEntityAbilityInstance;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.parkabird.changedsynergy.ai.CreaturePeerInteractionService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Games cannot be promoted to suits or saved as an orphaned native grab. */
@Mixin(value = GrabEntityAbilityInstance.class, remap = false)
public abstract class ChasePlayGrabMixin {
    @Inject(method = "suitEntity", at = @At("HEAD"), cancellable = true)
    private void changedSynergy$noPlaySuit(LivingEntity target, CallbackInfoReturnable<Boolean> callback) {
        var grab = (GrabEntityAbilityInstance)(Object)this;
        if (grab.entity.getLevel().isClientSide()) return;
        if (grab.entity.getEntity() instanceof ChangedEntity carrier && CreaturePeerInteractionService.isPlayHold(carrier, target)) {
            callback.setReturnValue(false);
        } else {
            // Real hostile contact interrupts play; invitations must not grant combat immunity.
            CreaturePeerInteractionService.cancel(grab.entity.getEntity());
            CreaturePeerInteractionService.cancel(target);
        }
    }

    @Inject(method = "saveData", at = @At("TAIL"))
    private void changedSynergy$noSavedGame(CompoundTag tag, CallbackInfo callback) {
        var grab = (GrabEntityAbilityInstance)(Object)this;
        if (grab.entity.getEntity() instanceof ChangedEntity carrier && grab.grabbedEntity != null
                && CreaturePeerInteractionService.isPlayHold(carrier, grab.grabbedEntity)) {
            tag.remove("GrabbedEntity");
            tag.putBoolean("GrabbedHasControl", false);
            tag.putBoolean("Suited", false);
            tag.putFloat("GrabStrength", 0);
            tag.putFloat("SuitTransition", 0);
            tag.putInt("TicksGrabbed", 0);
        }
    }
}
