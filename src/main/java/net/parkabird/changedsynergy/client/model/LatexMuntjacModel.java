package net.parkabird.changedsynergy.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
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

public final class LatexMuntjacModel<T extends ChangedEntity> extends AdvancedHumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("changed_synergy", "latex_muntjac"), "main");
    private final ModelPart root, head, torso, leftArm, rightArm, leftLeg, rightLeg;
    private final HumanoidAnimator<T, LatexMuntjacModel<T>> animator;

    public LatexMuntjacModel(ModelPart root) {
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
        animator = HumanoidAnimator.<T, LatexMuntjacModel<T>>of(this).hipOffset(-1.5F)
                .addPreset(AnimatorPresets.deerLike(head, head.getChild("LeftEar"), head.getChild("RightEar"),
                        torso, leftArm, rightArm, tail, List.of(tail.getChild("TailPrimary")),
                        leftLeg, leftLower, leftFoot, leftFoot.getChild("LeftPad"),
                        rightLeg, rightLower, rightFoot, rightFoot.getChild("RightPad")))
                .addAnimator(new MuntjacEarsAnimator<>(head.getChild("LeftEar"), head.getChild("RightEar")));
        SmoothGrabAnimator.install(animator, head, torso, leftArm, rightArm);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition RightLeg = root.addOrReplaceChild("RightLeg", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.5F, 10.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        RightLeg.addOrReplaceChild("cube_1", CubeListBuilder.create().texOffs(32, 44).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.218166F, 0.0F, 0.0F));
        PartDefinition RightLowerLeg = RightLeg.addOrReplaceChild("RightLowerLeg", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 6.375F, -3.45F, 0.0F, 0.0F, 0.0F));
        RightLowerLeg.addOrReplaceChild("cube_2", CubeListBuilder.create().texOffs(48, 22).addBox(-1.99F, -0.125F, -2.9F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.125F, 1.95F, 0.872665F, 0.0F, 0.0F));
        PartDefinition RightFoot = RightLowerLeg.addOrReplaceChild("RightFoot", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.8F, 7.175F, 0.0F, 0.0F, 0.0F));
        RightFoot.addOrReplaceChild("cube_3", CubeListBuilder.create().texOffs(48, 46).addBox(-2.0F, -8.45F, -0.725F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.005F)), PartPose.offsetAndRotation(0.0F, 7.075F, -4.975F, -0.349066F, 0.0F, 0.0F));
        PartDefinition RightPad = RightFoot.addOrReplaceChild("RightPad", CubeListBuilder.create().texOffs(47, 39).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.325F, -4.425F, 0.0F, 0.0F, 0.0F));
        PartDefinition LeftLeg = root.addOrReplaceChild("LeftLeg", CubeListBuilder.create(), PartPose.offsetAndRotation(2.5F, 10.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        LeftLeg.addOrReplaceChild("cube_4", CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.218166F, 0.0F, 0.0F));
        PartDefinition LeftLowerLeg = LeftLeg.addOrReplaceChild("LeftLowerLeg", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 6.375F, -3.45F, 0.0F, 0.0F, 0.0F));
        LeftLowerLeg.addOrReplaceChild("cube_5", CubeListBuilder.create().texOffs(48, 0).addBox(-2.01F, -0.125F, -2.9F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.125F, 1.95F, 0.872665F, 0.0F, 0.0F));
        PartDefinition LeftFoot = LeftLowerLeg.addOrReplaceChild("LeftFoot", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.8F, 7.175F, 0.0F, 0.0F, 0.0F));
        LeftFoot.addOrReplaceChild("cube_6", CubeListBuilder.create().texOffs(29, 55).addBox(-2.0F, -8.45F, -0.725F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.005F)), PartPose.offsetAndRotation(0.0F, 7.075F, -4.975F, -0.349066F, 0.0F, 0.0F));
        PartDefinition LeftPad = LeftFoot.addOrReplaceChild("LeftPad", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.325F, -4.425F, 0.0F, 0.0F, 0.0F));
        PartDefinition Head = root.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(52, 32).addBox(-2.0F, -3.0F, -6.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 16).addBox(-1.5F, -1.0F, -5.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(110, 0).addBox(-1.9F, -1.7F, -6.3F, 0.7F, 2.3F, 0.7F, new CubeDeformation(0.0F)).texOffs(114, 0).addBox(-1.55F, 0.4F, -6.1F, 0.45F, 1.1F, 0.45F, new CubeDeformation(0.0F)).texOffs(101, 8).addBox(1.2F, -1.7F, -6.3F, 0.7F, 2.3F, 0.7F, new CubeDeformation(0.0F)).texOffs(105, 8).addBox(1.1F, 0.4F, -6.1F, 0.45F, 1.1F, 0.45F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        Head.addOrReplaceChild("cube_7", CubeListBuilder.create().texOffs(41, 6).addBox(-1.0F, -29.625F, -0.95F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 26.0F, 0.0F, 0.174533F, 0.0F, 0.0F));
        PartDefinition Hair = Head.addOrReplaceChild("Hair", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.2F)).texOffs(24, 8).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.3F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition RightEar = Head.addOrReplaceChild("RightEar", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.3F, -6.85F, 0.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition RightEarPivot = RightEar.addOrReplaceChild("RightEarPivot", CubeListBuilder.create().texOffs(72, 0).addBox(-1.6F, -4.55F, -0.1F, 3.2F, 4.8F, 1.0F, new CubeDeformation(0.0F)).texOffs(81, 0).addBox(-1.2F, -6.05F, -0.05F, 2.4F, 1.8F, 0.9F, new CubeDeformation(0.0F)).texOffs(88, 0).addBox(-1.0F, -5.25F, -0.13F, 2.0F, 4.5F, 0.0F, new CubeDeformation(0.0F)).texOffs(72, 24).addBox(-0.6F, -6.75F, 0.0F, 1.2F, 1.0F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.139626F, -0.139626F, -0.244346F));
        PartDefinition LeftEar = Head.addOrReplaceChild("LeftEar", CubeListBuilder.create(), PartPose.offsetAndRotation(3.3F, -6.85F, 0.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition LeftEarPivot = LeftEar.addOrReplaceChild("LeftEarPivot", CubeListBuilder.create().texOffs(117, 0).addBox(-1.6F, -4.55F, -0.1F, 3.2F, 4.8F, 1.0F, new CubeDeformation(0.0F)).texOffs(72, 8).addBox(-1.2F, -6.05F, -0.05F, 2.4F, 1.8F, 0.9F, new CubeDeformation(0.0F)).texOffs(79, 8).addBox(-1.0F, -5.25F, -0.13F, 2.0F, 4.5F, 0.0F, new CubeDeformation(0.0F)).texOffs(78, 24).addBox(-0.6F, -6.75F, 0.0F, 1.2F, 1.0F, 0.8F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.139626F, 0.139626F, 0.244346F));
        PartDefinition RightAntler = Head.addOrReplaceChild("RightAntler", CubeListBuilder.create().texOffs(93, 0).addBox(-0.725F, -3.05F, -0.725F, 1.45F, 3.15F, 1.45F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.05F, -7.5F, -1.25F, -0.174533F, 0.0F, 0.087266F));
        RightAntler.addOrReplaceChild("cube_8", CubeListBuilder.create().texOffs(101, 0).addBox(-0.42F, -2.35F, -0.42F, 0.84F, 2.45F, 0.84F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.8F, 0.0F, 0.069813F, 0.0F, -0.261799F));
        RightAntler.addOrReplaceChild("cube_9", CubeListBuilder.create().texOffs(106, 0).addBox(-0.26F, -2.31F, -0.26F, 0.52F, 2.43F, 0.52F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.61F, -4.99F, -0.17F, 0.15708F, 0.0F, 0.10472F));
        RightAntler.addOrReplaceChild("cube_10", CubeListBuilder.create().texOffs(72, 32).addBox(-0.225F, -1.75F, -0.325F, 0.65F, 1.95F, 0.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1F, -2.9F, -0.175F, 0.087266F, 0.0F, 0.523599F));
        PartDefinition LeftAntler = Head.addOrReplaceChild("LeftAntler", CubeListBuilder.create().texOffs(84, 8).addBox(-0.725F, -3.05F, -0.725F, 1.45F, 3.15F, 1.45F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.05F, -7.5F, -1.25F, -0.174533F, 0.0F, -0.087266F));
        LeftAntler.addOrReplaceChild("cube_11", CubeListBuilder.create().texOffs(92, 8).addBox(-0.42F, -2.35F, -0.42F, 0.84F, 2.45F, 0.84F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.8F, 0.0F, 0.069813F, 0.0F, 0.261799F));
        LeftAntler.addOrReplaceChild("cube_12", CubeListBuilder.create().texOffs(97, 8).addBox(-0.26F, -2.31F, -0.26F, 0.52F, 2.43F, 0.52F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.61F, -4.99F, -0.17F, 0.15708F, 0.0F, -0.10472F));
        LeftAntler.addOrReplaceChild("cube_13", CubeListBuilder.create().texOffs(80, 32).addBox(-0.425F, -1.75F, -0.325F, 0.65F, 1.95F, 0.65F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1F, -2.9F, -0.175F, 0.087266F, 0.0F, -0.523599F));
        PartDefinition Torso = root.addOrReplaceChild("Torso", CubeListBuilder.create().texOffs(28, 28).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition Tail = Torso.addOrReplaceChild("Tail", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 10.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition TailPrimary = Tail.addOrReplaceChild("TailPrimary", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        TailPrimary.addOrReplaceChild("cube_14", CubeListBuilder.create().texOffs(16, 32).addBox(-1.5F, -2.425F, -0.35F, 3.0F, 4.0F, 3.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, 2.5F, 6.0F, 1.48353F, 0.0F, 0.0F));
        TailPrimary.addOrReplaceChild("cube_15", CubeListBuilder.create().texOffs(48, 10).addBox(-1.5F, 1.1F, -0.6F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 1.178097F, 0.0F, 0.0F));
        PartDefinition RightArm = root.addOrReplaceChild("RightArm", CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition LeftArm = root.addOrReplaceChild("LeftArm", CubeListBuilder.create().texOffs(16, 40).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override public void setupAnim(T entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        super.setupAnim(entity, swing, amount, age, yaw, pitch);
    }
    @Override public ModelPart getArm(HumanoidArm side) { return side == HumanoidArm.LEFT ? leftArm : rightArm; }
    @Override public ModelPart getLeg(HumanoidArm side) { return side == HumanoidArm.LEFT ? leftLeg : rightLeg; }
    @Override public ModelPart getHead() { return head; }
    @Override public ModelPart getTorso() { return torso; }
    @Override public HumanoidAnimator<T, LatexMuntjacModel<T>> getAnimator(T entity) { return animator; }
    @Override public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        root.render(pose, buffer, light, overlay, red, green, blue, alpha);
    }
}
