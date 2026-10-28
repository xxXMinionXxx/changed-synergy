package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.parkabird.changedsynergy.ability.SwordfishRushAbility.RushDamageSource;
import net.parkabird.changedsynergy.init.ChangedSynergyEntities;

/** Checks the real transformed LivingEntity calculation, with and without Apothic Attributes. */
@Mod.EventBusSubscriber(modid = "changed_synergy", value = Dist.DEDICATED_SERVER)
public final class ApothicArmourRegression {
    private static int checks;
    @SubscribeEvent public static void run(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            var level = server.overworld();
            boolean apothic = ModList.get().isLoaded("attributeslib");
            Method absorb = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "m_21161_", DamageSource.class, float.class);
            Method formula = apothic ? Class.forName("dev.shadowsoffire.attributeslib.api.ALCombatRules")
                    .getMethod("getDamageAfterArmor", LivingEntity.class, DamageSource.class, float.class, float.class, float.class) : null;
            var player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "ArmourCheck"));
            var swordfish = ChangedSynergyEntities.LATEX_SWORDFISH.get().create(level);
            var target = EntityType.COW.create(level);
            for (var attacker : new LivingEntity[]{player, swordfish}) {
                DamageSource attack = attacker == player ? level.damageSources().playerAttack(player) : level.damageSources().mobAttack(attacker);
                var rush = new RushDamageSource(attack);
                check(rush.getEntity() == attacker && rush.typeHolder() == attack.typeHolder(), "native attack attribution is preserved");
                for (double pierce : apothic ? new double[]{0, 4} : new double[]{0}) {
                    if (apothic) {
                        attacker.getAttribute(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", "armor_pierce"))).setBaseValue(pierce);
                        attacker.getAttribute(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", "armor_shred"))).setBaseValue(pierce == 0 ? 0 : .25);
                    }
                    for (int armour : new int[]{0, 20, 40}) for (int toughness : new int[]{0, 8}) for (float damage : new float[]{8, 24}) {
                        target.getAttribute(Attributes.ARMOR).setBaseValue(armour);
                        target.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(toughness);
                        float actualArmour = target.getArmorValue();
                        float ordinary = (float)absorb.invoke(target, attack, damage);
                        float rushing = (float)absorb.invoke(target, rush, damage);
                        float expected = formula == null ? CombatRules.getDamageAfterAbsorb(damage, actualArmour, toughness)
                                : (float)formula.invoke(null, target, attack, damage, actualArmour, (float)toughness);
                        float expectedRush = formula == null ? CombatRules.getDamageAfterAbsorb(damage, actualArmour * .5F, toughness)
                                : (float)formula.invoke(null, target, rush, damage, actualArmour * .5F, (float)toughness);
                        check(Math.abs(ordinary - expected) < .001, "ordinary attacks preserve the active armour formula: damage=" + damage
                                + " armour=" + actualArmour + " toughness=" + toughness + " pierce=" + pierce + " actual=" + ordinary + " expected=" + expected);
                        check(Math.abs(rushing - expectedRush) < .001,
                                "rush composes half armour with active formula: apothic=" + apothic + " damage=" + damage
                                + " armour=" + armour + " toughness=" + toughness + " pierce=" + pierce + " actual=" + rushing + " expected=" + expectedRush);
                        check(rushing >= ordinary && rushing <= damage, "half-armour penetration stays bounded");
                    }
                }
            }
            target.getAttribute(Attributes.ARMOR).setBaseValue(20);
            target.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
            var attack = level.damageSources().playerAttack(player);
            float health = target.getHealth();
            target.hurt(new RushDamageSource(attack), 8);
            float damage = health - target.getHealth();
            check(damage > 0 && damage < 8, "real rush remains partially reduced by armour");
            target.invulnerableTime = 0; target.setHealth(target.getMaxHealth());
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
            health = target.getHealth(); target.hurt(new RushDamageSource(attack), 8);
            check(health - target.getHealth() < damage, "resistance remains effective after penetration");
            System.out.println("APOTHIC_ARMOUR_REGRESSION_PASS: " + checks + " checks; attributeslib=" + apothic);
        } catch (Throwable failure) {
            System.err.println("APOTHIC_ARMOUR_REGRESSION_FAIL"); failure.printStackTrace();
        } finally { server.halt(false); }
    }
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
}
