package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.client.gui.TamedDarkLatexScreen;
import net.ltxprogrammer.changed.client.gui.VariantRadialScreen;
import net.parkabird.changedsynergy.client.RadialWheelAnimations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets native Changed wheel frames share Synergy's per-section fade. */
@Mixin(
        value = {VariantRadialScreen.class, TamedDarkLatexScreen.class},
        remap = false)
public abstract class NativeRadialFrameAlphaMixin {
    @Group(name = "frameHighlight", min = 1, max = 1)
    @Redirect(method = "renderSectionBackground", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphics;m_280163_(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"), require = 0)
    private void changedSynergy$blendFrame(net.minecraft.client.gui.GuiGraphics graphics,
            net.minecraft.resources.ResourceLocation texture, int x, int y, float u, float v,
            int width, int height, int textureWidth, int textureHeight) {
        RadialWheelAnimations.drawFrame(this, graphics, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    @Group(name = "frameHighlight", min = 1, max = 1)
    @Redirect(method = "renderSectionBackground", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"), require = 0)
    private void changedSynergy$blendFrameMojmap(net.minecraft.client.gui.GuiGraphics graphics,
            net.minecraft.resources.ResourceLocation texture, int x, int y, float u, float v,
            int width, int height, int textureWidth, int textureHeight) {
        changedSynergy$blendFrame(graphics, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
    @ModifyArg(
            method = "renderSectionBackground",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;m_280246_(FFFF)V",
                    remap = false),
            index = 3,
            remap = false,
            require = 0)
    private float changedSynergy$fadeNativeFrame(float originalAlpha) {
        return originalAlpha * RadialWheelAnimations.layerAlpha(this);
    }

    /** Mojmap development runs expose GuiGraphics#setColor by its readable name. */
    @ModifyArg(
            method = "renderSectionBackground",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;setColor(FFFF)V",
                    remap = false),
            index = 3,
            remap = false,
            require = 0)
    private float changedSynergy$fadeNativeFrameMojmap(float originalAlpha) {
        return originalAlpha * RadialWheelAnimations.layerAlpha(this);
    }

}
