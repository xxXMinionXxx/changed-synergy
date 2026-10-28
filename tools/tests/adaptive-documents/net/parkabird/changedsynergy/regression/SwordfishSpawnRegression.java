package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.init.ChangedSynergyEntities;

@Mod.EventBusSubscriber(modid="changed_synergy",value=Dist.DEDICATED_SERVER)
public final class SwordfishSpawnRegression {
    private static MinecraftServer server;
    private static int ticks,checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    @SubscribeEvent public static void prepare(ServerStartedEvent event){
        server=event.getServer();var level=server.overworld();
        for(int x=84;x<=92;x++)for(int z=84;z<=92;z++)for(int y=45;y<level.getMaxBuildHeight();y++)
            level.setBlock(new BlockPos(x,y,z),(y<level.getSeaLevel()?Blocks.WATER:Blocks.AIR).defaultBlockState(),3);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"fillbiome 84 44 84 92 64 92 minecraft:deep_cold_ocean");
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||server==null||++ticks<60)return;
        try{
            var level=server.overworld();var fish=ChangedSynergyEntities.LATEX_SWORDFISH.get();var wet=new BlockPos(88,50,88);
            check(level.getSeaLevel()>60,"normal ocean world with sea level");
            var entries=level.getBiome(wet).value().getMobSettings().getMobs(fish.getCategory()).unwrap().stream().filter(e->e.type==fish).toList();
            check(entries.size()==1&&entries.get(0).getWeight().asInt()==5&&entries.get(0).minCount==1&&entries.get(0).maxCount==2,"one weighted spawn entry, unchanged pack size");
            for(long time:new long[]{6000,18000}){
                level.setDayTime(time);level.updateSkyBrightness();
                for(var reason:new MobSpawnType[]{MobSpawnType.NATURAL,MobSpawnType.CHUNK_GENERATION}){
                    var random=RandomSource.create(234);int accepted=0;
                    for(int i=0;i<10000;i++)if(SpawnPlacements.checkSpawnRules(fish,level,reason,wet,random))accepted++;
                    check(accepted>=200&&accepted<=300,"1-in-40 rarity during "+time+" "+reason+": "+accepted);
                    System.out.println("SWORDFISH_SPAWN_RATE: "+time+" "+reason+" accepted="+accepted+"/10000");
                }
            }
            var random=RandomSource.create(789);
            for(int i=0;i<100;i++)check(SpawnPlacements.checkSpawnRules(fish,level,MobSpawnType.SPAWNER,wet,random),"spawner remains reliable");
            check(!SpawnPlacements.checkSpawnRules(fish,level,MobSpawnType.SPAWNER,new BlockPos(88,61,88),random),"shallow water excluded");
            check(!SpawnPlacements.checkSpawnRules(fish,level,MobSpawnType.SPAWNER,new BlockPos(88,75,88),random),"air excluded");
            server.setDifficulty(Difficulty.PEACEFUL,true);
            check(!SpawnPlacements.checkSpawnRules(fish,level,MobSpawnType.SPAWNER,wet,random),"peaceful remains excluded");
            server.setDifficulty(Difficulty.NORMAL,true);level.setDayTime(6000);level.updateSkyBrightness();
            var observer=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"SpawnObserver"));
            observer.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),observer);
            observer.moveTo(125.5,50,88.5);level.addNewPlayer(observer);
            level.setDefaultSpawnPos(new BlockPos(10000,80,10000),0);level.random.setSeed(1234);
            var area=new net.minecraft.world.phys.AABB(wet).inflate(16);int attempts=0;
            for(;attempts<8192&&level.getEntitiesOfClass(net.parkabird.changedsynergy.entity.LatexSwordfish.class,area).isEmpty();attempts++)
                net.minecraft.world.level.NaturalSpawner.spawnCategoryForPosition(fish.getCategory(),level,wet);
            check(attempts<8192,"vanilla weighted natural spawning still creates swordfish: "+attempts);
            System.out.println("SWORDFISH_SPAWN_REGRESSION_PASS: "+checks+" checks; natural engine creates swordfish after "+attempts+" attempts");
        }catch(Throwable failure){System.err.println("SWORDFISH_SPAWN_REGRESSION_FAIL");failure.printStackTrace();}
        finally{server.halt(false);server=null;}
    }
}
