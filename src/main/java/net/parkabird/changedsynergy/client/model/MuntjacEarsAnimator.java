package net.parkabird.changedsynergy.client.model;

import java.util.Map;
import java.util.WeakHashMap;
import net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.SpringType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Small, entity-local ear rebound; the upright bind pose remains on each EarPivot child. */
public final class MuntjacEarsAnimator<T extends ChangedEntity, M extends AdvancedHumanoidModel<T>>
        extends HumanoidAnimator.Animator<T, M> {
    private final ModelPart left, right;
    private final Map<T, TurnResponse> turns = new WeakHashMap<>();

    public MuntjacEarsAnimator(ModelPart left, ModelPart right) {
        this.left = left;
        this.right = right;
    }

    @Override public HumanoidAnimator.AnimateStage preferredStage() {
        return HumanoidAnimator.AnimateStage.FINAL;
    }

    @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        float partial = core.partialTicks;
        float vertical = entity.getSimulatedSpring(SpringType.LIGHT_NORMAL, SpringType.Direction.VERTICAL, partial);
        float forwards = entity.getSimulatedSpring(SpringType.LIGHT_NORMAL, SpringType.Direction.FORWARDS, partial);
        // World head yaw also catches a head-only turn while the body stays still.
        float turn = turns.computeIfAbsent(entity, ignored -> new TurnResponse())
                .sample(entity.tickCount, entity.yHeadRot, partial);
        poseEars(left, right, vertical, forwards, turn);
    }

    static void poseEars(ModelPart left, ModelPart right, float vertical, float forwards, float turn) {
        left.resetPose();
        right.resetPose();
        float pitch = Mth.clamp(-vertical * 0.16F + forwards * 0.05F, -0.10472F, 0.10472F);
        float yaw = Mth.clamp(turn * 0.40F, -0.08727F, 0.08727F);
        float roll = Mth.clamp(vertical * 0.05F, -0.03491F, 0.03491F);
        left.xRot += pitch;
        right.xRot += pitch;
        left.yRot += yaw;
        right.yRot += yaw;
        left.zRot += roll;
        right.zRot -= roll;
    }

    static final class TurnResponse {
        private SpringType.Simulator spring = new SpringType.Simulator(SpringType.LIGHT_NORMAL);
        private int lastTick = Integer.MIN_VALUE;
        private float lastYaw;

        float sample(int tick, float headYaw, float partial) {
            if (lastTick == Integer.MIN_VALUE || tick < lastTick || tick - lastTick > 10) {
                spring = new SpringType.Simulator(SpringType.LIGHT_NORMAL);
                lastTick = tick;
                lastYaw = headYaw;
            } else if (tick != lastTick) {
                for (int skipped = 1; skipped < tick - lastTick; skipped++) spring.tick(0.0F);
                float delta = Mth.wrapDegrees(headYaw - lastYaw) * Mth.DEG_TO_RAD;
                spring.tick(-Mth.clamp(delta * 0.55F, -0.35F, 0.35F));
                lastTick = tick;
                lastYaw = headYaw;
            }
            return spring.getSpring(Mth.clamp(partial, 0.0F, 1.0F));
        }
    }
}
