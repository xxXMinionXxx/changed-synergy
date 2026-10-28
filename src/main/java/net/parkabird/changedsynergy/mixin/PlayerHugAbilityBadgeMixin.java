package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.ability.AbstractAbilityInstance;
import net.ltxprogrammer.changed.ability.GrabEntityAbilityInstance;
import net.ltxprogrammer.changed.client.AbilityRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.parkabird.changedsynergy.ai.PlayerHugService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbilityRenderer.class, remap = false)
public abstract class PlayerHugAbilityBadgeMixin {
    @Inject(method = "renderAndDecorateAbility(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/LivingEntity;Lnet/ltxprogrammer/changed/ability/AbstractAbilityInstance;IIIFZI)V", at = @At("TAIL"))
    private void changedSynergy$friendlyBadge(GuiGraphics graphics, LivingEntity entity, AbstractAbilityInstance ability,
            int x, int y, int size, float alpha, boolean enabled, int seed, CallbackInfo callback) {
        if (!(ability instanceof GrabEntityAbilityInstance grab) || !PlayerHugService.mode(grab)) return;
        int badge = Math.max(7, size / 3);
        graphics.setColor(1, 1, 1, alpha);
        graphics.blit(ResourceLocation.fromNamespaceAndPath("changed_synergy", "textures/gui/radial/icons/friendly_hug_heart.png"),
                x + size - badge + Math.max(2, badge / 2), y + size - badge + Math.max(1, badge / 4),
                badge, badge, 0, 0, 9, 9, 9, 9);
        graphics.setColor(1, 1, 1, 1);
    }
}
