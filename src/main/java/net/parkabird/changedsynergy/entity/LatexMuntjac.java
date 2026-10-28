package net.parkabird.changedsynergy.entity;

import net.ltxprogrammer.changed.entity.beast.LatexDeer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Jungle deer relative; inherits the native deer's attributes and transfur behaviour. */
public final class LatexMuntjac extends LatexDeer implements net.ltxprogrammer.changed.entity.GenderedEntity {
    public LatexMuntjac(EntityType<? extends LatexMuntjac> type, Level level) {
        super(type, level);
    }

    @Override public net.ltxprogrammer.changed.entity.Gender getGender() {
        return net.ltxprogrammer.changed.entity.Gender.MALE;
    }
}
