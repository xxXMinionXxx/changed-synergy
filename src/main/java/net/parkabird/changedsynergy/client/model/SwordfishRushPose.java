package net.parkabird.changedsynergy.client.model;

import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/** Straightens the native swimming animation without replacing its anchor or resetting the rig. */
public final class SwordfishRushPose {
    private SwordfishRushPose() {}
    public static void apply(AdvancedHumanoidModel<?> model, float amount) {
        if (amount <= 0) return;
        straighten(model.getHead(), amount, -(float)Math.PI / 2, 0);
        straighten(model.getTorso(), amount, 0, 0);
        for (var side : HumanoidArm.values()) {
            straighten(model.getArm(side), amount, 0, side == HumanoidArm.LEFT ? -0.12F : 0.12F);
            straighten(model.getLeg(side), amount, -0.10471976F, side == HumanoidArm.LEFT ? -0.075F : 0.075F);
        }
        var tail = model.getTorso().getChild("Tail");
        straighten(tail, amount, -1.1780972F, 0);
        var joint = tail.getChild("TailPrimary");
        straighten(joint, amount, 0, 0);
        joint = joint.getChild("TailSecondary");
        straighten(joint, amount, 0, 0);
        if (joint.hasChild("TailTertiary")) straighten(joint.getChild("TailTertiary"), amount, 0, 0);
    }
    private static void straighten(ModelPart part, float amount, float pitch, float roll) {
        part.xRot = Mth.lerp(amount, part.xRot, pitch);
        part.yRot = Mth.lerp(amount, part.yRot, 0);
        part.zRot = Mth.lerp(amount, part.zRot, roll);
        var anchor = part.getInitialPose();
        part.x = Mth.lerp(amount, part.x, anchor.x);
        part.y = Mth.lerp(amount, part.y, anchor.y);
        part.z = Mth.lerp(amount, part.z, anchor.z);
    }
}
