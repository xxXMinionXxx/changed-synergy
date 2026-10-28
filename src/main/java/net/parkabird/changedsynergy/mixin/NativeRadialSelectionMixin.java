package net.parkabird.changedsynergy.mixin;

import java.util.Optional;
import net.ltxprogrammer.changed.client.gui.VariantRadialScreen;
import net.parkabird.changedsynergy.client.RadialWheelAnimations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Native frames use visible slots; input and selected abilities use absolute action indices. */
@Mixin(value = VariantRadialScreen.class, remap = false)
public abstract class NativeRadialSelectionMixin {
    @Redirect(method = "renderSectionBackground", at = @At(value = "INVOKE",
            target = "Lnet/ltxprogrammer/changed/client/gui/VariantRadialScreen;getSectionAt(II)Ljava/util/Optional;", remap = false), remap = false)
    private Optional<Integer> changedSynergy$hoveredSlot(VariantRadialScreen<?> screen, int x, int y) {
        return screen.getSectionAt(x, y).map(index -> index - RadialWheelAnimations.viewOffset(screen));
    }

    @ModifyArg(method = "getTextureForSection", at = @At(value = "INVOKE",
            target = "Lnet/ltxprogrammer/changed/client/gui/VariantRadialScreen;isSelected(I)Z", remap = false), index = 0, remap = false)
    private int changedSynergy$selectedAction(int slot) {
        return slot + RadialWheelAnimations.viewOffset(this);
    }
}
