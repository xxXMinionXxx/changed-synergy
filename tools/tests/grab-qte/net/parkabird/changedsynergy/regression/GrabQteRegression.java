package net.parkabird.changedsynergy.regression;

import net.ltxprogrammer.changed.ability.GrabEntityAbilityInstance;
import net.ltxprogrammer.changed.ability.AbstractAbilityInstance.KeyReference;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.LivingEntityDataExtension;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.parkabird.changedsynergy.ai.BondedSuitService;

/** Runs the actual Changed escape/input and handoff code on both logical sides. */
public final class GrabQteRegression {
    private static int checks;

    public static void runServer(ServerPlayer player) throws Exception {
        var bee = ChangedEntities.LATEX_BEE.get().create(player.level());
        var plant = ChangedEntities.LATEX_MIMIC_PLANT.get().create(player.level());
        bee.setNoAi(true); plant.setNoAi(true);
        bee.setPos(player.position()); plant.setPos(player.position());
        player.serverLevel().addFreshEntity(bee); player.serverLevel().addFreshEntity(plant);
        try {
            run(player, bee, plant);
            System.out.println("GRAB_QTE_SERVER_PASS: " + checks + " checks");
        } finally { bee.discard(); plant.discard(); }
    }

    public static void run(Player player, ChangedEntity bee, ChangedEntity plant) throws Exception {
        var position = player.position();
        boolean physics = player.noPhysics;
        var previousGrabber = ((LivingEntityDataExtension)player).getGrabbedBy();
        BondedSuitService.ensureGrabAbility(bee); BondedSuitService.ensureGrabAbility(plant);
        var first = BondedSuitService.ability(bee);
        var second = BondedSuitService.ability(plant);
        try {
            for (var hold : java.util.List.of(first, second)) {
                hold.tickIdle(); // Addon's hint used to crash even with no active grab.
                check(hold.grabbedEntity == null, "Idle ability has no invented hold or QTE");
                hold.grabbedEntity = player;
                hold.grabStrength = 1;
                hold.ticksGrabbed = 20;
                hold.currentEscapeKey = KeyReference.MOVE_FORWARD;
                hold.escapeKeyRandom = null;
                hold.escapeKeys.queueKeyState(KeyReference.MOVE_FORWARD, true);
                hold.handleEscape(); // Original crash path: input advances a null generator.
                check(player.level().isClientSide()
                                ? hold.currentEscapeKey == null && hold.escapeKeyRandom == null
                                : hold.currentEscapeKey != null && hold.escapeKeyRandom != null,
                        "Incomplete QTE waits on client and reinitializes on server");
                hold.initializeEscape(451L);
                hold.entity.getEntity().tickCount++;
                var expected = hold.currentEscapeKey;
                hold.escapeKeys.reset(false);
                hold.escapeKeys.queueKeyState(expected, true);
                hold.tickIdle();
                check(hold.lastEscapeKey == expected && hold.currentEscapeKey != null,
                        "Real input advances an initialized native escape sequence");
                hold.releaseEntity(false);
                hold.tickIdle();
                check(hold.grabbedEntity == null && hold.currentEscapeKey == null,
                        "Released hold keeps native cleanup and idle ticking");
            }

            first.grabbedEntity = player;
            first.grabStrength = .8F;
            first.ticksGrabbed = 20;
            first.initializeEscape(830L);
            var generator = first.escapeKeyRandom;
            var key = first.currentEscapeKey;
            second.grabbedEntity = player;
            var steal = GrabEntityAbilityInstance.class.getDeclaredMethod(
                    "stealGrabFrom", GrabEntityAbilityInstance.class);
            steal.setAccessible(true);
            steal.invoke(second, first);
            check(second.escapeKeyRandom == generator && second.currentEscapeKey == key,
                    "Native handoff preserves the existing key and random-sequence position");
            check(((net.parkabird.changedsynergy.ai.GrabEscapeState)second).changedSynergy$escapeSeed() == 830L
                            && ((net.parkabird.changedsynergy.ai.GrabEscapeState)second).changedSynergy$escapeDraws() == 1,
                    "Handoff also preserves recoverable seed and draw position");
            check(first.grabbedEntity == null && first.escapeKeyRandom == null,
                    "Previous grabber is fully released without clearing the new sequence");
            second.escapeKeys.queueKeyState(key, true);
            second.handleEscape();
            check(second.lastEscapeKey == key && second.currentEscapeKey != null,
                    "Escape input works immediately after native handoff");
            steal.invoke(second, second);
            check(second.escapeKeyRandom == generator,
                    "Self-handoff leaves the active sequence untouched");
            if (player instanceof ServerPlayer serverPlayer) checkFriendlyFallback(serverPlayer, first);
        } finally {
            first.releaseEntity(false); second.releaseEntity(false);
            ((LivingEntityDataExtension)player).setGrabbedBy(previousGrabber);
            player.noPhysics = physics;
            player.setPos(position);
        }
    }

    private static void checkFriendlyFallback(ServerPlayer player, GrabEntityAbilityInstance hold) {
        var mob=(ChangedEntity)hold.entity.getEntity();
        hold.releaseEntity(false);
        net.parkabird.changedsynergy.ai.LatexSocialMemory.beginFriendlySocialHug(mob,player,200);
        net.parkabird.changedsynergy.compat.ChangedAddonCompat.configureFriendlyGrab(hold,true);
        hold.grabbedEntity=player;hold.grabStrength=1;hold.grabbedHasControl=true;hold.suited=false;
        hold.attackDown=hold.useDown=false;
        ((LivingEntityDataExtension)player).setGrabbedBy(null);
        var health=player.getHealth();
        for(int i=0;i<12;i++){mob.tickCount++;net.parkabird.changedsynergy.ai.GrabEscapeService.tick(hold);}
        check(hold.currentEscapeKey!=null&&hold.escapeKeyRandom!=null&&!hold.grabbedHasControl,
                "Friendly NoAI arm hold has QTE without any AI idle updates");
        check(((LivingEntityDataExtension)player).getGrabbedBy()==mob,"Fallback repairs the native player input link");
        int ticks=hold.ticksGrabbed;
        net.parkabird.changedsynergy.ai.GrabEscapeService.tick(hold);hold.handleEscape();
        check(hold.ticksGrabbed==ticks,"Native and fallback escape cannot double-tick a player hold");
        for(int i=0;i<100&&hold.grabbedEntity!=null;i++){
            mob.tickCount++;hold.ticksUnpressed=5;
            hold.escapeKeys.queueKeyState(hold.currentEscapeKey,true);
            net.parkabird.changedsynergy.ai.GrabEscapeService.tick(hold);
            if(hold.grabbedEntity==null)break;
            for(var key:new KeyReference[]{KeyReference.MOVE_FORWARD,KeyReference.MOVE_BACKWARD,KeyReference.MOVE_LEFT,KeyReference.MOVE_RIGHT})hold.escapeKeys.queueKeyState(key,false);
            mob.tickCount++;net.parkabird.changedsynergy.ai.GrabEscapeService.tick(hold);
        }
        check(hold.grabbedEntity==null&&!player.noPhysics&&((LivingEntityDataExtension)player).getGrabbedBy()==null,
                "Friendly QTE completely releases even with AI idle ticking absent");
        check(player.getHealth()==health&&!player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                        &&!player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),"Friendly escape has no damage or release debuffs");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++; System.out.println("GRAB_QTE_CHECK: " + message);
    }
}
