package net.parkabird.changedsynergy.ai;

/** Frozen on the native ability for the lifetime of one player-initiated hold. */
public interface PlayerHugAccess {
    boolean changedSynergy$isPlayerHug();
    void changedSynergy$setPlayerHug(boolean friendly);
    boolean changedSynergy$isUprightCarry();
    void changedSynergy$setUprightCarry(boolean upright);
}
