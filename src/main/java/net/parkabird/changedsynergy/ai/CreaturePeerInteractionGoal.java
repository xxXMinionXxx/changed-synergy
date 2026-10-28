package net.parkabird.changedsynergy.ai;

import java.util.EnumSet;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** Reserves movement for one server-owned game, below commands, rescue and work. */
public final class CreaturePeerInteractionGoal extends Goal {
    public static final int PRIORITY = 1;
    private final ChangedEntity creature;

    public CreaturePeerInteractionGoal(ChangedEntity creature) {
        this.creature = creature;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override public boolean canUse() { return CreaturePeerInteractionService.active(creature); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() { CreaturePeerInteractionService.move(creature); }
    @Override public void stop() {
        // Changed stops a held mob's goals every tick. That is the game's own hold.
        if (!CreaturePeerInteractionService.isHeld(creature)) CreaturePeerInteractionService.cancel(creature);
    }
}
