package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.world.entity.Mob;
import net.parkabird.changedsynergy.ai.BondedSuitService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The controlled suit must leave nearby item pickups to its player. */
@Mixin(Mob.class)
public abstract class PlayerControlledSuitMobMixin {
    @Group(name = "suitLoot", min = 1, max = 1)
    @Redirect(method = {"aiStep", "m_8107_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;canPickUpLoot()Z"), remap = false, require = 0)
    private boolean changedSynergy$suitLoot(Mob mob) {
        return !(mob instanceof ChangedEntity suit && BondedSuitService.getSuitController(suit) != null)
                && !net.parkabird.changedsynergy.ai.PlayerHugService.held(mob)
                && mob.canPickUpLoot();
    }

    @Group(name = "suitLoot", min = 1, max = 1)
    @Redirect(method = {"aiStep", "m_8107_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;m_21531_()Z"), remap = false, require = 0)
    private boolean changedSynergy$suitLootProduction(Mob mob) {
        return changedSynergy$suitLoot(mob);
    }

    @Inject(method = {"playAmbientSound", "m_8032_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void changedSynergy$suitAmbient(CallbackInfo callback) {
        if ((Object)this instanceof ChangedEntity suit && BondedSuitService.getSuitController(suit) != null)
            callback.cancel();
    }
}
