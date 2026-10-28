package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers both player scare navigation and the species-only village reputation penalty. */
@Mixin(value = TransfurVariant.class, remap = false)
public abstract class VillagePlayerScareMixin {
    @Inject(method = "shouldScareVillager", at = @At("HEAD"), cancellable = true)
    private static void changedSynergy$peacefulVillage(ChangedEntity form, AbstractVillager villager,
            CallbackInfoReturnable<Boolean> callback) {
        if (ChangedSynergyConfig.COMMON.peacefulVillageRelations.get()) callback.setReturnValue(false);
    }
}
