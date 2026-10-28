package net.parkabird.changedsynergy.entity;

import net.ltxprogrammer.changed.entity.Gender;
import net.ltxprogrammer.changed.entity.GenderedEntity;
import net.ltxprogrammer.changed.entity.beast.LatexDeer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class LatexMuntjacFemale extends LatexDeer implements GenderedEntity {
    public LatexMuntjacFemale(EntityType<? extends LatexMuntjacFemale> type, Level level) { super(type, level); }
    @Override public Gender getGender() { return Gender.FEMALE; }
}
