package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.TransfurContext;
import net.ltxprogrammer.changed.entity.ai.LatexAssimilationDecision;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.process.TransfurEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.init.*;

final class October22Regression {
    private static int checks;
    static void prepareNaturalHabitat(MinecraftServer server) {
        var level=server.overworld();
        for(int x=84;x<=92;x++)for(int z=84;z<=92;z++)for(int y=45;y<level.getMaxBuildHeight();y++)level.setBlock(new net.minecraft.core.BlockPos(x,y,z),(y<level.getSeaLevel()?net.minecraft.world.level.block.Blocks.WATER:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState(),3);
        for(int x=104;x<=112;x++)for(int z=104;z<=112;z++)for(int y=79;y<level.getMaxBuildHeight();y++)level.setBlock(new net.minecraft.core.BlockPos(x,y,z),(y==79?net.minecraft.world.level.block.Blocks.GRASS_BLOCK:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState(),3);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"fillbiome 84 44 84 92 64 92 minecraft:deep_ocean");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"fillbiome 104 76 104 112 84 112 minecraft:forest");
    }
    private static void naturalSpawns(MinecraftServer server) {
        var level=server.overworld();long time=level.getDayTime();
        var fish=ChangedSynergyEntities.LATEX_SWORDFISH.get();var moth=ChangedSynergyEntities.LATEX_SILK_MOTH.get();
        var registry=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        for(var entry:java.util.List.of(net.minecraft.world.level.biome.Biomes.DEEP_OCEAN,net.minecraft.world.level.biome.Biomes.DEEP_COLD_OCEAN,net.minecraft.world.level.biome.Biomes.DEEP_LUKEWARM_OCEAN))
            check(registry.getOrThrow(entry).getMobSettings().getMobs(fish.getCategory()).unwrap().stream().anyMatch(s->s.type==fish&&s.minCount==1&&s.maxCount==2),"swordfish is in deep ocean spawn table: "+entry.location());
        check(registry.getOrThrow(net.minecraft.world.level.biome.Biomes.FOREST).getMobSettings().getMobs(moth.getCategory()).unwrap().stream().anyMatch(s->s.type==moth),"silk moth is in forest spawn table");
        var wet=new net.minecraft.core.BlockPos(88,50,88);var ground=new net.minecraft.core.BlockPos(108,80,108);
        try {
            level.setDayTime(18000);level.updateSkyBrightness();
            var random=net.minecraft.util.RandomSource.create(1234);
            int fishPassed=0,mothPassed=0;
            for(int i=0;i<4096;i++) {
                if(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(fish,level,net.minecraft.world.entity.MobSpawnType.NATURAL,wet,random))fishPassed++;
                if(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(moth,level,net.minecraft.world.entity.MobSpawnType.NATURAL,ground,random))mothPassed++;
            }
            check(fishPassed>0,"real natural spawn predicate accepts deep-water swordfish: "+fishPassed+" sea="+level.getSeaLevel()+" sky="+level.canSeeSkyFromBelowWater(wet)+" light="+level.getMaxLocalRawBrightness(wet));
            check(fishPassed>50&&fishPassed<160,"deep-ocean swordfish retain the aquatic 1-in-40 natural rarity: "+fishPassed);
            check(mothPassed>0,"real natural spawn predicate accepts forest moth: "+mothPassed);
            var fishEntity=fish.create(level);fishEntity.moveTo(88.5,50,88.5);
            check(fishEntity.checkSpawnRules(level,net.minecraft.world.entity.MobSpawnType.NATURAL)&&fishEntity.checkSpawnObstruction(level),"swordfish instance permits natural spawn and water obstruction");fishEntity.discard();
            check(!net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(fish,level,net.minecraft.world.entity.MobSpawnType.NATURAL,ground,random),"swordfish rejects dry land");
            level.setDayTime(6000);level.updateSkyBrightness();
            int dayFish=0;for(int i=0;i<4096;i++)if(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(fish,level,net.minecraft.world.entity.MobSpawnType.NATURAL,wet,random))dayFish++;
            check(dayFish>50&&dayFish<160,"daylight permits deep-ocean swordfish at the same low rate: "+dayFish);
            for(int i=0;i<100;i++)check(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(fish,level,net.minecraft.world.entity.MobSpawnType.SPAWNER,wet,random),"spawner checks are not reduced by natural rarity");
            check(!net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(fish,level,net.minecraft.world.entity.MobSpawnType.NATURAL,new net.minecraft.core.BlockPos(88,60,88),random),"shallow water does not satisfy swordfish depth");
            int dayMoths=0;for(int i=0;i<512;i++)if(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(moth,level,net.minecraft.world.entity.MobSpawnType.NATURAL,ground,random))dayMoths++;
            check(dayMoths>0,"owned species share the Changed daylight exception");
            var observer=player(server,"SpawnObserver");observer.moveTo(125.5,50,88.5);level.addNewPlayer(observer);
            var spawn=level.getSharedSpawnPos();float angle=level.getSharedSpawnAngle();level.setDefaultSpawnPos(new net.minecraft.core.BlockPos(10000,80,10000),0);
            try {
                level.random.setSeed(1234);var area=new net.minecraft.world.phys.AABB(wet).inflate(16);
                boolean spawned=false;int attempts=0;
                for(;attempts<4096&&!spawned;attempts++) {
                    net.minecraft.world.level.NaturalSpawner.spawnCategoryForPosition(fish.getCategory(),level,wet);
                    spawned=!level.getEntitiesOfClass(net.parkabird.changedsynergy.entity.LatexSwordfish.class,area).isEmpty();
                }
                check(spawned,"actual vanilla natural-spawn engine creates a swordfish from the deep-ocean weighted table after "+attempts+" attempts");
                System.out.println("NATURAL_SWORDFISH_CREATED: "+attempts+" weighted attempts");
                for(var mob:level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,area))mob.discard();
            } finally {level.setDefaultSpawnPos(spawn,angle);removePlayer(server,observer);}
            System.out.println("NATURAL_SPAWN_CHECK: night swordfish="+fishPassed+" moth="+mothPassed+" /4096; wetBiome="+level.getBiome(wet).unwrapKey());
        } finally {level.setDayTime(time);level.updateSkyBrightness();}
    }
    static void run(MinecraftServer server) throws Exception {
        swimming(server);roles(server);gender(server);npcHit(server);
        if(server.overworld().getSeaLevel()>0)naturalSpawns(server);
        System.out.println("OCTOBER22_REGRESSION_PASS: "+checks+" gender continuity, wanderer-only roles and NPC player contact checks");
    }
    private static void swimming(MinecraftServer server) {
        var type=ChangedSynergyEntities.LATEX_SWORDFISH.get();var swim=net.minecraftforge.common.ForgeMod.SWIM_SPEED.get();
        for(double saved:new double[]{1.48,2.4}) {
            var fish=type.create(server.overworld());fish.getAttribute(swim).setBaseValue(saved);fish.setHealth(17);
            var snapshot=fish.saveWithoutId(new CompoundTag());var copy=type.create(server.overworld());copy.load(snapshot);
            check(Math.abs(copy.getAttribute(swim).getBaseValue()-(saved==1.48?1.85:saved))<.00001&&copy.getHealth()==17,"NPC save migration preserves custom speed and health");fish.discard();copy.discard();
            var player=player(server,"SwimProbe");ProcessTransfur.setPlayerTransfurVariant(player,ChangedSynergyTransfurVariants.LATEX_SWORDFISH.get(),null,1,false);
            var variant=ProcessTransfur.getPlayerTransfurVariant(player);var tag=variant.save();tag.getCompound("newAttributes").putDouble("forge:swim_speed",saved);variant.load(tag);
            check(Math.abs(variant.save().getCompound("newAttributes").getDouble("forge:swim_speed")-(saved==1.48?1.85:saved))<.00001,"player saved attribute map gains the new default without resetting customized speed");removePlayer(server,player);
        }
    }
    private static void roles(MinecraftServer server) {
        for(var type:java.util.List.of(ChangedEntities.MILK_PUDDING.get(),ChangedEntities.FERAL_LATEX_SHARK.get())) {
            var mob=type.create(server.overworld());
            for(var role:CreatureLifeMemory.GroupRole.values()) {
                CreatureLifeMemory.setRole(mob,role);
                check(CreatureLifeMemory.role(mob)==CreatureLifeMemory.GroupRole.WANDERER,"restricted species cannot be assigned "+role);
                var life=mob.getPersistentData().getCompound("ChangedSynergyLife");life.putString("GroupRole",role.id());life.putString("Routine","gathering");
                CreatureLifeMemory.ensure(mob);
                check(CreatureLifeMemory.role(mob)==CreatureLifeMemory.GroupRole.WANDERER,"saved profession is normalized for "+type);
            }
            check(!new CreatureRoutineGoal(mob).canUse(),"wanderer does not enter community jobs");
            var copy=type.create(server.overworld());copy.load(mob.saveWithoutId(new CompoundTag()));
            check(CreatureLifeMemory.role(copy)==CreatureLifeMemory.GroupRole.WANDERER,"wanderer survives save/reload");mob.discard();copy.discard();
        }
        var legacy=ChangedSynergyEntities.LATEX_SILK_MOTH.get().create(server.overworld());
        var life=new CompoundTag();life.putInt("Version",2);life.putString("GroupRole","wanderer");legacy.getPersistentData().put("ChangedSynergyLife",life);
        check(CreatureLifeMemory.role(legacy)==CreatureLifeMemory.GroupRole.SCOUT,"unrelated legacy prototype roles retain their original migration");legacy.discard();
    }
    @SuppressWarnings({"rawtypes","unchecked"}) private static void gender(MinecraftServer server) {
        boolean takeover=ChangedSynergyConfig.COMMON.takeoverEnabled.get();ChangedSynergyConfig.COMMON.takeoverEnabled.set(false);
        boolean origins=server.overworld().getGameRules().getBoolean(ChangedSynergyGameRules.NATIVE_ORIGINS);
        server.overworld().getGameRules().getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(true,server);
        boolean keep=server.overworld().getGameRules().getBoolean(net.ltxprogrammer.changed.init.ChangedGameRules.RULE_KEEP_BRAIN);
        server.overworld().getGameRules().getRule(net.ltxprogrammer.changed.init.ChangedGameRules.RULE_KEEP_BRAIN).set(true,server);
        try {
            for(var form:java.util.List.of(ChangedTransfurVariants.WHITE_LATEX_WOLF_MALE.get(),ChangedTransfurVariants.DARK_LATEX_WOLF_MALE.get()))
                for(boolean absorption:new boolean[]{false,true}) {
                    var player=player(server,"GenderProbe");var source=form.getEntityType().create(server.overworld());
                    player.moveTo(60,84,60);source.moveTo(61,84,60);server.overworld().addFreshEntity(source);
                    try {
                        var decision=LatexAssimilationDecision.strong(absorption?LatexAssimilationDecision.Method.ABSORPTION:LatexAssimilationDecision.Method.REPLICATION,
                                (TransfurVariant)form,TransfurContext.npcLatexAttack(source),100F);
                        var event=new TransfurEvents.LatexAssimilationDecisionEvent(player,decision);InvoluntaryTransfurNegotiation.observeDecision(event);
                        ProcessTransfur.setPlayerTransfurVariant(player,form,null,1F,false);
                        event.getDecision().postTransfurListener().accept(IAbstractChangedEntity.forPlayer(player));
                        check(negotiable(player,source,absorption),"genuine assimilation starts negotiable: "+form.getFormId()+" absorption="+absorption
                                +" enabled="+InvoluntaryTransfurNegotiation.ordinaryReversalEnabled()+" takeover="+TakeoverService.active(player)
                                +" blocked="+net.parkabird.changedsynergy.compat.TrueTransfurCompat.blocksReversal(player)
                                +" alive="+source.isAlive()+" removed="+source.isRemoved()+" sourceMarker="+source.getPersistentData().getCompound(InvoluntaryTransfurNegotiation.SOURCE_ROOT));
                        var original=data(player).copy();data(player).putInt("Progress",21);data(player).putInt("Attempts",2);
                        ChangedAbilities.SWITCH_GENDER.get().startUsing(IAbstractChangedEntity.forPlayer(player));
                        var opposite=ChangedTransfurVariants.Gendered.getOpposite(form).orElseThrow();
                        check(ProcessTransfur.getPlayerTransfurVariant(player).getParent()==opposite,"native gender ability changes the form");
                        check(negotiable(player,source,absorption)&&data(player).getInt("Progress")==21&&data(player).getInt("Attempts")==2
                                &&data(player).getUUID("CaptureId").equals(original.getUUID("CaptureId")),"gender change retains the same claim and progress");
                        InvoluntaryTransfurNegotiation.onPlayerReady(player);
                        check(negotiable(player,source,absorption),"login validation accepts the updated result form");
                        ChangedAbilities.SWITCH_GENDER.get().startUsing(IAbstractChangedEntity.forPlayer(player));
                        check(negotiable(player,source,absorption)&&ProcessTransfur.getPlayerTransfurVariant(player).getParent()==form,"switching back also retains negotiation");
                        ProcessTransfur.setPlayerTransfurVariant(player,opposite,null,1F,false);
                        check(data(player)==null,"direct replacement still retires an old claim outside the gender ability");
                    } finally {InvoluntaryTransfurNegotiation.abandonForSecondaryTransfur(player);removePlayer(server,player);source.discard();}
                }
        } finally {ChangedSynergyConfig.COMMON.takeoverEnabled.set(takeover);server.overworld().getGameRules().getRule(net.ltxprogrammer.changed.init.ChangedGameRules.RULE_KEEP_BRAIN).set(keep,server);server.overworld().getGameRules().getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(origins,server);}
    }
    private static boolean negotiable(ServerPlayer player,ChangedEntity source,boolean absorption) {
        return absorption?InvoluntaryTransfurNegotiation.canNegotiateAbsorption(player):InvoluntaryTransfurNegotiation.canNegotiate(player,source);
    }
    private static CompoundTag data(ServerPlayer player) {
        var persisted=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return persisted.contains("ChangedSynergyTransfurNegotiation")?persisted.getCompound("ChangedSynergyTransfurNegotiation"):null;
    }
    @SuppressWarnings("unchecked") private static ServerPlayer player(MinecraftServer server,String name) {
        var p=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),name));
        p.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),p);
        var identity=new net.parkabird.changedsynergy.origin.NativeOriginData.Identity();identity.state="human";
        net.parkabird.changedsynergy.origin.NativeOriginData.get(server).players.put(p.getUUID(),identity);
        var online=(java.util.Map<UUID,ServerPlayer>)net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
                net.minecraft.server.players.PlayerList.class,server.getPlayerList(),"f_11197_");online.put(p.getUUID(),p);
        net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class,p,0,"f_8921_");
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);return p;
    }
    private static void npcHit(MinecraftServer server) {
        var level=server.overworld();var player=player(server,"RushTarget");
        var fish=ChangedSynergyEntities.LATEX_SWORDFISH.get().create(level);
        player.moveTo(12.5,80,8.5);level.addNewPlayer(player);fish.moveTo(8.5,80,8.5);level.addFreshEntity(fish);fish.setNoAi(true);fish.baseTick();
        try {
            LatexSocialMemory.markProvoked(fish,player);fish.setTarget(player);fish.setYRot(0);fish.setDeltaMovement(Vec3.ZERO);
            check(fish.canAttack(player),"hostile NPC recognizes the survival player as a legal target");
            var rush=fish.getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());rush.startUsing();
            check(rush.direction().dot(player.getBoundingBox().getCenter().subtract(fish.getBoundingBox().getCenter()).normalize())>.999,"NPC aims at the target rather than its previous body yaw");
            float health=player.getHealth();fish.setPos(10.9,80,8.5);rush.tickIdle();
            check(player.getHealth()<health,"NPC sword contact damages the human before the bodies overlap: "+health+" -> "+player.getHealth());
            float hurt=player.getHealth();player.invulnerableTime=0;rush.tickIdle();check(player.getHealth()==hurt,"one NPC rush cannot damage the same player twice");
        } finally {fish.discard();removePlayer(server,player);}
    }
    @SuppressWarnings("unchecked") private static void removePlayer(MinecraftServer server,ServerPlayer player) {
        var online=(java.util.Map<UUID,ServerPlayer>)net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
                net.minecraft.server.players.PlayerList.class,server.getPlayerList(),"f_11197_");online.remove(player.getUUID());player.discard();
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
}
