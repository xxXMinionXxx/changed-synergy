package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedItems;
import net.ltxprogrammer.changed.item.Syringe;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.init.*;
import net.parkabird.changedsynergy.origin.NativeOriginCatalog;

@Mod.EventBusSubscriber(modid="changed_synergy",value=Dist.DEDICATED_SERVER)
public final class SwordfishServerRegression {
    private static int checks;
    private static net.parkabird.changedsynergy.entity.LatexSilkMoth restingMoth;
    private static net.minecraft.server.MinecraftServer testServer;
    private static int liveTicks;
    private static final BlockPos LAMP=new BlockPos(38,82,34);
    private static ServerStartedEvent pendingStart;
    private static int preparationTicks;
    @SubscribeEvent public static void prepare(ServerStartedEvent event) {
        var server=event.getServer();var level=server.overworld();
            for (int x=0;x<20;x++) for(int z=0;z<20;z++) for(int y=79;y<=84;y++)
                level.setBlock(new BlockPos(x,y,z),(y==79?Blocks.STONE:y<=82?Blocks.WATER:Blocks.AIR).defaultBlockState(),3);
            for(int y=85;y<level.getMaxBuildHeight();y++)level.setBlock(new BlockPos(8,y,8),Blocks.AIR.defaultBlockState(),3);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"fillbiome 0 76 0 20 92 20 minecraft:forest");
        if(level.getSeaLevel()>0)October22Regression.prepareNaturalHabitat(server);
        pendingStart=event;
    }
    private static void run(ServerStartedEvent event) {
        try {
            ChangedSynergyConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
            var server=event.getServer(); var level=server.overworld();
            level.getGameRules().getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false,server);
            repeatRush(level);
            if(!(level.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource)) {
                // The older inventory/rescue arenas require an empty flat world; ocean spawning requires normal sea level.
                October22Regression.run(server);
                System.out.println("SWORDFISH_SERVER_REGRESSION_PASS: natural-ocean, gender, profession, swimming and NPC contact checks");return;
            }
            var fish=ChangedSynergyEntities.LATEX_SWORDFISH.get().create(level);
            fish.moveTo(8.5,80,8.5);level.addFreshEntity(fish);fish.baseTick();
            var eel=ChangedEntities.LATEX_EEL.get().create(level);
            for (var attribute : java.util.List.of(Attributes.MAX_HEALTH,Attributes.ATTACK_DAMAGE,Attributes.MOVEMENT_SPEED,Attributes.ARMOR))
                check(fish.getAttributeValue(attribute)==eel.getAttributeValue(attribute),"same aquatic attributes: "+attribute);
            check(Math.abs(fish.getAttributeValue(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get())
                    /eel.getAttributeValue(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get())-1.25)<.00001,"swordfish swims 25 percent faster than the eel baseline");
            check(HunterFaction.of(fish)==HunterFaction.AQUATIC,"aquatic faction tag");
            check(HunterArchetype.of(fish)==HunterArchetype.AQUATIC,"aquatic dialogue/behavior tag");
            check(CreatureSettlementService.usesOpenOceanHarvest(fish),"explicit open-ocean food/work mode");
            var form=ChangedSynergyTransfurVariants.LATEX_SWORDFISH.get();
            check(NativeOriginCatalog.allowed(form),"native origin selectable without other-mod toggle");
            check(NativeOriginCatalog.describe(level.registryAccess(),form).faction().equals("aquatic"),"origin faction");
            var biomes=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
            check(biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.DEEP_OCEAN).getMobSettings().getMobs(fish.getType().getCategory()).unwrap().stream().anyMatch(s->s.type==fish.getType()),"natural deep-ocean spawn");
            check(biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.FOREST).getMobSettings().getMobs(fish.getType().getCategory()).unwrap().stream().noneMatch(s->s.type==fish.getType()),"no forest spawn");
            for(var item:java.util.List.of(ChangedItems.LATEX_SYRINGE.get(),ChangedItems.LATEX_FLASK.get(),ChangedItems.LATEX_TIPPED_ARROW.get()))
                check(Syringe.setPureVariant(new ItemStack(item),form.getFormId()).hasTag(),"native transfur item NBT");
            ServerPlayer player=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"SwordfishTest"));
            player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);
            player.moveTo(8.5,80,8.5);player.setYRot(0);player.setXRot(0);player.baseTick();
            ProcessTransfur.setPlayerTransfurVariant(player,form,null,1,false);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getFoodData().setFoodLevel(20);
            var rush=IAbstractChangedEntity.forPlayer(player).getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());
            check(ChangedSynergyAbilities.SWORDFISH_RUSH.get().getCoolDown(IAbstractChangedEntity.forPlayer(player))==60,"player cooldown remains three seconds");
            check(rush!=null&&rush.canUse(),"wet player can charge");
            player.setDeltaMovement(Vec3.ZERO);rush.startUsing();
            check(Math.abs(player.getDeltaMovement().length()-2.25)<.00001,"Riptide II launch speed");
            check(rush.isDashing()&&!rush.canUse(),"no repeated activation during charge");
            check(player.getFoodData().getFoodLevel()==19,"successful rush costs half a hunger shank");
            rush.startUsing();check(player.getFoodData().getFoodLevel()==19,"repeated activation cannot double-charge hunger");
            var enemy=EntityType.ZOMBIE.create(level);enemy.moveTo(8.5,80,10.5);enemy.setNoAi(true);
            enemy.getAttribute(Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(enemy);float health=enemy.getHealth();
            player.setPos(8.5,80,11.5);rush.tickIdle();
            check(enemy.getHealth()==health-8,"swept collision hits a passed enemy for 8 damage");
            enemy.invulnerableTime=0;rush.tickIdle();check(enemy.getHealth()==health-8,"only one hit per target per charge");
            for(int i=0;i<20;i++)rush.tickIdle();check(!rush.isDashing(),"charge always expires");
            player.moveTo(8.5,84,8.5);player.baseTick();check(!rush.canUse(),"dry weather prevents use");
            level.setWeatherParameters(0,1200,true,false);level.setRainLevel(1);
            check(player.isInWaterOrRain()&&!player.isInWater(),"fixture is in rain, outside water");
            rush.startUsing();check(!rush.isDashing()&&player.getFoodData().getFoodLevel()==19,"rain alone cannot activate or spend food");
            CompoundTag spoof=new CompoundTag();spoof.putInt("RushTicks",20);rush.acceptPayload(spoof);
            check(!rush.isDashing(),"server rejects forged animation/duration payload");
            player.moveTo(8.5,80,8.5);player.baseTick();rush.startUsing();rush.onRemove();
            check(!rush.isDashing(),"form removal clears active charge");
            player.getFoodData().setFoodLevel(0);check(!rush.canUse(),"empty hunger blocks survival rush");
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            rush.startUsing();check(rush.isDashing()&&player.getFoodData().getFoodLevel()==0,"creative rush has no food cost");
            for(int tick=0;tick<4;tick++)rush.tickIdle();
            check(!rush.isDashing()&&player.getDeltaMovement().length()>.35,"stationary player ends the rush even when server velocity still contains the launch impulse");rush.onRemove();
            var npcRush=fish.getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());
            fish.setTarget(enemy);fish.setNoAi(true);fish.setYRot(0);fish.baseTick();fish.setDeltaMovement(Vec3.ZERO);npcRush.startUsing();
            check(Math.abs(fish.getDeltaMovement().length()-2.25)<.00001,"NPC uses the same Riptide II launch speed");
            for(int i=0;i<21;i++)fish.tick();check(!npcRush.isDashing(),"native NPC charge ticks and expires even without selected player ability");
            fish.moveTo(8.5,80,8.5);fish.baseTick();fish.setDeltaMovement(Vec3.ZERO);npcRush.startUsing();
            npcRush.tickIdle();npcRush.tickIdle();fish.setDeltaMovement(npcRush.direction().scale(.1));npcRush.tickIdle();
            check(npcRush.isDashing(),"a single slow movement update does not prematurely cancel the rush");npcRush.tickIdle();
            check(!npcRush.isDashing(),"water drag ends the rush before the fixed timeout once launch momentum is gone");
            npcRush.startUsing();fish.moveTo(8.5,84,8.5);fish.baseTick();npcRush.tickIdle();
            check(!npcRush.isDashing(),"leaving water ends the rush without retaining the spin state");
            level.setWeatherParameters(1200,0,false,false);level.setRainLevel(0);
            BondReviveRegression.run(server);
            CompanionFoodRegression.run(server);
            October22Regression.run(server);
            mothSafety(level);
            testServer=server;
        }catch(Throwable failure){System.err.println("SWORDFISH_SERVER_REGRESSION_FAIL");failure.printStackTrace();}
        finally{if(testServer==null)event.getServer().halt(false);}
    }
    private static void repeatRush(net.minecraft.server.level.ServerLevel level) {
        boolean throttle=ChangedSynergyConfig.COMMON.distantAiThrottling.get();
        ChangedSynergyConfig.COMMON.distantAiThrottling.set(false);
        var fish=ChangedSynergyEntities.LATEX_SWORDFISH.get().create(level);
        var enemy=EntityType.ZOMBIE.create(level);
        try {
            fish.moveTo(8.5,80,8.5);fish.setNoAi(true);level.addFreshEntity(fish);
            enemy.moveTo(8.5,80,10);enemy.setNoAi(true);level.addFreshEntity(enemy);
            fish.setTarget(enemy);fish.baseTick();
            var rush=fish.getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());
            var goal=fish.goalSelector.getAvailableGoals().stream().map(g->g.getGoal())
                    .filter(g->g.getClass().getEnclosingClass()==net.parkabird.changedsynergy.entity.LatexSwordfish.class)
                    .findFirst().orElseThrow();
            check(ChangedSynergyAbilities.SWORDFISH_RUSH.get().getCoolDown(IAbstractChangedEntity.forEntity(fish))==80,"NPC cooldown is four seconds");
            for(int round=0;round<3;round++) {
                check(fish.isInWater()&&fish.distanceToSqr(enemy)<9&&goal.canUse(),"NPC can rush again at close range without leaving water, round "+round);
                goal.start();check(rush.isDashing()&&rush.getController().isCoolingDown(),"real AI goal launches and starts cooldown");
                for(int tick=1;tick<=80;tick++) {
                    fish.setPos(8.5,80,8.5);fish.setDeltaMovement(Vec3.ZERO);fish.tick();
                    check(fish.isInWater(),"NPC remains underwater throughout cooldown");
                    check(Math.abs(rush.getController().coolDownPercent()-tick/80F)<.00001,"paused ability goal still advances cooldown exactly once per tick");
                    if(tick<80)check(!goal.canUse(),"NPC cannot rush before four-second cooldown");
                }
                check(!rush.isDashing(),"previous spin ended before the next rush");
            }
            fish.goalSelector.removeAllGoals(g->!(g instanceof net.ltxprogrammer.changed.entity.ai.UseAbilityGoal));
            fish.setNoAi(false);rush.getController().applyCoolDown();
            for(int tick=1;tick<=20;tick++) {
                fish.setPos(8.5,80,8.5);fish.setDeltaMovement(Vec3.ZERO);fish.tick();
                check(Math.abs(rush.getController().coolDownPercent()-tick/80F)<.00001,"running native ability goal does not double-decrement cooldown");
            }
            System.out.println("SWORDFISH_REPEAT_RUSH_PASS: three underwater AI launches; four-second cooldown with and without native ability ticking");
        } finally {fish.discard();enemy.discard();ChangedSynergyConfig.COMMON.distantAiThrottling.set(throttle);}
    }
    private static void mothSafety(net.minecraft.server.level.ServerLevel level)throws Exception {
        // There are no connected players in this fixture; test movement without the distant-idle throttle.
        ChangedSynergyConfig.COMMON.distantAiThrottling.set(false);
        for(int x=30;x<40;x++)for(int z=30;z<40;z++)for(int y=79;y<84;y++)level.setBlock(new BlockPos(x,y,z),(y==79?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
        var moth=ChangedSynergyEntities.LATEX_SILK_MOTH.get().create(level);moth.moveTo(34.5,80,34.5);level.addFreshEntity(moth);
        var goal=new MothLightRestGoal(moth);var safe=MothLightRestGoal.class.getDeclaredMethod("safeStand",BlockPos.class);safe.setAccessible(true);
        BlockPos feet=new BlockPos(35,80,35);
        check((boolean)safe.invoke(goal,feet),"safe solid floor accepted");
        for(var block:java.util.List.of(Blocks.FIRE,Blocks.SOUL_FIRE,Blocks.LAVA,Blocks.CAMPFIRE,Blocks.SOUL_CAMPFIRE,Blocks.MAGMA_BLOCK)) {
            BlockPos hazard=feet.offset(1,block==Blocks.MAGMA_BLOCK?-1:0,0);
            if(block==Blocks.SOUL_FIRE)level.setBlock(hazard.below(),Blocks.SOUL_SOIL.defaultBlockState(),3);
            level.setBlock(hazard,block.defaultBlockState(),3);
            check(!(boolean)safe.invoke(goal,feet),"light destination avoids "+block);
            level.setBlock(hazard,(hazard.getY()==79?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
            if(block==Blocks.SOUL_FIRE)level.setBlock(hazard.below(),Blocks.STONE.defaultBlockState(),3);
        }
        check((boolean)safe.invoke(goal,feet),"safe destination restored after hazard removed");
        var idle=MothLightRestGoal.class.getDeclaredMethod("idle");idle.setAccessible(true);
        check((boolean)idle.invoke(goal),"ordinary idle moth eligible");
        moth.setTarget(EntityType.ZOMBIE.create(level));check(!(boolean)idle.invoke(goal),"combat takes precedence over light");moth.setTarget(null);
        moth.goalSelector.removeAllGoals(g->!(g instanceof MothLightRestGoal));
        level.setBlock(LAMP,Blocks.GLOWSTONE.defaultBlockState(),3);
        restingMoth=moth;
    }
    @SubscribeEvent public static void liveTick(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        if(pendingStart!=null) {
            if(++preparationTicks<80)return;
            var ready=pendingStart;pendingStart=null;run(ready);return;
        }
        if(testServer==null)return;
        // No real players are connected, so the fixture advances this otherwise unticked entity.
        restingMoth.tick();
        if(++liveTicks<400)return;
        try{
            check(restingMoth.position().distanceToSqr(Vec3.atBottomCenterOf(LAMP))<9,"real idle AI moves beside the safe lamp: pos="+restingMoth.position()+", light="+restingMoth.level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK,restingMoth.blockPosition()));
            check(restingMoth.level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK,restingMoth.blockPosition())>=10,"moth rests in actual block light");
            System.out.println("SWORDFISH_SERVER_REGRESSION_PASS: "+checks+" registration, ocean, items, player/NPC collision and live moth-light safety checks");
        }catch(Throwable failure){System.err.println("SWORDFISH_SERVER_REGRESSION_FAIL");failure.printStackTrace();}
        finally{testServer.halt(false);testServer=null;}
    }
    private static void check(boolean value,String description){if(!value)throw new AssertionError(description);checks++;}
}
