package net.parkabird.changedsynergy.ai;

/** Native random-sequence position, so a missed seed packet cannot disable escape. */
public interface GrabEscapeState {
    long changedSynergy$escapeSeed();
    int changedSynergy$escapeDraws();
    void changedSynergy$restoreEscape(long seed, int draws);
}
