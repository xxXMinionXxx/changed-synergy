package net.parkabird.changedsynergy.mixin;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.parkabird.changedsynergy.ai.BondedSuitService;
import net.parkabird.changedsynergy.ai.TakeoverService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinAi.class)
public abstract class PiglinWrappedTargetMixin {
    @Inject(method = {"findNearestValidAttackTarget", "m_35000_"}, at = @At("RETURN"), cancellable = true, remap = false, require = 1)
    private static void changedSynergy$visibleWrappedTarget(Piglin piglin,
            CallbackInfoReturnable<Optional<? extends LivingEntity>> callback) {
        if (!(callback.getReturnValue().orElse(null) instanceof ServerPlayer player)) return;
        var carrier = TakeoverService.controllingCarrier(player);
        if (carrier == null) carrier = BondedSuitService.getWrappingPet(player);
        // The fight validity check must select the same body as StartAttacking's target event.
        if (carrier != null && carrier.isAlive() && !carrier.isRemoved())
            callback.setReturnValue(Optional.of(carrier));
    }
}
