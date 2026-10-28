package net.parkabird.changedsynergy.client.model;

import net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.parkabird.changedsynergy.entity.LatexSilkMoth;

public final class SilkmothJumpAnimator<T extends ChangedEntity, M extends AdvancedHumanoidModel<T>>
        extends HumanoidAnimator.Animator<T, M> {
    private final ModelPart left, right, leftLower, rightLower;

    public SilkmothJumpAnimator(ModelPart left, ModelPart right, ModelPart leftLower, ModelPart rightLower) {
        this.left = left;
        this.right = right;
        this.leftLower = leftLower;
        this.rightLower = rightLower;
    }

    @Override public HumanoidAnimator.AnimateStage preferredStage() {
        return HumanoidAnimator.AnimateStage.FINAL;
    }

    @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        float elapsed = entity instanceof LatexSilkMoth moth
                ? moth.getWingFlapTime(core.partialTicks) : Float.POSITIVE_INFINITY;
        flap(left, elapsed, 1, 1);
        flap(right, elapsed, -1, 1);
        flap(leftLower, elapsed - .75F, 1, .8F);
        flap(rightLower, elapsed - .75F, -1, .8F);
    }

    private static void flap(ModelPart hinge, float elapsed, float side, float scale) {
        hinge.resetPose();
        if (elapsed <= 0 || elapsed >= 8) return;
        float envelope = Mth.sin(elapsed / 8 * Mth.PI);
        // The hinge is at the top end of the short stem; the whole wing lifts from that attachment.
        hinge.zRot -= side * 1.31F * envelope * envelope * scale;
    }
}
