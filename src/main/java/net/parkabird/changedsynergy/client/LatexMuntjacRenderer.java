package net.parkabird.changedsynergy.client;

import net.ltxprogrammer.changed.client.renderer.AdvancedHumanoidRenderer;
import net.ltxprogrammer.changed.client.renderer.layers.CustomEyesLayer;
import net.ltxprogrammer.changed.client.renderer.layers.GasMaskLayer;
import net.ltxprogrammer.changed.client.renderer.layers.LatexParticlesLayer;
import net.ltxprogrammer.changed.client.renderer.layers.TransfurCapeLayer;
import net.ltxprogrammer.changed.client.renderer.model.armor.ArmorLatexDeerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.parkabird.changedsynergy.ChangedSynergyMod;
import net.parkabird.changedsynergy.client.model.LatexMuntjacModel;
import net.parkabird.changedsynergy.entity.LatexMuntjac;

public final class LatexMuntjacRenderer
        extends AdvancedHumanoidRenderer<LatexMuntjac, LatexMuntjacModel<LatexMuntjac>> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ChangedSynergyMod.MOD_ID, "textures/latex_muntjac.png");

    public LatexMuntjacRenderer(EntityRendererProvider.Context context) {
        super(context, new LatexMuntjacModel<>(context.bakeLayer(LatexMuntjacModel.LAYER_LOCATION)),
                ArmorLatexDeerModel.MODEL_SET, 0.5F);
        addLayer(new LatexParticlesLayer<>(this, getModel()));
        addLayer(TransfurCapeLayer.normalCape(this, context.getModelSet()));
        addLayer(new CustomEyesLayer<>(this, context.getModelSet()));
        addLayer(GasMaskLayer.forSnouted(this, context.getModelSet()));
    }

    @Override public ResourceLocation getTextureLocation(LatexMuntjac entity) { return TEXTURE; }
}
