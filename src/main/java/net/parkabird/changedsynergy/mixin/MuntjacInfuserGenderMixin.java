package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.entity.Gender;
import net.ltxprogrammer.changed.recipe.InfuserRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.parkabird.changedsynergy.init.ChangedSynergyTransfurVariants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the existing male form ID while using the native infuser's gender selector. */
@Mixin(value = InfuserRecipe.class, remap = false)
public abstract class MuntjacInfuserGenderMixin {
    @Shadow @Final public ResourceLocation form;
    @Shadow @Final public boolean gendered;

    @Inject(method = "processItem", at = @At("RETURN"))
    private void changedSynergy$muntjacGender(ItemStack input, Gender gender, CallbackInfoReturnable<ItemStack> cir) {
        if (gendered && form.equals(ChangedSynergyTransfurVariants.LATEX_MUNTJAC.getId()))
            cir.getReturnValue().getOrCreateTag().putString("form", (gender == Gender.FEMALE
                    ? ChangedSynergyTransfurVariants.LATEX_MUNTJAC_FEMALE.getId()
                    : ChangedSynergyTransfurVariants.LATEX_MUNTJAC.getId()).toString());
    }
}
