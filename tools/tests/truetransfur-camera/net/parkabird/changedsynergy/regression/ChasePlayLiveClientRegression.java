package net.parkabird.changedsynergy.regression;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.LivingEntityDataExtension;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.BondedSuitService;
import net.parkabird.changedsynergy.ai.CreaturePeerInteractionService;
import net.parkabird.changedsynergy.ai.LatexSocialMemory;
import net.parkabird.changedsynergy.ai.TakeoverService;
import net.parkabird.changedsynergy.client.ChasePlayClient;
import net.parkabird.changedsynergy.network.ChasePlayInputPacket;
import net.parkabird.changedsynergy.network.ChasePlayStatePacket;
import net.parkabird.changedsynergy.network.ChangedSynergyNetwork;
import net.parkabird.changedsynergy.network.FriendlySocialHugState;

/** Real C2S choice and native S2C ARMS/RELEASE, observed over unpaused client/server ticks. */
final class ChasePlayLiveClientRegression {
    private record Invite(int carrierId,int sessionId,Object form) {}
    private static int stage, ticks;
    private static Invite invite;
    private static CompletableFuture<Invite> setup;
    private static CompletableFuture<Boolean> finish;
    private static boolean sawCrouch, sawHug;

    static boolean tick(Minecraft mc) throws Exception {
        if(++ticks>600)throw new AssertionError("Live chase timed out: stage="+stage+" crouch="+sawCrouch+" hug="+sawHug);
        if(stage==0) {
            mc.setScreen(null);
            var id=mc.player.getUUID();
            setup=mc.getSingleplayerServer().submit(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                net.parkabird.changedsynergy.compat.TrueTransfurCompat.ensurePlayerControl(player);
                player.setGameMode(GameType.SURVIVAL);player.setHealth(player.getMaxHealth());
                player.setLastHurtByMob(null);player.setLastHurtMob(null);
                var level=player.serverLevel();
                for(int x=72;x<90;x++)for(int z=72;z<90;z++) {
                    level.setBlock(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState(),3);
                    for(int y=80;y<85;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
                }
                player.connection.teleport(80.5,80,80.5,0,0);player.setOnGround(true);
                var creature=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
                creature.setPos(81.4,80,80.5);creature.setOnGround(true);level.addFreshEntity(creature);
                LatexSocialMemory.addBond(creature,player);
                ChangedSynergyConfig.COMMON.chasePlayGames.set(true);ChangedSynergyConfig.COMMON.chasePlayShortHugs.set(true);
                if(!CreaturePeerInteractionService.invitePlayer(creature,player)||!CreaturePeerInteractionService.active(player))
                    throw new AssertionError("Live player did not receive a playable invitation");
                try {
                    var field=CreaturePeerInteractionService.class.getDeclaredField("PARTICIPANTS");field.setAccessible(true);
                    Object session=((Map<?,?>)field.get(null)).get(id);
                    var sessionId=session.getClass().getDeclaredField("id");sessionId.setAccessible(true);
                    return new Invite(creature.getId(),sessionId.getInt(session),ProcessTransfur.getPlayerTransfurVariantSafe(player)
                            .map(form->form.getParent()).orElse(null));
                } catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
            });
            stage=1;return false;
        }
        if(stage==1) {
            if(!setup.isDone())return false;
            invite=setup.join();stage=2;
        }
        if(stage==5) {
            if(!finish.isDone())return false;
            if(!finish.join()||!sawCrouch||!sawHug)throw new AssertionError("Live game did not preserve role visuals, health, player form and safe release");
            System.out.println("CHASE_PLAY_LIVE_CLIENT_PASS: real C2S role choice; remote NPC crouch; native ARMS and RELEASE; harmless player hold; clean physics and unchanged player form");
            return true;
        }
        if(!(mc.level.getEntity(invite.carrierId()) instanceof ChangedEntity carrier))return false;
        var field=ChasePlayClient.class.getDeclaredField("current");field.setAccessible(true);
        var state=(ChasePlayStatePacket)field.get(null);
        if(stage==2) {
            if(state==null||state.sessionId()!=invite.sessionId())return false;
            ChasePlayHudRegression.run(mc,state);
            if(BondedSuitService.ability(carrier)==null)throw new AssertionError("Client NPC is missing the native grab ability");
            ChangedSynergyNetwork.CHANNEL.sendToServer(new ChasePlayInputPacket(invite.sessionId(),ChasePlayInputPacket.JUMP));
            stage=3;return false;
        }
        var hold=BondedSuitService.ability(carrier);
        if(stage==3) {
            if(state==null)throw new AssertionError("Game ended before the native catch");
            sawCrouch|=carrier.getPose()==Pose.CROUCHING;
            if(hold.grabbedEntity!=mc.player)return false;
            if(hold.suited||hold.grabbedHasControl||!FriendlySocialHugState.isActive(carrier.getId(),mc.player.getId()))
                throw new AssertionError("Native ARMS packet did not arrive with harmless play-hug state: suited="+hold.suited
                        +" control="+hold.grabbedHasControl+" friendly="+FriendlySocialHugState.isActive(carrier.getId(),mc.player.getId())
                        +" clientStage="+(state==null?null:state.stage()));
            sawHug=true;stage=4;return false;
        }
        if(stage==4) {
            if(hold.grabbedEntity!=null||state!=null||mc.player.noPhysics)return false;
            if(((LivingEntityDataExtension)mc.player).getGrabbedBy()!=null)throw new AssertionError("Client passenger retained a stale carrier");
            var id=mc.player.getUUID();int carrierId=carrier.getId();
            finish=mc.getSingleplayerServer().submit(()->{
                var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                var creature=(ChangedEntity)player.serverLevel().getEntity(carrierId);
                boolean safe=creature!=null&&!CreaturePeerInteractionService.active(player)
                        &&BondedSuitService.ability(creature).grabbedEntity==null&&!TakeoverService.active(player)
                        &&ProcessTransfur.getPlayerTransfurVariantSafe(player).map(form->form.getParent()).orElse(null)==invite.form()&&!player.noPhysics
                        &&player.getHealth()==player.getMaxHealth();
                if(creature!=null)creature.discard();
                return safe;
            });
            stage=5;
        }
        return false;
    }
}
