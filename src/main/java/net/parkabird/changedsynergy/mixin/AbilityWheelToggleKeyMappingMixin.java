package net.parkabird.changedsynergy.mixin;

import net.minecraft.client.ToggleKeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.parkabird.changedsynergy.client.AbilityWheelInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Forge's crouch/sprint mappings override both the read and toggle context checks. */
@Mixin(ToggleKeyMapping.class)
public abstract class AbilityWheelToggleKeyMappingMixin {
    @Redirect(method = {"isDown", "m_90857_", "setDown", "m_7249_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/ToggleKeyMapping;isConflictContextAndModifierActive()Z", remap = false), remap = false, require = 2)
    private boolean changedSynergy$movement(ToggleKeyMapping key) {
        return key.isConflictContextAndModifierActive()
                || AbilityWheelInput.allowsMovement(key) && key.getKeyModifier().isActive(KeyConflictContext.IN_GAME);
    }
}
