package net.parkabird.changedsynergy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ltxprogrammer.changed.client.renderer.layers.LatexSpinAttackEffectLayer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.parkabird.changedsynergy.client.model.LatexSwordfishModel;
import net.parkabird.changedsynergy.entity.LatexSwordfish;
import net.parkabird.changedsynergy.init.ChangedSynergyAbilities;

/** Native Riptide visuals without enabling vanilla's additional weapon-based collision damage. */
public final class SwordfishRiptideLayer extends RenderLayer<LatexSwordfish,LatexSwordfishModel> {
    private final ModelPart box = LatexSpinAttackEffectLayer.createLayer().bakeRoot().getChild("box");
    public SwordfishRiptideLayer(LatexSwordfishRenderer parent) { super(parent); }

    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,LatexSwordfish entity,
            float swing,float swingAmount,float partial,float age,float yaw,float pitch) {
        var rush=entity.getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());
        float alpha=rush==null?0:rush.visualAmount(partial);
        if (alpha<=0 || entity.isAutoSpinAttack()) return;
        var vertices=buffers.getBuffer(RenderType.entityTranslucent(LatexSpinAttackEffectLayer.TEXTURE));
        for (int i=1;i<3;i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(age*-(45+i*5)));
            float scale=0.75F*i;
            pose.scale(scale,scale,scale);
            pose.translate(0,-0.2F+0.6F*i,0);
            box.render(pose,vertices,light,OverlayTexture.NO_OVERLAY,1,1,1,alpha);
            pose.popPose();
        }
    }
}
