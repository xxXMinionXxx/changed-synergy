package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.parkabird.changedsynergy.ai.CreatureMorphContinuity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Stasis, growth and absorption all replace NPCs through this Changed abstraction. */
@Mixin(targets = "net.ltxprogrammer.changed.ability.IAbstractChangedEntity$2", remap = false)
public abstract class CreatureVariantReplacementMixin {
    @Redirect(method = "replaceVariant", at = @At(value = "INVOKE",
            target = "Lnet/ltxprogrammer/changed/entity/ChangedEntity;copyTraitsFrom(Lnet/ltxprogrammer/changed/ability/IAbstractChangedEntity;)V"))
    private void changedSynergy$preserveIndividual(ChangedEntity replacement, IAbstractChangedEntity source) {
        boolean interruptedMeal=false;
        if (source.getEntity() instanceof ChangedEntity previous
                && net.parkabird.changedsynergy.ai.CompanionAutoEatGoal.active(previous)) {
            interruptedMeal=true;
            net.parkabird.changedsynergy.ai.CompanionAutoEatGoal.cleanup(previous);
            replacement.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,previous.getOffhandItem().copy());
        }
        replacement.copyTraitsFrom(source);
        if (source.getEntity() instanceof ChangedEntity previous) {
            CreatureMorphContinuity.transfer(previous, replacement);
            if (interruptedMeal) net.parkabird.changedsynergy.ai.CompanionAutoEatGoal.transferMealInventory(previous,replacement);
        }
    }
}
