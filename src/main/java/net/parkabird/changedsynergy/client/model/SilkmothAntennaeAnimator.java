package net.parkabird.changedsynergy.client.model;

import net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.SpringType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SilkmothAntennaeAnimator<T extends ChangedEntity, M extends AdvancedHumanoidModel<T>>
        extends HumanoidAnimator.Animator<T, M> {
    private final ModelPart left;
    private final ModelPart right;
    private final ModelPart leftTip;
    private final ModelPart rightTip;
    private final Quaternionf leftRest;
    private final Quaternionf rightRest;

    public SilkmothAntennaeAnimator(ModelPart left, ModelPart right, ModelPart leftBind, ModelPart rightBind,
            ModelPart leftTip, ModelPart rightTip) {
        this.left = left;
        this.right = right;
        this.leftTip = leftTip;
        this.rightTip = rightTip;
        this.leftRest = new Quaternionf().rotationZYX(leftBind.zRot, leftBind.yRot, leftBind.xRot);
        this.rightRest = new Quaternionf().rotationZYX(rightBind.zRot, rightBind.yRot, rightBind.xRot);
    }

    @Override public HumanoidAnimator.AnimateStage preferredStage() {
        return HumanoidAnimator.AnimateStage.FINAL;
    }

    @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        // BeeAntennaeInitAnimator owns the root yaw/roll and its full turning response.
        float vertical = entity.getSimulatedSpring(SpringType.LIGHT_WEAK, SpringType.Direction.VERTICAL, core.partialTicks);
        float forwards = entity.getSimulatedSpring(SpringType.LIGHT_WEAK, SpringType.Direction.FORWARDS, core.partialTicks);
        float pitch = Mth.clamp(-vertical * 0.22F + forwards * 0.18F, -0.12F, 0.12F);
        left.xRot += pitch;
        right.xRot += pitch;
        float tipVertical = entity.getSimulatedSpring(SpringType.MODERATE_WEAK, SpringType.Direction.VERTICAL, core.partialTicks);
        float tipForwards = entity.getSimulatedSpring(SpringType.MODERATE_WEAK, SpringType.Direction.FORWARDS, core.partialTicks);
        float tipPitch = Mth.clamp(-tipVertical * 0.18F + tipForwards * 0.15F, -0.10F, 0.10F);
        float tipYaw = entity.getTailDragAmount(core.partialTicks) * 0.20F;
        float phase = ageInTicks * 0.05235988F - 0.6F;
        poseTip(leftTip, leftRest, tipPitch + Mth.sin(phase - 0.35F) * 0.015F,
            tipYaw + Mth.sin(phase + 0.7F) * 0.025F);
        poseTip(rightTip, rightRest, tipPitch + Mth.sin(phase + 0.45F) * 0.015F,
            tipYaw + Mth.sin(phase) * 0.025F);
    }

    // Both tips bend in head space despite the mirrored, diagonally rotated shafts.
    private static void poseTip(ModelPart tip, Quaternionf rest, float pitch, float yaw) {
        Vector3f angles = new Quaternionf(rest).conjugate()
            .mul(new Quaternionf().rotationYXZ(yaw, pitch, 0)).mul(rest)
            .getEulerAnglesZYX(new Vector3f());
        tip.xRot = angles.x;
        tip.yRot = angles.y;
        tip.zRot = angles.z;
    }
}
