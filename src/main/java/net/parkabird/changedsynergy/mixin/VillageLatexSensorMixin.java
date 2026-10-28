package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import net.minecraft.world.entity.player.Player;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerHostilesSensor.class)
public abstract class VillageLatexSensorMixin {
    @Inject(method = {"isMatchingEntity", "m_142628_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void changedSynergy$peacefulLatex(LivingEntity villager, LivingEntity candidate,
            CallbackInfoReturnable<Boolean> callback) {
        if (ChangedSynergyConfig.COMMON.peacefulVillageRelations.get()
                && (candidate instanceof ChangedEntity || candidate instanceof Player player
                    && ProcessTransfur.isPlayerTransfurred(player))) callback.setReturnValue(false);
    }
}
