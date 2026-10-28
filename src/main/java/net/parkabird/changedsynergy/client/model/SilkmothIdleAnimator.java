package net.parkabird.changedsynergy.client.model;

import net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public final class SilkmothIdleAnimator<T extends ChangedEntity, M extends AdvancedHumanoidModel<T>>
        extends HumanoidAnimator.Animator<T, M> {
    private final ModelPart head;
    private final ModelPart torso;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftLowerWing;
    private final ModelPart rightLowerWing;

    public SilkmothIdleAnimator(ModelPart head, ModelPart torso, ModelPart leftWing, ModelPart rightWing,
            ModelPart leftLowerWing, ModelPart rightLowerWing) {
        this.head = head;
        this.torso = torso;
        this.leftWing = leftWing;
        this.rightWing = rightWing;
        this.leftLowerWing = leftLowerWing;
        this.rightLowerWing = rightLowerWing;
    }

    @Override public HumanoidAnimator.AnimateStage preferredStage() {
        return HumanoidAnimator.AnimateStage.FINAL;
    }

    @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        float idle = 1.0F - Mth.clamp(limbSwingAmount * 3.0F, 0.0F, 1.0F);
        float phase = ageInTicks * 0.05235988F;
        // Changed has already restored and posed the torso and head for this frame.
        torso.y -= Mth.sin(phase) * 0.12F * idle;
        head.xRot -= Mth.sin(phase) * 0.0113F * idle;
        addWingIdle(leftWing, rightWing, leftLowerWing, rightLowerWing, ageInTicks, idle);
    }

    // Upper roots are initialized by BeeWingInitAnimator each frame; lower roots are independent.
    static void addWingIdle(ModelPart left, ModelPart right, ModelPart leftLower, ModelPart rightLower,
            float age, float weight) {
        float phase = age * 0.05235988F;
        wing(left, phase, 1.0F, false, weight);
        wing(right, phase, -1.0F, false, weight);
        leftLower.resetPose();
        rightLower.resetPose();
        wing(leftLower, phase - 0.35F, 1.0F, true, weight);
        wing(rightLower, phase - 0.35F, -1.0F, true, weight);
    }

    private static void wing(ModelPart part, float phase, float side, boolean lower, float weight) {
        float radians = (float)(Math.PI / 180.0) * weight;
        rotate(part, -(lower ? 0.9F : 1.2F) * Mth.sin(phase + 0.4F) * radians,
            -side * (lower ? 2.8F : 4.0F) * Mth.sin(phase) * radians,
            side * (lower ? 0.6F : 0.8F) * Mth.sin(phase + 0.3F) * radians);
    }

    private static void rotate(ModelPart part, float x, float y, float z) {
        part.xRot += x;
        part.yRot += y;
        part.zRot += z;
    }

}
