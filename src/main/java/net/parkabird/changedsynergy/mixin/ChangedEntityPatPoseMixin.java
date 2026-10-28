package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.parkabird.changedsynergy.client.PatAnimationClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses the same synchronized third-person pat stroke on Changed creature models. */
@Mixin(AdvancedHumanoidModel.class)
public abstract class ChangedEntityPatPoseMixin {
    @Unique private ModelPart changedSynergy$tiltedHead;
    @Unique private float changedSynergy$headRollBefore;
    @Unique private float changedSynergy$headRollApplied;

    @Inject(method = {
                "setupAnim(Lnet/ltxprogrammer/changed/entity/ChangedEntity;FFFFF)V",
                "m_6973_(Lnet/ltxprogrammer/changed/entity/ChangedEntity;FFFFF)V"
            }, at = @At("HEAD"))
    private void changedSynergy$restoreHeadRoll(ChangedEntity creature,
            float walkPosition, float walkSpeed, float age, float headYaw,
            float headPitch, CallbackInfo callback) {
        // Renderers share models; remove our previous tilt before native animations run.
        if (changedSynergy$tiltedHead != null) {
            if (Math.abs(changedSynergy$tiltedHead.zRot - changedSynergy$headRollApplied) < .0001F)
                changedSynergy$tiltedHead.zRot = changedSynergy$headRollBefore;
            changedSynergy$tiltedHead = null;
        }
    }

    @Inject(method = {
                "setupAnim(Lnet/ltxprogrammer/changed/entity/ChangedEntity;FFFFF)V",
                "m_6973_(Lnet/ltxprogrammer/changed/entity/ChangedEntity;FFFFF)V"
            },
            at = @At("TAIL"))
    private void changedSynergy$patRightArm(ChangedEntity creature,
            float walkPosition, float walkSpeed, float age, float headYaw,
            float headPitch, CallbackInfo callback) {
        AdvancedHumanoidModel<?> model =
                (AdvancedHumanoidModel<?>)(Object)this;
        // A transformed player renders through a separate ChangedEntity; the
        // synchronized pat session still belongs to the real player ID.
        var player = creature.getUnderlyingPlayer();
        PatAnimationClientState.applyModelRightArmPose(
                player == null ? creature.getId() : player.getId(),
                age - creature.tickCount,
                model);
        var head = model.getHead();
        float nativeRoll = head == null ? 0 : head.zRot;
        net.parkabird.changedsynergy.client.PlayerHugPoses.animate(creature, model, age);
        if (head != null && head.zRot != nativeRoll) {
            changedSynergy$tiltedHead = head;
            changedSynergy$headRollBefore = nativeRoll;
            changedSynergy$headRollApplied = head.zRot;
        }
    }
}
