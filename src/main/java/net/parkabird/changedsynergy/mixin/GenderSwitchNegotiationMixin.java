package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.ability.SwitchGenderAbility;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.parkabird.changedsynergy.ai.InvoluntaryTransfurNegotiation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Native gender switching changes the body, not the player's assimilation history. */
@Mixin(value = SwitchGenderAbility.class, remap = false)
public abstract class GenderSwitchNegotiationMixin {
    @Redirect(method = "lambda$startUsing$0", at = @At(value = "INVOKE",
            target = "Lnet/ltxprogrammer/changed/ability/IAbstractChangedEntity;replaceVariant(Lnet/ltxprogrammer/changed/entity/variant/TransfurVariant;)V"))
    private static void changedSynergy$keepNegotiation(IAbstractChangedEntity entity, TransfurVariant<?> opposite) {
        InvoluntaryTransfurNegotiation.switchGender(entity, opposite);
    }
}
