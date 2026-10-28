package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.entity.variant.TransfurVariantInstance;
import net.minecraft.nbt.CompoundTag;
import net.parkabird.changedsynergy.entity.LatexSwordfish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Update the old species default without replacing custom attributes or modifiers. */
@Mixin(value = TransfurVariantInstance.class, remap = false)
public abstract class SwordfishSavedSwimSpeedMixin {
    @ModifyVariable(method = "load", at = @At("HEAD"), argsOnly = true)
    private CompoundTag changedSynergy$updateSavedSwimSpeed(CompoundTag tag) {
        if (((TransfurVariantInstance<?>)(Object)this).getChangedEntity() instanceof LatexSwordfish
                && Math.abs(tag.getCompound("newAttributes").getDouble("forge:swim_speed") - 1.48D) < 0.00001D) {
            tag = tag.copy();
            tag.getCompound("newAttributes").putDouble("forge:swim_speed", LatexSwordfish.SWIM_SPEED);
        }
        return tag;
    }
}
