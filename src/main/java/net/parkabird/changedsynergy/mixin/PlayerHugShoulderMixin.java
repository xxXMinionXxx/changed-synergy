package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.ability.AbstractAbility;
import net.ltxprogrammer.changed.client.renderer.animate.upperbody.HoldEntityAnimator;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.parkabird.changedsynergy.ai.PlayerHugService;
import net.parkabird.changedsynergy.client.PlayerHugPoses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Native HOLD translates shoulders forward/down for its combat pose; friendly rotations keep the shoulder sockets. */
@Mixin(value = HoldEntityAnimator.class, remap = false)
public abstract class PlayerHugShoulderMixin {
    @ModifyConstant(method = "setupAnim", constant = {@Constant(floatValue = 2F), @Constant(floatValue = 2.5F)}, require = 4)
    private float changedSynergy$keepShouldersAttached(float offset, ChangedEntity creature, float walk, float speed, float age, float yaw, float pitch) {
        var grab = AbstractAbility.getAbilityInstance(PlayerHugPoses.physical(creature), ChangedAbilities.GRAB_ENTITY_ABILITY.get());
        return grab != null && PlayerHugService.active(grab) && grab.grabbedEntity != null && !grab.suited ? 0 : offset;
    }
}
