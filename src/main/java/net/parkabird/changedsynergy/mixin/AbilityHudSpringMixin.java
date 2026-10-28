package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.client.gui.AbilityOverlay;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.parkabird.changedsynergy.client.AbilityHudMotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applied after optional HUD injectors; the plugin replaces their shared slide easing call. */
@Mixin(value = AbilityOverlay.class, priority = 800, remap = false)
public abstract class AbilityHudSpringMixin {
    @Inject(method = "renderSelectedAbility", at = @At("HEAD"), remap = false)
    private static void changedSynergy$hudMotion(Gui gui, GuiGraphics graphics, float partialTick,
            int width, int height, CallbackInfo callback) { AbilityHudMotion.beginFrame(); }
}
