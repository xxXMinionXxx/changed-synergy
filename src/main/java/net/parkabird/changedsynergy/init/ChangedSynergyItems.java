package net.parkabird.changedsynergy.init;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.parkabird.changedsynergy.ChangedSynergyMod;
import net.parkabird.changedsynergy.item.BrokenDarkLatexMaskItem;
import net.parkabird.changedsynergy.item.RepairedDarkLatexMaskItem;

/** Items owned by Synergy. */
public final class ChangedSynergyItems {
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(
            ForgeRegistries.ITEMS, ChangedSynergyMod.MOD_ID);

    public static final RegistryObject<Item> LATEX_SILK_MOTH_SPAWN_EGG = REGISTRY.register(
            "latex_silk_moth_spawn_egg", () -> new ForgeSpawnEggItem(
                    ChangedSynergyEntities.LATEX_SILK_MOTH, 0xEADCC6, 0xB19B7A, new Item.Properties()));

    public static final RegistryObject<Item> LATEX_SWORDFISH_SPAWN_EGG = REGISTRY.register(
            "latex_swordfish_spawn_egg", () -> new ForgeSpawnEggItem(
                    ChangedSynergyEntities.LATEX_SWORDFISH, 0x294960, 0xB7CAD3, new Item.Properties()));

    public static final RegistryObject<Item> LATEX_MUNTJAC_SPAWN_EGG = REGISTRY.register(
            "latex_muntjac_spawn_egg", () -> new ForgeSpawnEggItem(
                    ChangedSynergyEntities.LATEX_MUNTJAC, 0xB78A5D, 0xE6D4B4, new Item.Properties()));

    public static final RegistryObject<Item> LATEX_MUNTJAC_FEMALE_SPAWN_EGG = REGISTRY.register(
            "latex_muntjac_female_spawn_egg", () -> new ForgeSpawnEggItem(
                    ChangedSynergyEntities.LATEX_MUNTJAC_FEMALE, 0xB78A5D, 0xE6D4B4, new Item.Properties()));

    public static final RegistryObject<Item> BROKEN_DARK_LATEX_MASK = REGISTRY.register(
            "broken_dark_latex_mask",
            () -> new BrokenDarkLatexMaskItem(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()));

    public static final RegistryObject<Item> REPAIRED_DARK_LATEX_MASK = REGISTRY.register(
            "repaired_dark_latex_mask",
            RepairedDarkLatexMaskItem::new);

    private ChangedSynergyItems() {
    }
}
