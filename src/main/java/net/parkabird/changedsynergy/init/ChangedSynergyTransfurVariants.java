package net.parkabird.changedsynergy.init;

import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.parkabird.changedsynergy.ChangedSynergyMod;
import net.parkabird.changedsynergy.entity.LatexSilkMoth;
import net.parkabird.changedsynergy.entity.LatexSwordfish;
import net.parkabird.changedsynergy.entity.LatexMuntjac;
import net.parkabird.changedsynergy.entity.LatexMuntjacFemale;

public final class ChangedSynergyTransfurVariants {
    public static final DeferredRegister<TransfurVariant<?>> REGISTRY =
            ChangedRegistry.TRANSFUR_VARIANT.createDeferred(ChangedSynergyMod.MOD_ID);

    public static final RegistryObject<TransfurVariant<LatexSilkMoth>> LATEX_SILK_MOTH =
            REGISTRY.register("form_latex_silk_moth", () -> TransfurVariant.Builder
                    .<LatexSilkMoth>of(ChangedSynergyEntities.LATEX_SILK_MOTH)
                    .extraJumps(6)
                    .extraHands()
                    .addAbility(ChangedSynergyAbilities.SPIN_SILK)
                    .build());

    private ChangedSynergyTransfurVariants() {
    }

    public static final RegistryObject<TransfurVariant<LatexSwordfish>> LATEX_SWORDFISH =
            REGISTRY.register("form_latex_swordfish", () -> TransfurVariant.Builder
                    .<LatexSwordfish>of(ChangedSynergyEntities.LATEX_SWORDFISH)
                    .gills().absorbing().addAbility(ChangedSynergyAbilities.SWORDFISH_RUSH).build());

    public static final RegistryObject<TransfurVariant<LatexMuntjac>> LATEX_MUNTJAC =
            REGISTRY.register("form_latex_muntjac", () -> TransfurVariant.Builder
                    .<LatexMuntjac>of(ChangedSynergyEntities.LATEX_MUNTJAC).build());

    public static final RegistryObject<TransfurVariant<LatexMuntjacFemale>> LATEX_MUNTJAC_FEMALE =
            REGISTRY.register("form_latex_muntjac_female", () -> TransfurVariant.Builder
                    .<LatexMuntjacFemale>of(ChangedSynergyEntities.LATEX_MUNTJAC_FEMALE).build());
}
