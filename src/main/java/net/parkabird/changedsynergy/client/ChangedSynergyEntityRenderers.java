package net.parkabird.changedsynergy.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.ChangedSynergyMod;
import net.parkabird.changedsynergy.init.ChangedSynergyEntities;
import net.parkabird.changedsynergy.client.model.LatexSilkMothModel;
import net.parkabird.changedsynergy.client.model.LatexSwordfishModel;
import net.parkabird.changedsynergy.client.model.LatexMuntjacModel;
import net.parkabird.changedsynergy.client.model.LatexMuntjacFemaleModel;

/** Client-only entity renderer registration. */
@Mod.EventBusSubscriber(
        modid = ChangedSynergyMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ChangedSynergyEntityRenderers {
    private ChangedSynergyEntityRenderers() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ChangedSynergyEntities.CREATURE_FISHING_HOOK.get(),
                CreatureFishingHookRenderer::new);
        event.registerEntityRenderer(ChangedSynergyEntities.LATEX_SILK_MOTH.get(), LatexSilkMothRenderer::new);
        event.registerEntityRenderer(ChangedSynergyEntities.LATEX_SWORDFISH.get(), LatexSwordfishRenderer::new);
        event.registerEntityRenderer(ChangedSynergyEntities.LATEX_MUNTJAC.get(), LatexMuntjacRenderer::new);
        event.registerEntityRenderer(ChangedSynergyEntities.LATEX_MUNTJAC_FEMALE.get(), LatexMuntjacFemaleRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LatexSilkMothModel.LAYER_LOCATION, LatexSilkMothModel::createBodyLayer);
        event.registerLayerDefinition(LatexSwordfishModel.LAYER_LOCATION, LatexSwordfishModel::createBodyLayer);
        event.registerLayerDefinition(LatexMuntjacModel.LAYER_LOCATION, LatexMuntjacModel::createBodyLayer);
        event.registerLayerDefinition(LatexMuntjacFemaleModel.LAYER_LOCATION, LatexMuntjacFemaleModel::createBodyLayer);
    }
}
