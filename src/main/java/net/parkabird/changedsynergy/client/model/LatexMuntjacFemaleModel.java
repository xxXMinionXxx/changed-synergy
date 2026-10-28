package net.parkabird.changedsynergy.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.ltxprogrammer.changed.client.animations.Limb;
import net.ltxprogrammer.changed.client.tfanimations.HelperModel;
import net.ltxprogrammer.changed.client.tfanimations.TransfurHelper;
import net.ltxprogrammer.changed.client.renderer.animate.AnimatorPresets;
import net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

public final class LatexMuntjacFemaleModel<T extends ChangedEntity> extends AdvancedHumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("changed_synergy", "latex_muntjac_female"), "main");
    private final ModelPart root, head, torso, leftArm, rightArm, leftLeg, rightLeg;
    private final HumanoidAnimator<T, LatexMuntjacFemaleModel<T>> animator;

    public LatexMuntjacFemaleModel(ModelPart root) {
        super(root);
        this.root = root;
        head = root.getChild("Head");
        torso = root.getChild("Torso");
        leftArm = root.getChild("LeftArm");
        rightArm = root.getChild("RightArm");
        leftLeg = root.getChild("LeftLeg");
        rightLeg = root.getChild("RightLeg");
        ModelPart leftLower = leftLeg.getChild("LeftLowerLeg"), rightLower = rightLeg.getChild("RightLowerLeg");
        ModelPart leftFoot = leftLower.getChild("LeftFoot"), rightFoot = rightLower.getChild("RightFoot");
        ModelPart tail = torso.getChild("Tail");
        animator = HumanoidAnimator.<T, LatexMuntjacFemaleModel<T>>of(this).hipOffset(-1.5F)
                .addPreset(AnimatorPresets.deerLike(head, head.getChild("LeftEar"), head.getChild("RightEar"),
                        torso, leftArm, rightArm, tail, List.of(tail.getChild("TailPrimary")),
                        leftLeg, leftLower, leftFoot, leftFoot.getChild("LeftPad"),
                        rightLeg, rightLower, rightFoot, rightFoot.getChild("RightPad")))
                .addAnimator(new MuntjacEarsAnimator<>(head.getChild("LeftEar"), head.getChild("RightEar")));
        SmoothGrabAnimator.install(animator, head, torso, leftArm, rightArm);
    }

    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition RightLeg = partdefinition.addOrReplaceChild("RightLeg", CubeListBuilder.create(), PartPose.offset(-2.5F, 10.5F, 0.0F));

		PartDefinition RightThigh_r1 = RightLeg.addOrReplaceChild("RightThigh_r1", CubeListBuilder.create().texOffs(32, 44).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2182F, 0.0F, 0.0F));

		PartDefinition RightLowerLeg = RightLeg.addOrReplaceChild("RightLowerLeg", CubeListBuilder.create(), PartPose.offset(0.0F, 6.375F, -3.45F));

		PartDefinition RightCalf_r1 = RightLowerLeg.addOrReplaceChild("RightCalf_r1", CubeListBuilder.create().texOffs(48, 22).addBox(-1.99F, -0.125F, -2.9F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.125F, 1.95F, 0.8727F, 0.0F, 0.0F));

		PartDefinition RightFoot = RightLowerLeg.addOrReplaceChild("RightFoot", CubeListBuilder.create(), PartPose.offset(0.0F, 0.8F, 7.175F));

		PartDefinition RightArch_r1 = RightFoot.addOrReplaceChild("RightArch_r1", CubeListBuilder.create().texOffs(48, 46).addBox(-2.0F, -8.45F, -0.725F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.005F)), PartPose.offsetAndRotation(0.0F, 7.075F, -4.975F, -0.3491F, 0.0F, 0.0F));

		PartDefinition RightPad = RightFoot.addOrReplaceChild("RightPad", CubeListBuilder.create().texOffs(47, 39).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 4.325F, -4.425F));

		PartDefinition LeftLeg = partdefinition.addOrReplaceChild("LeftLeg", CubeListBuilder.create(), PartPose.offset(2.5F, 10.5F, 0.0F));

		PartDefinition LeftThigh_r1 = LeftLeg.addOrReplaceChild("LeftThigh_r1", CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2182F, 0.0F, 0.0F));

		PartDefinition LeftLowerLeg = LeftLeg.addOrReplaceChild("LeftLowerLeg", CubeListBuilder.create(), PartPose.offset(0.0F, 6.375F, -3.45F));

		PartDefinition LeftCalf_r1 = LeftLowerLeg.addOrReplaceChild("LeftCalf_r1", CubeListBuilder.create().texOffs(48, 0).addBox(-2.01F, -0.125F, -2.9F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.125F, 1.95F, 0.8727F, 0.0F, 0.0F));

		PartDefinition LeftFoot = LeftLowerLeg.addOrReplaceChild("LeftFoot", CubeListBuilder.create(), PartPose.offset(0.0F, 0.8F, 7.175F));

		PartDefinition LeftArch_r1 = LeftFoot.addOrReplaceChild("LeftArch_r1", CubeListBuilder.create().texOffs(29, 55).addBox(-2.0F, -8.45F, -0.725F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.005F)), PartPose.offsetAndRotation(0.0F, 7.075F, -4.975F, -0.3491F, 0.0F, 0.0F));

		PartDefinition LeftPad = LeftFoot.addOrReplaceChild("LeftPad", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 4.325F, -4.425F));

		PartDefinition Head = partdefinition.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(52, 32).addBox(-2.0F, -3.0F, -6.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 16).addBox(-1.5F, -1.0F, -5.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, 0.0F));

		PartDefinition Nose_r1 = Head.addOrReplaceChild("Nose_r1", CubeListBuilder.create().texOffs(41, 6).addBox(-1.0F, -29.625F, -0.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 26.0F, 0.0F, 0.1745F, 0.0F, 0.0F));

		PartDefinition Hair = Head.addOrReplaceChild("Hair", CubeListBuilder.create().texOffs(64, 48).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.2F))
		.texOffs(96, 48).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 9.0F, 8.0F, new CubeDeformation(0.35F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition RightEar = Head.addOrReplaceChild("RightEar", CubeListBuilder.create(), PartPose.offset(-3.3F, -6.85F, 0.5F));

		PartDefinition RightEarPivot = RightEar.addOrReplaceChild("RightEarPivot", CubeListBuilder.create().texOffs(72, 0).addBox(-1.6F, -4.55F, -0.1F, 3.2F, 4.8F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(81, 0).addBox(-1.2F, -6.05F, -0.05F, 2.4F, 1.8F, 0.9F, new CubeDeformation(0.0F))
		.texOffs(88, 0).addBox(-1.0F, -5.25F, -0.13F, 2.0F, 4.5F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(72, 24).addBox(-0.6F, -6.75F, 0.0F, 1.2F, 1.0F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1396F, -0.1396F, -0.2443F));

		PartDefinition LeftEar = Head.addOrReplaceChild("LeftEar", CubeListBuilder.create(), PartPose.offset(3.3F, -6.85F, 0.5F));

		PartDefinition LeftEarPivot = LeftEar.addOrReplaceChild("LeftEarPivot", CubeListBuilder.create().texOffs(117, 0).addBox(-1.6F, -4.55F, -0.1F, 3.2F, 4.8F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(72, 8).addBox(-1.2F, -6.05F, -0.05F, 2.4F, 1.8F, 0.9F, new CubeDeformation(0.0F))
		.texOffs(79, 8).addBox(-1.0F, -5.25F, -0.13F, 2.0F, 4.5F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(78, 24).addBox(-0.6F, -6.75F, 0.0F, 1.2F, 1.0F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1396F, 0.1396F, 0.2443F));

		PartDefinition Torso = partdefinition.addOrReplaceChild("Torso", CubeListBuilder.create().texOffs(28, 28).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, 0.0F));

		PartDefinition Tail = Torso.addOrReplaceChild("Tail", CubeListBuilder.create(), PartPose.offset(0.0F, 10.5F, 0.0F));

		PartDefinition TailPrimary = Tail.addOrReplaceChild("TailPrimary", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Base_r1 = TailPrimary.addOrReplaceChild("Base_r1", CubeListBuilder.create().texOffs(48, 10).addBox(-1.5F, 1.1F, -0.6F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 1.1781F, 0.0F, 0.0F));

		PartDefinition Base_r2 = TailPrimary.addOrReplaceChild("Base_r2", CubeListBuilder.create().texOffs(16, 32).addBox(-1.5F, -2.425F, -0.35F, 3.0F, 4.0F, 3.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, 2.5F, 6.0F, 1.4835F, 0.0F, 0.0F));

		PartDefinition Plantoids = Torso.addOrReplaceChild("Plantoids", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -2.0F));

		PartDefinition Center_r1 = Plantoids.addOrReplaceChild("Center_r1", CubeListBuilder.create().texOffs(28, 80).addBox(-0.5F, -1.5F, -0.1F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.192F, 0.0F, 0.0F));

		PartDefinition LeftPlantoid_r1 = Plantoids.addOrReplaceChild("LeftPlantoid_r1", CubeListBuilder.create().texOffs(0, 80).addBox(0.25F, -1.9F, -0.6F, 4.0F, 4.0F, 2.0F, new CubeDeformation(-0.03F))
		.texOffs(14, 80).addBox(-4.25F, -1.9F, -0.6F, 4.0F, 4.0F, 2.0F, new CubeDeformation(-0.03F)), PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.2793F, 0.0F, 0.0F));

		PartDefinition RightArm = partdefinition.addOrReplaceChild("RightArm", CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 1.5F, 0.0F));

		PartDefinition LeftArm = partdefinition.addOrReplaceChild("LeftArm", CubeListBuilder.create().texOffs(16, 40).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, 1.5F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

    @Override public HelperModel getTransfurHelperModel(Limb limb) {
        return limb == Limb.TORSO ? TransfurHelper.getFeminineTorsoAlt() : super.getTransfurHelperModel(limb);
    }

    @Override public void setupAnim(T entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        super.setupAnim(entity, swing, amount, age, yaw, pitch);
    }
    @Override public ModelPart getArm(HumanoidArm side) { return side == HumanoidArm.LEFT ? leftArm : rightArm; }
    @Override public ModelPart getLeg(HumanoidArm side) { return side == HumanoidArm.LEFT ? leftLeg : rightLeg; }
    @Override public ModelPart getHead() { return head; }
    @Override public ModelPart getTorso() { return torso; }
    @Override public HumanoidAnimator<T, LatexMuntjacFemaleModel<T>> getAnimator(T entity) { return animator; }
    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        root.render(pose, buffer, light, overlay, red, green, blue, alpha);
    }
}
