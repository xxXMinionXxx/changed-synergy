package net.parkabird.changedsynergy.ai;

import java.util.EnumSet;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public final class CompanionSeatGoal extends Goal {
    private final ChangedEntity mob;
    public CompanionSeatGoal(ChangedEntity mob) { this.mob = mob; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
    @Override public boolean canUse() { return CompanionSeatingService.pending(mob); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void start() { mob.getNavigation().stop(); }
    @Override public void tick() { CompanionSeatingService.walk(mob); }
    @Override public void stop() { CompanionSeatingService.finish(mob, false, "interrupted"); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
}
