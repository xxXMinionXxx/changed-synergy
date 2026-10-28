package net.parkabird.changedsynergy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ltxprogrammer.changed.client.renderer.AdvancedHumanoidRenderer;
import net.ltxprogrammer.changed.client.renderer.layers.CustomEyesLayer;
import net.ltxprogrammer.changed.client.renderer.layers.LatexParticlesLayer;
import net.ltxprogrammer.changed.client.renderer.layers.TransfurCapeLayer;
import net.ltxprogrammer.changed.client.renderer.model.armor.ArmorLatexMaleSharkModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.parkabird.changedsynergy.client.model.LatexSwordfishModel;
import net.parkabird.changedsynergy.client.model.SwordfishRushPose;
import net.parkabird.changedsynergy.entity.LatexSwordfish;
import net.parkabird.changedsynergy.init.ChangedSynergyAbilities;

public final class LatexSwordfishRenderer extends AdvancedHumanoidRenderer<LatexSwordfish, LatexSwordfishModel> {
    private final java.util.Set<net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator<?,?>> rushArmour =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "changed_synergy", "textures/latex_swordfish.png");
    public LatexSwordfishRenderer(EntityRendererProvider.Context context) {
        super(context,new LatexSwordfishModel(context.bakeLayer(LatexSwordfishModel.LAYER_LOCATION)),
                ArmorLatexMaleSharkModel.MODEL_SET,0.5F);
        addLayer(new LatexParticlesLayer<>(this,getModel()));
        addLayer(TransfurCapeLayer.normalCape(this,context.getModelSet()));
        addLayer(new CustomEyesLayer<>(this,context.getModelSet()));
        addLayer(new SwordfishRiptideLayer(this));
    }
    @Override public ResourceLocation getTextureLocation(LatexSwordfish entity) { return TEXTURE; }
    @Override protected void setupRotations(LatexSwordfish entity, PoseStack pose, float bob,float bodyYaw,float partialTicks) {
        getArmorLayer().modelPicker.forEach(entity, net.ltxprogrammer.changed.client.renderer.model.armor.ArmorModel::isArmor,
                (layer,model) -> {
                    var animator=model.getAnimator(entity);
                    if (rushArmour.add(animator)) configureArmour(animator);
                });
        var rush=entity.getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());
        float blend=rush==null?0:rush.visualAmount(partialTicks);
        if (blend<=0) { super.setupRotations(entity,pose,bob,bodyYaw,partialTicks); return; }
        var direction=rush.direction();
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=(float)Math.toDegrees(Math.atan2(-direction.y,direction.horizontalDistance()));
        float originalPitch=entity.getXRot();
        try {
            entity.setXRot(Mth.rotLerp(blend,originalPitch,pitch));
            super.setupRotations(entity,pose,bob,Mth.rotLerp(blend,bodyYaw,yaw),partialTicks);
        } finally { entity.setXRot(originalPitch); }
        // Keep the same swimming anchor for NPCs that have not entered the vanilla swim pose.
        if (!entity.isVisuallySwimming()) pose.translate(0,-blend,0.3F*blend);
        pose.mulPose(Axis.YP.rotationDegrees(rush.spin(partialTicks)));
    }
    private static <T extends net.ltxprogrammer.changed.entity.ChangedEntity,
            M extends net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel<T>>
            void configureArmour(net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator<T,M> animator) {
        animator.addAnimator(new net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator.Animator<T,M>() {
            @Override public net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator.AnimateStage preferredStage() {
                return net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator.AnimateStage.FINAL;
            }
            @Override public void setupAnim(T entity,float swing,float amount,float age,float yaw,float pitch) {
                var rush=entity.getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());
                if (rush!=null) SwordfishRushPose.apply(core.entityModel,rush.visualAmount(core.partialTicks));
            }
        },net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator.AnimateStage.FINAL);
    }
}
