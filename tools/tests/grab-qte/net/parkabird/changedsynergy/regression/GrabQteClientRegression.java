package net.parkabird.changedsynergy.regression;

import net.ltxprogrammer.changed.ability.AbstractAbilityInstance.KeyReference;
import net.ltxprogrammer.changed.entity.LivingEntityDataExtension;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.parkabird.changedsynergy.ai.BondedSuitService;
import net.parkabird.changedsynergy.client.GrabQteClientState;
import net.parkabird.changedsynergy.network.FriendlySocialHugState;
import net.parkabird.changedsynergy.network.GrabQteSyncPacket;

/** Private real-client fixture, including Addon's enabled optional key hint. */
public final class GrabQteClientRegression {
    public static void run(LocalPlayer player) throws Exception {
        var level = (net.minecraft.client.multiplayer.ClientLevel)player.level();
        var bee = ChangedEntities.LATEX_BEE.get().create(level);
        var plant = ChangedEntities.LATEX_MIMIC_PLANT.get().create(level);
        bee.setNoAi(true); plant.setNoAi(true);
        bee.setPos(player.position()); plant.setPos(player.position());
        level.putNonPlayerEntity(910001, bee); level.putNonPlayerEntity(910002, plant);
        ForgeConfigSpec.ConfigValue<Boolean> hint = null;
        boolean previousHint = false;
        try {
            if (net.minecraftforge.fml.ModList.get().isLoaded("changed_addon")) {
                @SuppressWarnings("unchecked") var config = (ForgeConfigSpec.ConfigValue<Boolean>)Class.forName(
                        "net.foxyas.changedaddon.configuration.ChangedAddonClientConfiguration")
                        .getField("GRAB_ABILITY_KEY_INFO").get(null);
                hint = config; previousHint = hint.get(); hint.set(true);
                check(hint.get(), "Addon grab-key hints are actually enabled in this fixture");
            }
            GrabQteRegression.run(player, bee, plant);
            var hold = BondedSuitService.ability(bee);
            hold.grabbedEntity = player; hold.grabStrength = 1; hold.ticksGrabbed = 20;
            var packet = new GrabQteSyncPacket(bee.getId(), player.getId(),
                    KeyReference.MOVE_BACKWARD.ordinal(), KeyReference.MOVE_LEFT.ordinal(), 7);
            GrabQteClientState.receive(packet);
            check(hold.currentEscapeKey == null && hold.escapeKeyRandom == null,
                    "An early authoritative key packet waits for Changed's seed");
            hold.initializeEscape(617L);
            var random = hold.escapeKeyRandom;
            GrabQteClientState.onClientTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            check(hold.currentEscapeKey == KeyReference.MOVE_BACKWARD
                            && hold.lastEscapeKey == KeyReference.MOVE_LEFT
                            && hold.ticksUnpressed == 7 && hold.escapeKeyRandom == random,
                    "Queued sync applies after initialization without restarting the sequence");
            GrabQteClientState.receive(new GrabQteSyncPacket(bee.getId(), player.getId(), -1, -1, 0));
            check(hold.currentEscapeKey == null && hold.lastEscapeKey == null,
                    "A no-QTE sync still clears the prompt immediately");
            FriendlySocialHugState.update(bee.getId(), player.getId(), true, -100);
            hold.escapeKeyRandom=null;hold.currentEscapeKey=null;
            GrabQteClientState.receive(new GrabQteSyncPacket(bee.getId(),player.getId(),
                    KeyReference.MOVE_RIGHT.ordinal(),KeyReference.MOVE_LEFT.ordinal(),7,9821L,12));
            check(hold.escapeKeyRandom!=null&&hold.currentEscapeKey==KeyReference.MOVE_RIGHT,
                    "Authoritative sequence recovers a missing native seed during a safe carry");
            var restored=hold.escapeKeyRandom;var state=(net.parkabird.changedsynergy.ai.GrabEscapeState)hold;
            check(state.changedSynergy$escapeSeed()==9821L&&state.changedSynergy$escapeDraws()==12,
                    "Recovered random sequence is at the server position");
            GrabQteClientState.receive(new GrabQteSyncPacket(bee.getId(),player.getId(),
                    KeyReference.MOVE_RIGHT.ordinal(),KeyReference.MOVE_LEFT.ordinal(),7,9821L,12));
            check(hold.escapeKeyRandom==restored,"Heartbeat does not restart an already aligned sequence");
            var expectedRandom=net.minecraft.util.RandomSource.create(9821L);
            for(int i=0;i<12;i++)expectedRandom.nextInt(4);
            check(hold.escapeKeyRandom.nextInt(4)==expectedRandom.nextInt(4),"Recovered native random sequence predicts the same next key");
            hold.initializeEscape(9821L);bee.tickCount++;hold.handleEscape();hold.tickIdle();
            check(hold.currentEscapeKey!=null&&hold.grabStrength==1&&hold.grabbedEntity==player,
                    "Safe carries retain escape QTE with key hints enabled");
            FriendlySocialHugState.update(bee.getId(), player.getId(), false, 0);
            hold.releaseEntity(false);
            check(hold.grabbedEntity == null && !player.noPhysics,
                    "Safe carry can still release and restore player movement");
            System.out.println("GRAB_QTE_CLIENT_PASS");
        } finally {
            if (hint != null) hint.set(previousHint);
            FriendlySocialHugState.update(bee.getId(), player.getId(), false, 0);
            for (var creature : java.util.List.of(bee, plant)) {
                var ability = BondedSuitService.ability(creature);
                if (ability != null) ability.releaseEntity(false);
            }
            ((LivingEntityDataExtension)player).setGrabbedBy(null); player.noPhysics = false;
            level.removeEntity(bee.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            level.removeEntity(plant.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("GRAB_QTE_CLIENT_CHECK: " + message);
    }
}
