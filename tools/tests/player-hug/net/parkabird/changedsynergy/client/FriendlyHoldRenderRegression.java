package net.parkabird.changedsynergy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.client.renderer.model.DoubleArmedModel;
import net.ltxprogrammer.changed.client.renderer.model.TripleHeadedModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.LivingEntityDataExtension;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.registries.ForgeRegistries;
import net.parkabird.changedsynergy.ai.BondedSuitService;
import net.parkabird.changedsynergy.ai.PlayerHugAccess;

/** Exercise Changed's nested held renderer, which skips the usual living-render preparation. */
public final class FriendlyHoldRenderRegression {
    private static int checks;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void run(Minecraft mc) {
        var holder = creature(mc, "changed:dark_latex_wolf_male");
        BondedSuitService.ensureGrabAbility(holder);
        var grab = holder.getAbilityInstance(ChangedAbilities.GRAB_ENTITY_ABILITY.get());
        for (var id : new String[]{"changed:pure_white_latex_cerberus", "changed:latex_stiger",
                "changed:dark_latex_wolf_male", "changed:pure_white_latex_wolf", "changed:dark_latex_double_yufeng",
                "changed:dark_latex_wolf_pup", "changed:pure_white_latex_wolf_pup", "changed:gas_wolf_pup",
                "changed:headless_knight", "changed:milk_pudding", "changed:latex_shark_feral",
                "changed_synergy:latex_silk_moth", "changed_synergy:latex_swordfish",
                "changed_synergy:latex_muntjac", "changed_synergy:latex_muntjac_female", "changed_addon:latex_snep"}) {
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(ResourceLocation.parse(id))) continue;
            var held = creature(mc, id);
            var renderer = (LivingEntityRenderer)mc.getEntityRenderDispatcher().getRenderer(held);
            if (!(renderer.getModel() instanceof AdvancedHumanoidModel model)) continue;
            var animator = model.getAnimator(held);
            grab.grabbedEntity = held;
            grab.ticksGrabbed = 40;
            ((PlayerHugAccess)grab).changedSynergy$setPlayerHug(true);
            ((LivingEntityDataExtension)held).setGrabbedBy(holder);
            // A different creature of the same species was holding someone in the previous frame.
            animator.grabState = AdvancedHumanoidModel.GrabState.HOLD;
            animator.swimAmount = 1;
            animator.crouching = true;
            draw(mc, holder);
            boolean shared = mc.getEntityRenderDispatcher().getRenderer(holder) == renderer;
            check(animator.grabState == (shared ? AdvancedHumanoidModel.GrabState.HOLD : AdvancedHumanoidModel.GrabState.EMPTY),
                    id + " inherited another entity's HOLD state or failed to restore the holder");
            check(animator.swimAmount == 0 && !animator.crouching, id + " inherited another entity's swim/crouch state");
            check(Math.abs(animator.partialTicks - .5F) < .001F, id + " has a stale animation interpolation time");
            var head = model.getHead().storePose();
            var center = model instanceof TripleHeadedModel multi ? multi.getCenterHead().storePose() : head;
            var limbs = (java.util.List<net.minecraft.client.model.geom.ModelPart>)model.getRootLevelLimbs()
                    .flatMap(part -> ((net.minecraft.client.model.geom.ModelPart)part).getAllParts()).toList();
            var poses = limbs.stream().map(net.minecraft.client.model.geom.ModelPart::storePose).toList();
            for (int frame = 0; frame < 12; frame++) {
                animator.grabState = AdvancedHumanoidModel.GrabState.HOLD;
                animator.swimAmount = (frame % 2);
                animator.crouching = frame % 2 == 0;
                animator.partialTicks = frame / 12F;
                draw(mc, holder);
                for (int limb = 0; limb < limbs.size(); limb++)
                    check(same(poses.get(limb), limbs.get(limb).storePose()), id + " head/tail/limb changes with another entity's state");
            }
            check(same(head, model.getHead().storePose()), id + " head changes when rendering the same held frame repeatedly");
            if (model instanceof TripleHeadedModel multi)
                check(same(center, multi.getCenterHead().storePose()), id + " center head drifts between held frames");
            if (held instanceof net.ltxprogrammer.changed.entity.beast.TripleHeadedEntity heads) {
                heads.setLeftHeadYRot(-179); heads.setRightHeadYRot(-179);
                held.yBodyRot = held.yBodyRotO = 179;
                draw(mc, holder);
                float expectedLeft = net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(heads.getLeftHeadYRot(.5F) - 179), -90, 10) * net.minecraft.util.Mth.DEG_TO_RAD;
                float expectedRight = net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(heads.getRightHeadYRot(.5F) - 179), -10, 90) * net.minecraft.util.Mth.DEG_TO_RAD;
                check(Math.abs(model.getHead().yRot - expectedLeft) < .001F, "left independent head wraps across carrier rotation");
                check(Math.abs(((TripleHeadedModel)model).getOtherHead().yRot - expectedRight) < .001F, "right independent head wraps across carrier rotation");
            } else if (held instanceof net.ltxprogrammer.changed.entity.beast.DoubleHeadedEntity heads) {
                heads.setHead2YRot(-179);
                held.yBodyRot = held.yBodyRotO = 179;
                draw(mc, holder);
                float expected = net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(heads.getHead2YRot(.5F) - 179), -90, 90) * net.minecraft.util.Mth.DEG_TO_RAD;
                check(Math.abs(((net.ltxprogrammer.changed.client.renderer.model.DoubleHeadedModel)model).getOtherHead().yRot - expected) < .001F, "second independent head wraps across carrier rotation");
            }
            grab.grabbedEntity = null;
            ((LivingEntityDataExtension)held).setGrabbedBy(null);
            held.discard();
        }
        for (String id : new String[]{"changed:pure_white_latex_cerberus", "changed:latex_stiger", "changed_synergy:latex_silk_moth"}) {
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(ResourceLocation.parse(id))) continue;
            var multiHolder = creature(mc, id);
            BondedSuitService.ensureGrabAbility(multiHolder);
            var multiGrab = multiHolder.getAbilityInstance(ChangedAbilities.GRAB_ENTITY_ABILITY.get());
            multiGrab.grabbedEntity = creature(mc, "changed:dark_latex_wolf_male");
            multiGrab.ticksGrabbed = 40;
            ((PlayerHugAccess)multiGrab).changedSynergy$setPlayerHug(true);
            draw(mc, multiHolder);
            var multiModel = (DoubleArmedModel)((LivingEntityRenderer)mc.getEntityRenderDispatcher().getRenderer(multiHolder)).getModel();
            for (HumanoidArm side : HumanoidArm.values()) {
                var advanced = (AdvancedHumanoidModel)multiModel;
                check(Math.abs(advanced.getArm(side).xRot + 1.15F) < .001F, id + " primary arms missed friendly pose");
                check(Math.abs(multiModel.getOtherArm(side).xRot + 1F) < .001F, id + " extra arms kept the combat hold");
            }
            ((PlayerHugAccess)multiGrab).changedSynergy$setPlayerHug(false);
            multiHolder.tickCount += 4;
            draw(mc, multiHolder);
            check(Math.abs(multiModel.getOtherArm(HumanoidArm.RIGHT).yRot + .9424779F) < .001F,
                    id + " ordinary grab keeps the native combat pose");
            multiHolder.discard();
        }
        var released = creature(mc, "changed:pure_white_latex_cerberus");
        released.setPos(0, 100, 0);
        released.lerpTo(12, 100, 0, 0, 0, 3, false);
        grab.grabbedEntity = released;
        ((PlayerHugAccess)grab).changedSynergy$setPlayerHug(true);
        ((LivingEntityDataExtension)released).setGrabbedBy(holder);
        grab.releaseEntity(false);
        for (int tick = 0; tick < 3; tick++) released.aiStep();
        check(Math.abs(released.getX()) < .001, "release resumes a stale tracking interpolation: " + released.position());
        holder.discard();
        System.out.println("FRIENDLY_HOLD_RENDER_PASS: " + checks + " nested model-state checks");
    }

    private static boolean same(net.minecraft.client.model.geom.PartPose a, net.minecraft.client.model.geom.PartPose b) {
        return Math.abs(a.x-b.x)+Math.abs(a.y-b.y)+Math.abs(a.z-b.z)
                +Math.abs(a.xRot-b.xRot)+Math.abs(a.yRot-b.yRot)+Math.abs(a.zRot-b.zRot) < .001F;
    }

    private static ChangedEntity creature(Minecraft mc, String id) {
        var entity = (ChangedEntity)ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(id)).create(mc.level);
        entity.setNoAi(true);
        entity.tickCount = 40;
        return entity;
    }

    private static void draw(Minecraft mc, ChangedEntity holder) {
        var pose = new PoseStack();
        mc.getEntityRenderDispatcher().render(holder, 0, 0, 0, 0, .5F, pose, mc.renderBuffers().bufferSource(), 15728880);
        mc.renderBuffers().bufferSource().endBatch();
    }
}
