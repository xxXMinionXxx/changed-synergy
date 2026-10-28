package net.parkabird.changedsynergy.ai;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.ChangedSynergyConfig;

@Mod.EventBusSubscriber(modid="changed_synergy")
public final class TaskNavigationRegression {
    private static int checks;
    @SubscribeEvent public static void run(ServerStartedEvent event) {
        var server=event.getServer();var level=server.overworld();long time=level.getGameTime();
        try {
            ChangedSynergyConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
            for(int x=-2;x<=40;x++)for(int z=-2;z<=12;z++)for(int y=89;y<=94;y++)
                level.setBlock(new BlockPos(x,y,z),(y==89?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
            for(var state:List.of(CreatureLifeMemory.RoutineState.GUARDING,CreatureLifeMemory.RoutineState.SCOUTING,CreatureLifeMemory.RoutineState.DELIVERING))
                routine(level,state);
            cacheReturn(level);routes(level);emergency(level);realBarrier(level);campIsolation(level);
            System.out.println("COMMUNITY_REGRESSION_PASS: "+checks+" failed-route backoff, real obstacle, cargo and paused-camp isolation checks");
        } catch(Throwable failure) {System.err.println("TASK_NAVIGATION_REGRESSION_FAIL");failure.printStackTrace();}
        finally {server.getWorldData().overworldData().setGameTime(time);server.halt(false);}
    }
    private static ChangedEntity mob(ServerLevel level) {
        var mob=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);mob.moveTo(2.5,90,2.5);mob.setOnGround(true);return mob;
    }
    private static CountingNavigation count(ChangedEntity mob,ServerLevel level) throws Exception {
        var nav=new CountingNavigation(mob,level);
        var field=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(Mob.class,"f_21344_");field.setAccessible(true);field.set(mob,nav);return nav;
    }
    private static void routine(ServerLevel level,CreatureLifeMemory.RoutineState state) throws Exception {
        var mob=mob(level);var nav=count(mob,level);var target=new BlockPos(32,90,2);
        for(int i=0;i<1000;i+=16)nav.moveTo(32.5,90,2.5,.3);
        int before=nav.calls;nav.calls=0;
        var cargo=new CompoundTag();cargo.put("Stack",new ItemStack(Items.BREAD,2).save(new CompoundTag()));cargo.putString("Kind","food");
        if(state==CreatureLifeMemory.RoutineState.DELIVERING)mob.getPersistentData().put("ChangedSynergyCarriedResource",cargo.copy());
        var goal=new CreatureRoutineGoal(mob);set(goal,"state",state);set(goal,"destination",Vec3.atBottomCenterOf(target));set(goal,"remainingTicks",1000);goal.start();
        for(int i=0;i<1000;i++){advance(level);goal.tick();if(i%80==0){goal.stop();set(goal,"state",state);set(goal,"destination",Vec3.atBottomCenterOf(target));set(goal,"remainingTicks",1000);goal.start();}}
        check(before>=60&&nav.calls<=9,"failed "+state+" path searches fall from "+before+" to "+nav.calls+" despite goal restarts");
        check(!goal.canContinueToUse(),"an impossible task releases movement instead of holding it for its entire travel budget");
        System.out.println("TASK_NAVIGATION_SEARCH_COUNTS: "+state+" old="+before+" new="+nav.calls+" per 1000 ticks");
        if(state==CreatureLifeMemory.RoutineState.DELIVERING)
            check(mob.getPersistentData().getCompound("ChangedSynergyCarriedResource").equals(cargo),"failed delivery preserves its exact cargo");
        goal.stop();
        if(state==CreatureLifeMemory.RoutineState.DELIVERING)check(CreatureLifeMemory.nextDecisionTick(mob)>=level.getGameTime()+200,"failed cargo delivery waits before trying again, with cargo retained");
        mob.discard();
    }
    private static void cacheReturn(ServerLevel level) throws Exception {
        var mob=mob(level);CreatureLifeMemory.setRole(mob,CreatureLifeMemory.GroupRole.GUARD);var nav=count(mob,level);
        var intruder=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"RouteIntruder"));
        intruder.connection=new ServerGamePacketListenerImpl(level.getServer(),new Connection(PacketFlow.SERVERBOUND),intruder);
        var post=new BlockPos(32,90,2);mob.moveTo(Vec3.atBottomCenterOf(post));CreatureCacheGuardService.begin(mob,intruder,post);
        CreatureCacheGuardService.standDownTowards(mob,intruder);CreatureCacheGuardService.tick(mob);mob.moveTo(2.5,90,2.5);mob.setOnGround(true);
        var goal=new CacheGuardReturnGoal(mob);check(goal.canUse(),"guard has a real completed defense to return from");goal.start();
        for(int i=0;i<261;i++){advance(level);goal.tick();}
        check(nav.calls<=4&&CreatureCacheGuardService.assignment(mob).isEmpty(),"blocked guard return backs off and still releases the completed assignment");
        goal.stop();mob.discard();intruder.discard();
    }
    private static void routes(ServerLevel level) throws Exception {
        var mob=mob(level);var nav=count(mob,level);var target=new BlockPos(20,90,2);
        var nearby=new CreatureRoutineGoal(mob);set(nearby,"state",CreatureLifeMemory.RoutineState.GATHERING);set(nearby,"destination",mob.position().add(2,0,0));set(nearby,"remainingTicks",100);nearby.start();
        check(nearby.canContinueToUse()&&nav.calls==0,"work already within interaction reach needs no new path and is not aborted");nearby.stop();
        for(int i=0;i<20;i++)check(!CreatureTaskNavigation.moveTo(mob,target,0,.3),"failed route reports failure");
        check(nav.calls==1,"same-tick retries share one failed path search");
        mob.moveTo(5.5,90,2.5);mob.setOnGround(true);CreatureTaskNavigation.moveTo(mob,target,0,.3);check(nav.calls==2,"actual displacement invalidates the negative cache");
        CreatureTaskNavigation.moveTo(mob,target.south(),0,.3);check(nav.calls==3,"new order target is not blocked by an old failure");
        nav.mode=1;level.getServer().getWorldData().overworldData().setGameTime(level.getGameTime()+240);
        check(CreatureTaskNavigation.moveTo(mob,target.south(),0,.3),"a reopened route is retried and accepted");
        int calls=nav.calls;for(int i=0;i<50;i++)check(CreatureTaskNavigation.moveTo(mob,target.south(),0,.3),"active route remains usable");
        check(nav.calls==calls,"an active unchanged route is reused without path searches");
        nav.stop();nav.mode=2;
        check(!CreatureTaskNavigation.moveTo(mob,target.east(),0,.3),"a partial path ending at the creature is rejected");
        nav.mode=3;level.getChunkAt(new BlockPos(80,90,2));
        check(CreatureTaskNavigation.moveTo(mob,new BlockPos(80,90,2),0,.3),"a partial path making progress still supports a distant delivery");
        nav.stop();var unloaded=new BlockPos(10000000,90,10000000);calls=nav.calls;
        check(!level.hasChunkAt(unloaded)&&!CreatureTaskNavigation.moveTo(mob,unloaded,0,.3)&&nav.calls==calls&&!level.hasChunkAt(unloaded),"old unloaded homes never force chunks or invoke pathfinding");
        mob.discard();
    }
    private static void realBarrier(ServerLevel level) throws Exception {
        var mob=mob(level);var nav=count(mob,level);nav.mode=4;
        for(var pos:BlockPos.betweenClosed(1,90,1,3,93,3))if(pos.getX()!=2||pos.getZ()!=2)level.setBlock(pos,Blocks.OBSIDIAN.defaultBlockState(),3);
        int routed=0;for(int i=0;i<1000;i++){advance(level);if(CreatureTaskNavigation.moveTo(mob,new BlockPos(24,90,2),0,.3))routed++;}
        check(routed==0&&nav.calls<=9,"a real sealed enclosure cannot cause continuous expensive searches: "+nav.calls);
        mob.discard();
    }
    private static void emergency(ServerLevel level) throws Exception {
        var mob=mob(level);var nav=count(mob,level);var target=new BlockPos(20,90,2);
        check(!CreatureTaskNavigation.moveTo(mob,target,0,.3),"ordinary work initially fails");
        nav.mode=1;var escape=new PlayerOutpostGoal.FireEscape(mob);escape.start();
        check(CreatureTaskNavigation.moveTo(mob,target,0,.3)&&nav.calls==2,"an emergency exit bypasses an earlier work-route cooldown");
        escape.stop();mob.discard();
    }
    private static void campIsolation(ServerLevel level) throws Exception {
        var owner=UUID.randomUUID();boolean enabled=ChangedSynergyConfig.COMMON.playerOutposts.get();
        try {
            for(var role:List.of(CreatureLifeMemory.GroupRole.SCOUT,CreatureLifeMemory.GroupRole.GUARD,CreatureLifeMemory.GroupRole.PROVISIONER)) {
                var mob=mob(level);CreatureLifeMemory.setRole(mob,role);var nav=count(mob,level);
                var original=CreatureCommunityData.bind(mob).orElseThrow();
                mob.getPersistentData().putUUID("ChangedSynergyPlayerOutpostOwner",owner);
                CreatureCommunityData.detach(mob);
                for(boolean paused:new boolean[]{false,true}) {
                    ChangedSynergyConfig.COMMON.playerOutposts.set(!paused);
                    check(PlayerOutpostService.hasAssignment(mob),"saved camp ownership persists while paused="+paused);
                    check(CreatureCommunityData.bind(mob).isEmpty()&&!new CreatureRoutineGoal(mob).canUse()&&!new CacheGuardReturnGoal(mob).canUse(),role+" camp resident does not resume wild-home work while paused="+paused);
                    check(!mob.getPersistentData().hasUUID("ChangedSynergyCommunityId")&&nav.calls==0,"camp isolation does not recreate the old community or calculate its route");
                }
                mob.discard();
            }
        } finally {ChangedSynergyConfig.COMMON.playerOutposts.set(enabled);}
    }
    private static void advance(ServerLevel level){level.getServer().getWorldData().overworldData().setGameTime(level.getGameTime()+1);}
    private static void set(Object object,String name,Object value)throws Exception{var field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value);}
    private static void check(boolean result,String message){if(!result)throw new AssertionError(message);checks++;}
    private static final class CountingNavigation extends GroundPathNavigation {
        int calls,mode;private BlockPos requested;
        CountingNavigation(Mob mob,ServerLevel level){super(mob,level);}
        @Override public Path createPath(BlockPos target,int reach) {
            calls++;if(mode==0)return null;if(mode==4)return super.createPath(target,reach);
            requested=target;var start=mob.blockPosition();
            return new Path(mode==2?List.of(new Node(start.getX(),start.getY(),start.getZ())):
                    List.of(new Node(start.getX(),start.getY(),start.getZ()),new Node(mode==3?start.getX()+10:target.getX(),target.getY(),target.getZ())),target,mode==1);
        }
        @Override public BlockPos getTargetPos(){return mode==4?super.getTargetPos():requested;}
    }
}
