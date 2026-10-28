package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.ability.GrabEntityAbilityInstance;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.PlayerHugService;
import net.parkabird.changedsynergy.network.PlayerHugActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One authoritative switch when Addon's safe-grab shortcut is used. */
@Pseudo
@Mixin(targets = "net.foxyas.changedaddon.network.packet.TurnOffTransfurPacket", remap = false)
public abstract class AddonPlayerHugToggleMixin {
    @Inject(method = "pressAction", at = @At("HEAD"), cancellable = true, require = 0)
    private static void changedSynergy$toggleHug(Player player, int type, CallbackInfo callback) {
        var variant = player == null ? null : ProcessTransfur.getPlayerTransfurVariant(player);
        if (type != 0 || variant == null || !(variant.getSelectedAbility() instanceof GrabEntityAbilityInstance)
                || !ChangedSynergyConfig.COMMON.playerFriendlyHugs.get()) return;
        callback.cancel();
        if (player instanceof ServerPlayer server)
            PlayerHugService.action(server, PlayerHugActionPacket.TOGGLE, -1);
    }
}
