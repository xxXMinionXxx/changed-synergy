package net.parkabird.changedsynergy.client;

import net.parkabird.changedsynergy.ChangedSynergyClientConfig;

/** Shared presentation policy; layout and game progress remain independent of animation. */
public final class UiMotion {
    private UiMotion() {}
    public static boolean reduced() { return ChangedSynergyClientConfig.CLIENT.reducedUiMotion.get(); }
    public static float transition(double progress) {
        if (progress <= 0) return 0;
        if (progress >= 1) return 1;
        return UiSpring.clamp(UiSpring.response(progress * 0.24));
    }
    public static float entrance(long openedAt, int index) {
        double age = (System.nanoTime() - openedAt) / 1_000_000_000.0;
        return UiSpring.response(reduced() ? age * 2.5 : age - index * 0.035);
    }
    public static int factionColor(net.parkabird.changedsynergy.ai.HunterFaction faction) {
        return switch (faction) {
            case WHITE -> 0xF2F5FF;
            case DARK -> 0x9B59D0;
            case AQUATIC -> 0x4FD6E8;
            case LIGHT -> 0xE5A84B;
        };
    }
}
