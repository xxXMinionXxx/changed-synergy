package net.parkabird.changedsynergy.init;

import java.util.Collection;
import java.util.List;
import net.ltxprogrammer.changed.ability.AbstractAbility;
import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.ability.SimpleCreateItemAbility;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.parkabird.changedsynergy.ChangedSynergyMod;

public final class ChangedSynergyAbilities {
    public static final DeferredRegister<AbstractAbility<?>> REGISTRY =
            ChangedRegistry.ABILITY.createDeferred(ChangedSynergyMod.MOD_ID);

    public static final RegistryObject<net.parkabird.changedsynergy.ability.SwordfishRushAbility> SWORDFISH_RUSH =
            REGISTRY.register("swordfish_rush", net.parkabird.changedsynergy.ability.SwordfishRushAbility::new);

    public static final RegistryObject<SimpleCreateItemAbility> SPIN_SILK =
            REGISTRY.register("spin_silk", () -> new SimpleCreateItemAbility(
                    () -> new ItemStack(Items.STRING, 4), 5.0F, 6.0F) {
                @Override
                public Collection<Component> getAbilityDescription(IAbstractChangedEntity entity) {
                    return List.of(Component.translatable("ability.changed_synergy.spin_silk.desc"));
                }
                @Override
                public void startUsing(IAbstractChangedEntity entity) {
                    super.startUsing(entity);
                    if (entity.getLevel().isClientSide) return;
                    var owner = entity.getEntity() instanceof net.minecraft.server.level.ServerPlayer player ? player
                            : net.parkabird.changedsynergy.compat.TrueTransfurCompat.bodyOwner(entity.getChangedEntity());
                    if (owner != null)
                        net.parkabird.changedsynergy.advancement.SynergyAdvancements.grant(owner,
                                net.parkabird.changedsynergy.advancement.SynergyAdvancements.SPIN_SILK);
                }
            });

    private ChangedSynergyAbilities() {}
}
