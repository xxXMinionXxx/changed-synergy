package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.client.gui.AbilityRadialScreen;
import net.ltxprogrammer.changed.util.SingleRunnable;
import net.parkabird.changedsynergy.client.RadialWheelAnimations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** AbilityRadialScreen closes from inside handleClicked instead of its base. */
@Mixin(value = AbilityRadialScreen.class, remap = false)
public abstract class AbilityRadialScreenCloseMixin {
    @ModifyArg(method = "renderSectionForeground", at = @At(value = "INVOKE",
            target = "Lnet/ltxprogrammer/changed/client/AbilityRenderer;renderAndDecorateAbility(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/LivingEntity;Lnet/ltxprogrammer/changed/ability/AbstractAbilityInstance;IIIFZI)V",
            remap = false), index = 6, remap = false)
    private float changedSynergy$fadeAbilityIcon(float alpha) {
        return alpha * RadialWheelAnimations.layerAlpha(this);
    }

    @Redirect(
            method = "handleClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/ltxprogrammer/changed/util/SingleRunnable;run()V",
                    remap = false),
            remap = false)
    private void changedSynergy$animateAbilitySelectionClose(
            SingleRunnable close) {
        if (!RadialWheelAnimations.requestClose(this, close::run)) {
            close.run();
        }
    }
}
