package net.parkabird.changedsynergy.client;

import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.parkabird.changedsynergy.entity.LatexSilkMoth;

public final class SilkMothWingFlapClient {
    public static void receive(int playerId) {
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(playerId) instanceof Player player)) return;
        ProcessTransfur.ifPlayerTransfurred(player, variant -> {
            if (variant.getChangedEntity() instanceof LatexSilkMoth moth) moth.flapWings();
        });
    }
}
