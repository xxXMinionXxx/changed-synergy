package net.parkabird.changedsynergy.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.parkabird.changedsynergy.client.PartialSkinLayersCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "net.ltxprogrammer.changed.client.renderer.model.LatexHumanModel",
        "net.ltxprogrammer.changed.client.renderer.model.DarkLatexWolfPartialModel",
        "net.foxyas.changedaddon.client.model.SnowLeopardPartialModel"
}, remap = false)
public abstract class PartialSkinLayersMixin {
    @Inject(method = "setModelProperties", at = @At("RETURN"))
    private void changedSynergy$playerSkinLayers(AbstractClientPlayer player, CallbackInfo callback) {
        PartialSkinLayersCompat.apply(this, player);
    }

    @Inject(method = "defaultModelProperties", at = @At("RETURN"))
    private void changedSynergy$clearPlayerSkinLayers(CallbackInfo callback) {
        PartialSkinLayersCompat.apply(this, null);
    }
}
