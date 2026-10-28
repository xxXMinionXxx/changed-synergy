package net.parkabird.changedsynergy.regression;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.parkabird.changedsynergy.ai.BondedSuitService;
import net.parkabird.changedsynergy.ai.CreaturePeerInteractionService;
import net.parkabird.changedsynergy.ai.LatexSocialMemory;
import net.parkabird.changedsynergy.client.ChasePlayClient;
import net.parkabird.changedsynergy.network.*;
import static net.parkabird.changedsynergy.network.ChasePlayStatePacket.*;

/** Real native right-click, held-pat refresh and physical hold/toggle crouch messages. */
final class ChasePlayRulesLiveRegression {
    private static int mode,stage,ticks,wait,presses,carrierId;
    private static Boolean oldToggle;
    private static CompletableFuture<Integer> setup;
    private static CompletableFuture<Boolean> task;

    static boolean tick(Minecraft mc)throws Exception {
        if(++ticks>1400)throw new AssertionError("Live chase rules timed out: mode="+mode+" stage="+stage);
        if(oldToggle==null)oldToggle=mc.options.toggleCrouch().get();
        if(stage==0) {
            mc.setScreen(null);mc.options.toggleCrouch().set(false);mc.options.keyShift.setDown(false);
            var id=mc.player.getUUID();
            setup=mc.getSingleplayerServer().submit(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                player.connection.teleport(80.5,80,80.5,0,0);player.setDeltaMovement(Vec3.ZERO);player.setOnGround(true);
                player.setLastHurtByMob(null);player.setLastHurtMob(null);
                var npc=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(player.serverLevel());
                npc.setPos(81.4,80,80.5);npc.setOnGround(true);player.serverLevel().addFreshEntity(npc);
                LatexSocialMemory.addBond(npc,player);
                if(!CreaturePeerInteractionService.invitePlayer(npc,player)||!CreaturePeerInteractionService.active(player))
                    throw new AssertionError("Live rule fixture cannot invite its player");
                return npc.getId();
            });
            stage=1;return false;
        }
        if(stage==1) {if(!setup.isDone())return false;carrierId=setup.join();stage=2;}
        var field=ChasePlayClient.class.getDeclaredField("current");field.setAccessible(true);
        var state=(ChasePlayStatePacket)field.get(null);
        var npc=mc.level.getEntity(carrierId);
        if(stage==2) {
            if(state==null||state.partnerId()!=carrierId||state.stage()!=INVITED||npc==null)return false;
            ChangedSynergyNetwork.CHANNEL.sendToServer(new ChasePlayInputPacket(state.sessionId(),mode<2?0:1));
            stage=3;return false;
        }
        if(stage==3) {
            if(state==null)throw new AssertionError("Live role choice unexpectedly ended play");
            if(mode>=2) {
                if(state.stage()<READY)return false;
                mc.options.toggleCrouch().set(mode==2);presses=0;press(mc);wait=0;stage=10;return false;
            }
            if(state.stage()!=CHASING)return false;
            var id=mc.player.getUUID();
            task=mc.getSingleplayerServer().submit(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                var creature=(ChangedEntity)player.serverLevel().getEntity(carrierId);
                try {
                    Object session=session(player);
                    var next=session.getClass().getDeclaredField("nextRepath");next.setAccessible(true);next.setLong(session,player.level().getGameTime()+200);
                } catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
                creature.getNavigation().stop();creature.setDeltaMovement(Vec3.ZERO);creature.setPos(player.position().add(.9,0,0));
                return true;
            });
            wait=0;stage=4;return false;
        }
        if(stage==4) {
            if(!task.isDone())return false;task.join();
            if(state==null)throw new AssertionError("Proximity completed a player catch without interaction");
            if(npc==null||mc.player.distanceToSqr(npc)>2||++wait<10)return false;
            if(mode==0) mc.gameMode.interact(mc.player,npc,InteractionHand.MAIN_HAND);
            else ChangedSynergyNetwork.CHANNEL.sendToServer(new PatAnimationControlPacket(carrierId,true,false));
            wait=0;stage=mode==0?20:5;return false;
        }
        if(stage==5) {
            if(++wait<12)return false;
            if(state==null)throw new AssertionError("Held pat refresh incorrectly caught the runner");
            mc.gameMode.interact(mc.player,npc,InteractionHand.MAIN_HAND);stage=20;return false;
        }
        if(stage==10) {
            if(++wait<8)return false;
            var id=mc.player.getUUID();
            task=mc.getSingleplayerServer().submit(()->{
                try {
                    var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);Object session=session(player);
                    if(session==null)return false;
                    var count=session.getClass().getDeclaredField("exitPresses");count.setAccessible(true);
                    return count.getInt(session)==presses;
                } catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
            });
            stage=11;return false;
        }
        if(stage==11) {
            if(!task.isDone())return false;
            if(!task.join())throw new AssertionError("Physical crouch presses were duplicated or lost: mode="+mode+" presses="+presses);
            press(mc);wait=0;stage=presses==3?20:10;return false;
        }
        if(stage==20) {
            if(state!=null)return false;
            var id=mc.player.getUUID();
            task=mc.getSingleplayerServer().submit(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                var creature=(ChangedEntity)player.serverLevel().getEntity(carrierId);
                boolean remembered=!creature.getPersistentData().getCompound("ChangedSynergyLife").getList("Playmates",10).isEmpty();
                boolean safe=!CreaturePeerInteractionService.active(player)&&BondedSuitService.ability(creature).grabbedEntity==null
                        &&!player.noPhysics&&remembered==(mode<2);
                creature.discard();return safe;
            });
            stage=21;return false;
        }
        if(stage==21) {
            if(!task.isDone())return false;
            if(!task.join())throw new AssertionError("Live catch/exit did not cleanly preserve its outcome: mode="+mode);
            if(++mode<4){stage=0;return false;}
            mc.options.toggleCrouch().set(false);mc.options.keyShift.setDown(false);mc.options.toggleCrouch().set(oldToggle);
            System.out.println("CHASE_PLAY_RULES_LIVE_PASS: real right-click catches; held-pat C2S and proximity do not win; three physical hold/toggle presses; repeat/release ignored; safe cleanup");
            return true;
        }
        return false;
    }
    private static Object session(net.minecraft.server.level.ServerPlayer player)throws ReflectiveOperationException {
        var field=CreaturePeerInteractionService.class.getDeclaredField("PARTICIPANTS");field.setAccessible(true);
        return ((Map<?,?>)field.get(null)).get(player.getUUID());
    }
    private static void press(Minecraft mc) {
        int key=mc.options.keyShift.getKey().getValue();mc.options.keyShift.setDown(true);
        ChasePlayClient.onKey(new InputEvent.Key(key,0,InputConstants.PRESS,0));
        ChasePlayClient.onKey(new InputEvent.Key(key,0,InputConstants.REPEAT,0));
        ChasePlayClient.onKey(new InputEvent.Key(key,0,InputConstants.RELEASE,0));
        if(!mc.options.toggleCrouch().get())mc.options.keyShift.setDown(false);
        presses++;
    }
}
