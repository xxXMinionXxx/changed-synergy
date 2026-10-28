package net.parkabird.changedsynergy.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.parkabird.changedsynergy.ability.SwordfishRushAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(LivingEntity.class)
public abstract class SwordfishRushArmourMixin {
    @Group(name = "rushArmour", min = 1, max = 1)
    @WrapOperation(method = {"getDamageAfterArmorAbsorb", "m_21161_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(FFF)F"), require = 0)
    private float changedSynergy$rushPenetration(float damage, float armour, float toughness,
            Operation<Float> original, @Local(argsOnly = true) DamageSource source) {
        // Compose with other armour formulas without requiring Mixin's generated Args classes.
        return original.call(damage, source instanceof SwordfishRushAbility.RushDamageSource ? armour * .5F : armour, toughness);
    }

    @Group(name = "rushArmour", min = 1, max = 1)
    @WrapOperation(method = {"getDamageAfterArmorAbsorb", "m_21161_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatRules;m_19272_(FFF)F"), require = 0)
    private float changedSynergy$rushPenetrationProduction(float damage, float armour, float toughness,
            Operation<Float> original, @Local(argsOnly = true) DamageSource source) {
        return changedSynergy$rushPenetration(damage, armour, toughness, original, source);
    }
}
