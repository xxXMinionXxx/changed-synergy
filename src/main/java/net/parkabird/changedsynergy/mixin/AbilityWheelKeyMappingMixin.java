package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.init.ChangedKeyMappings;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.parkabird.changedsynergy.client.AbilityWheelInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Hold-wheel movement is the only gameplay input allowed through these screens. */
@Mixin(KeyMapping.class)
public abstract class AbilityWheelKeyMappingMixin {
    @Redirect(method = {"isDown", "m_90857_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;isConflictContextAndModifierActive()Z", remap = false), remap = false)
    private boolean changedSynergy$movement(KeyMapping key) {
        return key.isConflictContextAndModifierActive()
                || AbilityWheelInput.allowsMovement(key) && key.getKeyModifier().isActive(KeyConflictContext.IN_GAME);
    }

    @Redirect(method = {"setAll", "m_90829_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;setDown(Z)V", remap = false), remap = false, require = 0)
    private static void changedSynergy$noPolledReopen(KeyMapping key, boolean pressed) {
        if (AbilityWheelInput.ownsPress() && pressed && key == ChangedKeyMappings.SELECT_ABILITY
                || AbilityWheelInput.allowsMovement(key)) return;
        key.setDown(pressed);
    }

    @Redirect(method = {"setAll", "m_90829_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;m_7249_(Z)V", remap = false), remap = false, require = 0)
    private static void changedSynergy$noPolledReopenSrg(KeyMapping key, boolean pressed) {
        changedSynergy$noPolledReopen(key, pressed);
    }
}
