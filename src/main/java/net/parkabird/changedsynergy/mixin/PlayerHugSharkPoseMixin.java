package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.client.renderer.model.SharkModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.parkabird.changedsynergy.client.PlayerHugPoses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feral fish setup does not call AdvancedHumanoidModel.setupAnim. */
@Mixin(SharkModel.class)
public abstract class PlayerHugSharkPoseMixin {
    @Unique private boolean changedSynergy$hadHeldFish;
    @Inject(method = "setupAnim(Lnet/ltxprogrammer/changed/entity/ChangedEntity;FFFFF)V", at = @At("TAIL"))
    private void changedSynergy$relaxHeldTail(ChangedEntity creature, float walk, float speed, float age, float yaw, float pitch, CallbackInfo callback) {
        var grab = PlayerHugPoses.heldBy(PlayerHugPoses.physical(creature));
        boolean held = grab != null && net.parkabird.changedsynergy.ai.PlayerHugService.active(grab);
        if (held || changedSynergy$hadHeldFish) PlayerHugPoses.animateFish(creature, (SharkModel<?>)(Object)this, age);
        changedSynergy$hadHeldFish = held;
    }
}
