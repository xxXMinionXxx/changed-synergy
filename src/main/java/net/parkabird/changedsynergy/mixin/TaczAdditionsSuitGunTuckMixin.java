package net.parkabird.changedsynergy.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.parkabird.changedsynergy.ai.BondedSuitService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A worn wrapper is not an obstruction in TaCZ Additions' muzzle ray. */
@Pseudo
@Mixin(targets = "com.raiiiden.taczadditions.util.GunTuckMath", remap = false)
public abstract class TaczAdditionsSuitGunTuckMixin {
    @Inject(method = "lambda$calculateTarget$0", at = @At("HEAD"),
            cancellable = true, require = 0, remap = false)
    private static void changedSynergy$ignoreWornCreature(
            LivingEntity shooter, Entity candidate,
            CallbackInfoReturnable<Boolean> result) {
        if (!(shooter instanceof Player)) return;
        if (candidate instanceof ChangedEntity carrier && BondedSuitService.getSuitController(carrier) == shooter) {
            result.setReturnValue(false);
        }
    }
}
