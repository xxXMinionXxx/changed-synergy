package net.parkabird.changedsynergy.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.ltxprogrammer.changed.client.renderer.animate.AnimatorPresets;
import net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.arm.ArmRideAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.arm.ArmSwimAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.arm.DoubleArmBobAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.ears.BeeAntennaeInitAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.upperbody.WolfHeadInitAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.upperbody.DoubleArmUpperBodyInitAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.upperbody.DoubleArmUpperBodyCrouchAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.upperbody.DoubleArmUpperBodyAttackAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.upperbody.DoubleArmUpperBodyStandAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.wing.BeeWingInitAnimator;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.client.renderer.model.DoubleArmedModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.HumanoidArm;

public class LatexSilkMothModel<T extends ChangedEntity> extends AdvancedHumanoidModel<T> implements DoubleArmedModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("changed_synergy", "latex_silk_moth"), "main");
	private final HumanoidAnimator<T, LatexSilkMothModel<T>> animator;
	private final ModelPart RightLeg;
	private final ModelPart RightLowerLeg;
	private final ModelPart RightFoot;
	private final ModelPart RightPad;
	private final ModelPart LeftLeg;
	private final ModelPart LeftLowerLeg;
	private final ModelPart LeftFoot;
	private final ModelPart LeftPad;
	private final ModelPart Head;
	private final ModelPart Hair;
	private final ModelPart RightAntennaMotion;
	private final ModelPart RightAntenna;
	private final ModelPart RightAntennaTipMotion;
	private final ModelPart LeftAntennaMotion;
	private final ModelPart LeftAntenna;
	private final ModelPart LeftAntennaTipMotion;
	private final ModelPart Torso;
	private final ModelPart RightWing;
	private final ModelPart RightWingPivot;
	private final ModelPart RightWing2;
	private final ModelPart RightWingPivot2;
	private final ModelPart LeftWing;
	private final ModelPart LeftWingPivot;
	private final ModelPart LeftWing2;
	private final ModelPart LeftWingPivot2;
	private final ModelPart Tail;
	private final ModelPart TailPrimary;
	private final ModelPart TailSecondary;
	private final ModelPart RightArm;
	private final ModelPart RightArm2;
	private final ModelPart LeftArm;
	private final ModelPart LeftArm2;

	public LatexSilkMothModel(ModelPart root) {
		super(root);
		this.RightLeg = root.getChild("RightLeg");
		this.RightLowerLeg = this.RightLeg.getChild("RightLowerLeg");
		this.RightFoot = this.RightLowerLeg.getChild("RightFoot");
		this.RightPad = this.RightFoot.getChild("RightPad");
		this.LeftLeg = root.getChild("LeftLeg");
		this.LeftLowerLeg = this.LeftLeg.getChild("LeftLowerLeg");
		this.LeftFoot = this.LeftLowerLeg.getChild("LeftFoot");
		this.LeftPad = this.LeftFoot.getChild("LeftPad");
		this.Head = root.getChild("Head");
		this.Hair = this.Head.getChild("Hair");
		this.RightAntennaMotion = this.Head.getChild("RightAntennaMotion");
		this.RightAntenna = this.RightAntennaMotion.getChild("RightAntenna");
		this.RightAntennaTipMotion = this.RightAntenna.getChild("RightAntennaTipMotion");
		this.LeftAntennaMotion = this.Head.getChild("LeftAntennaMotion");
		this.LeftAntenna = this.LeftAntennaMotion.getChild("LeftAntenna");
		this.LeftAntennaTipMotion = this.LeftAntenna.getChild("LeftAntennaTipMotion");
		this.Torso = root.getChild("Torso");
		this.RightWing = this.Torso.getChild("RightWing");
		this.RightWingPivot = this.RightWing.getChild("RightWingPivot");
		this.RightWing2 = this.Torso.getChild("RightWing2");
		this.RightWingPivot2 = this.RightWing2.getChild("RightWingPivot2");
		this.LeftWing = this.Torso.getChild("LeftWing");
		this.LeftWingPivot = this.LeftWing.getChild("LeftWingPivot");
		this.LeftWing2 = this.Torso.getChild("LeftWing2");
		this.LeftWingPivot2 = this.LeftWing2.getChild("LeftWingPivot2");
		this.Tail = this.Torso.getChild("Tail");
		this.TailPrimary = this.Tail.getChild("TailPrimary");
		this.TailSecondary = this.TailPrimary.getChild("TailSecondary");
		this.RightArm = root.getChild("RightArm");
		this.RightArm2 = root.getChild("RightArm2");
		this.LeftArm = root.getChild("LeftArm");
		this.LeftArm2 = root.getChild("LeftArm2");
		this.animator = HumanoidAnimator.<T, LatexSilkMothModel<T>>of(this)
            .hipOffset(-1.5F)
            .addPreset(AnimatorPresets.dragonBipedal(
                LeftLeg, LeftLowerLeg, LeftFoot, LeftPad, RightLeg, RightLowerLeg, RightFoot, RightPad))
            .setupHands(1, LeftArm, RightArm)
            .addAnimator(new SmoothGrabAnimator<>(Head, Torso, LeftArm, RightArm))
            .addAnimator(new SmoothGrabAnimator<>(Head, Torso, LeftArm2, RightArm2))
            .addAnimator(new DoubleArmUpperBodyInitAnimator<>(Head, Torso, LeftArm, RightArm, LeftArm2, RightArm2))
            .addAnimator(new DoubleArmUpperBodyCrouchAnimator<>(Head, Torso, LeftArm, RightArm, LeftArm2, RightArm2))
            .addAnimator(new DoubleArmUpperBodyAttackAnimator<>(Head, Torso, LeftArm, RightArm, LeftArm2, RightArm2))
            .addAnimator(new DoubleArmUpperBodyStandAnimator<>(Head, Torso, LeftArm, RightArm, LeftArm2, RightArm2))
            .addPreset(AnimatorPresets.dragonTail(Tail, List.of(TailPrimary, TailSecondary)))
            .addAnimator(new BeeAntennaeInitAnimator<>(LeftAntennaMotion, RightAntennaMotion))
            .addAnimator(new WolfHeadInitAnimator<>(Head))
            .addAnimator(new BeeWingInitAnimator<>(LeftWing, RightWing))
            .addAnimator(new ArmSwimAnimator<>(LeftArm, RightArm))
            .addAnimator(new DoubleArmBobAnimator<>(LeftArm, RightArm, LeftArm2, RightArm2))
            .addAnimator(new ArmRideAnimator<>(LeftArm, RightArm))
            .addAnimator(new SilkmothIdleAnimator<>(Head, Torso,
                LeftWing, RightWing, LeftWing2, RightWing2))
            .addAnimator(new SilkmothJumpAnimator<>(LeftWingPivot.getChild("LeftWingFlap"),
                RightWingPivot.getChild("RightWingFlap"), LeftWingPivot2.getChild("LeftWing2Flap"),
                RightWingPivot2.getChild("RightWing2Flap")))
            .addAnimator(new SilkmothAntennaeAnimator<>(LeftAntennaMotion, RightAntennaMotion, LeftAntenna, RightAntenna,
                LeftAntennaTipMotion, RightAntennaTipMotion));
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition RightLeg = partdefinition.addOrReplaceChild("RightLeg", CubeListBuilder.create(), PartPose.offset(-2.5F, 10.5F, 0.0F));

		PartDefinition RightThigh_r1 = RightLeg.addOrReplaceChild("RightThigh_r1", CubeListBuilder.create().texOffs(16, 46).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2182F, 0.0F, 0.0F));

		PartDefinition RightLowerLeg = RightLeg.addOrReplaceChild("RightLowerLeg", CubeListBuilder.create(), PartPose.offset(0.0F, 6.375F, -3.45F));

		PartDefinition RightCalf_r1 = RightLowerLeg.addOrReplaceChild("RightCalf_r1", CubeListBuilder.create().texOffs(52, 16).addBox(-1.99F, -0.125F, -2.9F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.125F, 1.95F, 0.8727F, 0.0F, 0.0F));

		PartDefinition RightFoot = RightLowerLeg.addOrReplaceChild("RightFoot", CubeListBuilder.create(), PartPose.offset(0.0F, 0.8F, 7.175F));

		PartDefinition RightAnkleFurCuff_r1 = RightFoot.addOrReplaceChild("RightAnkleFurCuff_r1", CubeListBuilder.create().texOffs(24, 104).addBox(-2.0F, -4.75F, -0.725F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.18F))
		.texOffs(56, 26).addBox(-2.0F, -8.45F, -0.725F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.005F)), PartPose.offsetAndRotation(0.0F, 7.075F, -4.975F, -0.3491F, 0.0F, 0.0F));

		PartDefinition RightPad = RightFoot.addOrReplaceChild("RightPad", CubeListBuilder.create().texOffs(0, 104).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 4.325F, -4.425F));

		PartDefinition LeftLeg = partdefinition.addOrReplaceChild("LeftLeg", CubeListBuilder.create(), PartPose.offset(2.5F, 10.5F, 0.0F));

		PartDefinition LeftThigh_r1 = LeftLeg.addOrReplaceChild("LeftThigh_r1", CubeListBuilder.create().texOffs(48, 46).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2182F, 0.0F, 0.0F));

		PartDefinition LeftLowerLeg = LeftLeg.addOrReplaceChild("LeftLowerLeg", CubeListBuilder.create(), PartPose.offset(0.0F, 6.375F, -3.45F));

		PartDefinition LeftCalf_r1 = LeftLowerLeg.addOrReplaceChild("LeftCalf_r1", CubeListBuilder.create().texOffs(32, 54).addBox(-2.01F, -0.125F, -2.9F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.125F, 1.95F, 0.8727F, 0.0F, 0.0F));

		PartDefinition LeftFoot = LeftLowerLeg.addOrReplaceChild("LeftFoot", CubeListBuilder.create(), PartPose.offset(0.0F, 0.8F, 7.175F));

		PartDefinition LeftAnkleFurCuff_r1 = LeftFoot.addOrReplaceChild("LeftAnkleFurCuff_r1", CubeListBuilder.create().texOffs(24, 104).addBox(-2.0F, -4.75F, -0.725F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.18F))
		.texOffs(16, 57).addBox(-2.0F, -8.45F, -0.725F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.005F)), PartPose.offsetAndRotation(0.0F, 7.075F, -4.975F, -0.3491F, 0.0F, 0.0F));

		PartDefinition LeftPad = LeftFoot.addOrReplaceChild("LeftPad", CubeListBuilder.create().texOffs(0, 104).mirror().addBox(-2.0F, 0.0F, -2.5F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 4.325F, -4.425F));

		PartDefinition Head = partdefinition.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(25, 16).addBox(-2.0F, -3.0F, -5.5F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(47, 16).addBox(-1.5F, -1.0F, -4.75F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, 0.0F));

		PartDefinition Hair = Head.addOrReplaceChild("Hair", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.2F))
		.texOffs(0, 32).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.3F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition RightAntennaMotion = Head.addOrReplaceChild("RightAntennaMotion", CubeListBuilder.create(), PartPose.offset(-1.7388F, -8.0F, -3.4538F));

		PartDefinition RightAntenna = RightAntennaMotion.addOrReplaceChild("RightAntenna", CubeListBuilder.create().texOffs(58, 96).addBox(-2.6546F, -1.5071F, -1.6512F, 6.35F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(88, 104).addBox(-2.3046F, -3.0071F, -1.1512F, 4.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.7812F, -1.0F, 2.1938F, -0.1745F, 0.5672F, 1.1345F));

		PartDefinition RightAntennaTipMotion = RightAntenna.addOrReplaceChild("RightAntennaTipMotion", CubeListBuilder.create(), PartPose.offset(-2.3046F, -1.0071F, -1.1512F));

		PartDefinition RightAntennaTip = RightAntennaTipMotion.addOrReplaceChild("RightAntennaTip", CubeListBuilder.create().texOffs(58, 96).addBox(-4.0F, -0.5F, -0.5F, 4.35F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(96, 104).addBox(-4.0F, -2.0F, 0.0F, 4.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.2094F, 0.0F));

		PartDefinition LeftAntennaMotion = Head.addOrReplaceChild("LeftAntennaMotion", CubeListBuilder.create(), PartPose.offset(1.7388F, -8.0F, -3.4538F));

		PartDefinition LeftAntenna = LeftAntennaMotion.addOrReplaceChild("LeftAntenna", CubeListBuilder.create().texOffs(58, 96).addBox(-3.6954F, -1.5071F, -1.6512F, 6.35F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(88, 104).mirror().addBox(-1.6954F, -3.0071F, -1.1512F, 4.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(1.7812F, -1.0F, 2.1938F, -0.1745F, -0.5672F, -1.1345F));

		PartDefinition LeftAntennaTipMotion = LeftAntenna.addOrReplaceChild("LeftAntennaTipMotion", CubeListBuilder.create(), PartPose.offset(2.3046F, -1.0071F, -1.1512F));

		PartDefinition LeftAntennaTip = LeftAntennaTipMotion.addOrReplaceChild("LeftAntennaTip", CubeListBuilder.create().texOffs(58, 96).addBox(-0.35F, -0.5F, -0.5F, 4.35F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(96, 104).mirror().addBox(0.0F, -2.0F, 0.0F, 4.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2094F, 0.0F));

		PartDefinition Torso = partdefinition.addOrReplaceChild("Torso", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(48, 38).addBox(-4.0F, 0.1F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.8F))
		.texOffs(32, 29).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 5.0F, 4.0F, new CubeDeformation(0.4F)), PartPose.offset(0.0F, -0.5F, 0.0F));

		PartDefinition RightWing = Torso.addOrReplaceChild("RightWing", CubeListBuilder.create(), PartPose.offset(-0.5F, 2.5F, 0.8F));

		PartDefinition RightWingPivot = RightWing.addOrReplaceChild("RightWingPivot", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.4F, 0.0F, 0.0F, 0.1309F, 0.2182F, -0.0436F));

		PartDefinition RightWingFlap = RightWingPivot.addOrReplaceChild("RightWingFlap", CubeListBuilder.create().texOffs(64, 52).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(56, 37).addBox(-2.5F, 10.0F, 0.5F, 4.0F, 1.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(48, 63).addBox(-3.5F, 1.0F, 0.5F, 5.0F, 9.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1157F, -0.8211F, 0.9366F, 0.2182F, -0.2182F, 0.2618F));

		PartDefinition RightWing2 = Torso.addOrReplaceChild("RightWing2", CubeListBuilder.create(), PartPose.offset(-0.5F, 4.5F, 1.0F));

		PartDefinition RightWingPivot2 = RightWing2.addOrReplaceChild("RightWingPivot2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1309F, 0.0F, 0.0F));

		PartDefinition RightWing2Flap = RightWingPivot2.addOrReplaceChild("RightWing2Flap", CubeListBuilder.create().texOffs(64, 54).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(58, 63).addBox(-3.5F, 1.0F, 0.5F, 5.0F, 7.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(64, 15).addBox(-2.5F, 7.986F, 0.5006F, 4.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1157F, -0.8211F, 0.9366F, 0.2182F, -0.2182F, 0.2618F));

		PartDefinition LeftWing = Torso.addOrReplaceChild("LeftWing", CubeListBuilder.create(), PartPose.offset(0.9F, 2.5F, 0.8F));

		PartDefinition LeftWingPivot = LeftWing.addOrReplaceChild("LeftWingPivot", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1309F, -0.2182F, 0.0436F));

		PartDefinition LeftWingFlap = LeftWingPivot.addOrReplaceChild("LeftWingFlap", CubeListBuilder.create().texOffs(56, 15).addBox(-1.5F, 10.0F, 0.5F, 4.0F, 1.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(0, 62).addBox(-1.5F, 1.0F, 0.5F, 5.0F, 9.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(40, 64).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.1157F, -0.8211F, 0.9366F, 0.2182F, 0.2182F, -0.2618F));

		PartDefinition LeftWing2 = Torso.addOrReplaceChild("LeftWing2", CubeListBuilder.create(), PartPose.offset(0.5F, 4.5F, 1.0F));

		PartDefinition LeftWingPivot2 = LeftWing2.addOrReplaceChild("LeftWingPivot2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1309F, 0.0F, 0.0F));

		PartDefinition LeftWing2Flap = LeftWingPivot2.addOrReplaceChild("LeftWing2Flap", CubeListBuilder.create().texOffs(30, 64).addBox(-1.5F, 1.0F, 0.5F, 5.0F, 7.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(64, 56).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(64, 37).addBox(-1.5F, 7.986F, 0.5006F, 4.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.1157F, -0.8211F, 0.9366F, 0.2182F, 0.2182F, -0.2618F));

		PartDefinition Tail = Torso.addOrReplaceChild("Tail", CubeListBuilder.create(), PartPose.offset(0.0F, 10.4F, -0.4F));

		PartDefinition TailPrimary = Tail.addOrReplaceChild("TailPrimary", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -0.4F, 0.3F, 0.1309F, 0.0F, 0.0F));

		PartDefinition Base_r1 = TailPrimary.addOrReplaceChild("Base_r1", CubeListBuilder.create().texOffs(56, 0).addBox(-2.0F, 0.75F, -1.5F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.4F, -0.3F, 1.0908F, 0.0F, 0.0F));

		PartDefinition TailSecondary = TailPrimary.addOrReplaceChild("TailSecondary", CubeListBuilder.create(), PartPose.offset(0.0F, 1.9F, 3.2F));

		PartDefinition Base_r2 = TailSecondary.addOrReplaceChild("Base_r2", CubeListBuilder.create().texOffs(32, 16).addBox(-2.5F, -0.45F, -2.0F, 5.0F, 8.0F, 5.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.0F, 0.65F, 0.8F, 1.3963F, 0.0F, 0.0F));

		PartDefinition RightArm = partdefinition.addOrReplaceChild("RightArm", CubeListBuilder.create().texOffs(0, 46).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(104, 112).addBox(-3.0F, 5.5F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.28F)), PartPose.offset(-5.0F, 1.5F, 0.0F));

		PartDefinition RightArm2 = partdefinition.addOrReplaceChild("RightArm2", CubeListBuilder.create().texOffs(0, 46).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(104, 112).addBox(-3.0F, 5.5F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.28F)), PartPose.offset(-5.0F, 5.5F, 0.0F));

		PartDefinition LeftArm = partdefinition.addOrReplaceChild("LeftArm", CubeListBuilder.create().texOffs(32, 38).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(104, 112).addBox(-1.0F, 5.5F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.28F)), PartPose.offset(5.0F, 1.5F, 0.0F));

		PartDefinition LeftArm2 = partdefinition.addOrReplaceChild("LeftArm2", CubeListBuilder.create().texOffs(32, 38).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(104, 112).addBox(-1.0F, 5.5F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.28F)), PartPose.offset(5.0F, 5.5F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}


	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		RightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		LeftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		Torso.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		RightArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		RightArm2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		LeftArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		LeftArm2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

    @Override public ModelPart getArm(HumanoidArm side) { return side == HumanoidArm.LEFT ? LeftArm : RightArm; }
    @Override public ModelPart getOtherArm(HumanoidArm side) { return side == HumanoidArm.LEFT ? LeftArm2 : RightArm2; }
    @Override public ModelPart getLeg(HumanoidArm side) { return side == HumanoidArm.LEFT ? LeftLeg : RightLeg; }
    @Override public ModelPart getHead() { return Head; }
    @Override public ModelPart getTorso() { return Torso; }
    @Override public HumanoidAnimator<T, LatexSilkMothModel<T>> getAnimator(T entity) { return animator; }
    @Override public void translateToUpperHand(T entity, HumanoidArm side, PoseStack poseStack) {
        getArm(side).translateAndRotate(poseStack);
        poseStack.translate(0.0, (animator.armLength - 12.0F) / 20.0, 0.0);
    }
    @Override public void translateToLowerHand(T entity, HumanoidArm side, PoseStack poseStack) {
        getOtherArm(side).translateAndRotate(poseStack);
        poseStack.translate(0.0, (animator.armLength - 12.0F) / 20.0, 0.0);
    }
}
