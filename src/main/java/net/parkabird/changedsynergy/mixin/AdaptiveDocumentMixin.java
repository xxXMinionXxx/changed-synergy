package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.client.gui.TextMenuScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.resources.ResourceLocation;
import net.parkabird.changedsynergy.client.AdaptiveDocumentScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Small hooks around the native editor's text pass; all editing and save logic stays native. */
@Mixin(TextMenuScreen.class)
public abstract class AdaptiveDocumentMixin implements AdaptiveDocumentScreen.NativeEditor {
    @Shadow(remap = false) @Final @Mutable private TextFieldHelper textEdit;
    @Shadow(remap = false) private void clearDisplayCache() { throw new AssertionError(); }
    @Override public void changedSynergy$setEditor(TextFieldHelper editor) { textEdit = editor; }
    @Override public TextFieldHelper changedSynergy$editor() { return textEdit; }
    @Override public void changedSynergy$invalidateDisplay() { clearDisplayCache(); }

    @Group(name = "documentPaper", min = 1, max = 1)
    @Redirect(method = {"render", "m_88315_"}, require = 0, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private void changedSynergy$paper(GuiGraphics graphics, ResourceLocation texture, int x, int y, float u, float v,
            int width, int height, int textureWidth, int textureHeight) {
        if ((Object)this instanceof AdaptiveDocumentScreen<?> screen) screen.renderPaper(graphics);
        else graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
    @Group(name = "documentPaper", min = 1, max = 1)
    @Redirect(method = {"render", "m_88315_"}, require = 0, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphics;m_280163_(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private void changedSynergy$paperProduction(GuiGraphics graphics, ResourceLocation texture, int x, int y, float u, float v,
            int width, int height, int textureWidth, int textureHeight) {
        changedSynergy$paper(graphics, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
    @Inject(method = {"render", "m_88315_"}, at = @At(value = "INVOKE", remap = false, target =
            "Lnet/ltxprogrammer/changed/client/gui/TextMenuScreen;getDisplayCache()Lnet/ltxprogrammer/changed/client/gui/TextMenuScreen$DisplayCache;"))
    private void changedSynergy$begin(GuiGraphics graphics, int x, int y, float partialTicks, CallbackInfo callback) {
        if ((Object)this instanceof AdaptiveDocumentScreen<?> screen) screen.beginText(graphics);
    }
    @Inject(method = {"render", "m_88315_"}, at = @At(value = "INVOKE", remap = false, target =
            "Lnet/ltxprogrammer/changed/client/gui/TextMenuScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void changedSynergy$end(GuiGraphics graphics, int x, int y, float partialTicks, CallbackInfo callback) {
        if ((Object)this instanceof AdaptiveDocumentScreen<?> screen) screen.endText(graphics);
    }
    @ModifyArg(method = "convertScreenToLocal", remap = false, at = @At(value = "INVOKE", target =
            "Lnet/ltxprogrammer/changed/client/gui/TextMenuScreen$Pos2i;<init>(II)V"), index = 1)
    private int changedSynergy$scrollInput(int y) {
        return (Object)this instanceof AdaptiveDocumentScreen<?> screen ? y + screen.textScroll() : y;
    }
}
