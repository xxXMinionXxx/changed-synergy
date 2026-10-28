package net.parkabird.changedsynergy.client;

import net.ltxprogrammer.changed.client.renderer.AdvancedHumanoidRenderer;
import net.ltxprogrammer.changed.client.renderer.layers.CustomEyesLayer;
import net.ltxprogrammer.changed.client.renderer.layers.DoubleItemInHandLayer;
import net.ltxprogrammer.changed.client.renderer.layers.GasMaskLayer;
import net.ltxprogrammer.changed.client.renderer.layers.LatexParticlesLayer;
import net.ltxprogrammer.changed.client.renderer.layers.TransfurCapeLayer;
import net.ltxprogrammer.changed.client.renderer.model.armor.ArmorLatexBeeModel;
import net.ltxprogrammer.changed.util.Color3;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.parkabird.changedsynergy.ChangedSynergyMod;
import net.parkabird.changedsynergy.client.model.LatexSilkMothModel;
import net.parkabird.changedsynergy.entity.LatexSilkMoth;

public final class LatexSilkMothRenderer
        extends AdvancedHumanoidRenderer<LatexSilkMoth, LatexSilkMothModel<LatexSilkMoth>> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ChangedSynergyMod.MOD_ID, "textures/latex_silk_moth.png");

    public LatexSilkMothRenderer(EntityRendererProvider.Context context) {
        super(context, new LatexSilkMothModel<>(context.bakeLayer(LatexSilkMothModel.LAYER_LOCATION)),
                ArmorLatexBeeModel.MODEL_SET, 0.5F);
        addLayer(new DoubleItemInHandLayer<>(this, context.getItemInHandRenderer()));
        addLayer(new LatexParticlesLayer<>(this, getModel()));
        addLayer(TransfurCapeLayer.normalCape(this, context.getModelSet()));
        addLayer(CustomEyesLayer.builder(this, context.getModelSet()).withSclera(Color3.fromInt(0x1B1B1B)).build());
        addLayer(GasMaskLayer.forNormal(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(LatexSilkMoth entity) {
        return TEXTURE;
    }
}
