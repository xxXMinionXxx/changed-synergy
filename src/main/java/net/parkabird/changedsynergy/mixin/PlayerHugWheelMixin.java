package net.parkabird.changedsynergy.mixin;

import java.util.ArrayList;
import java.util.List;
import net.ltxprogrammer.changed.client.gui.AbilityRadialScreen;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.parkabird.changedsynergy.ai.PlayerHugService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbilityRadialScreen.class, remap = false, priority = 900)
public abstract class PlayerHugWheelMixin {
    @Inject(method = "tooltipsFor", at = @At("RETURN"), cancellable = true)
    private void changedSynergy$hugModeTooltip(int section, CallbackInfoReturnable<List<Component>> callback) {
        var screen = (AbilityRadialScreen)(Object)this;
        if (section < 0 || section >= screen.abilities.size() || screen.abilities.get(section) != ChangedAbilities.GRAB_ENTITY_ABILITY.get()
                || callback.getReturnValue() == null) return;
        var grab = screen.variant.getAbilityInstance(ChangedAbilities.GRAB_ENTITY_ABILITY.get());
        if (grab == null) return;
        var lines = new ArrayList<Component>();
        boolean friendly = PlayerHugService.mode(grab);
        lines.add(Component.translatable(friendly ? "ability.changed_synergy.player_hug.mode_friendly" : "ability.changed_synergy.player_hug.mode_combat"));
        if (!friendly) for (var line : callback.getReturnValue().stream().skip(1).toList()) {
            if (line.getContents() instanceof TranslatableContents translated
                    && translated.getKey().startsWith("gui.changed_addon.abilities_radial_screen.mouse.right_click")) continue;
            lines.add(line);
        }
        lines.add(Component.translatable("ability.changed_synergy.player_hug.toggle_hint"));
        if (friendly) lines.add(Component.translatable("ability.changed_synergy.player_hug.description"));
        callback.setReturnValue(lines);
    }
}
