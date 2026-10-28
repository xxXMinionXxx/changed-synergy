package net.parkabird.changedsynergy.regression;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.CreatureLifeMemory;
import net.parkabird.changedsynergy.ai.MothLightRestGoal;
import net.parkabird.changedsynergy.ai.PlayerOutpostData;
import net.parkabird.changedsynergy.ai.PlayerOutpostGoal;
import net.parkabird.changedsynergy.ai.PlayerOutpostService;
import net.parkabird.changedsynergy.ai.PlayerOutpostSupplyType;
import net.parkabird.changedsynergy.entity.LatexSilkMoth;
import net.parkabird.changedsynergy.init.ChangedSynergyEntities;
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;

/** Full selectors and normal entity ticks: never strip the wandering or social goals. */
@Mod.EventBusSubscriber(modid = "changed_synergy")
public final class MothLightRegression {
    private static MinecraftServer server;
    private static int ticks, stage, checks;
    private static final List<LatexSilkMoth> moths = new ArrayList<>();
    private static final List<BlockPos> lamps = new ArrayList<>();
    private static final int[] rested = new int[4];
    private static final boolean[] fireEscaped = new boolean[2], escapeRan = new boolean[2];

    @SubscribeEvent public static void start(ServerStartedEvent event) {
        server = event.getServer(); var level = server.overworld();
        // No players connect to this fixture; foreground observation must not be distance-throttled.
        ChangedSynergyConfig.COMMON.distantAiThrottling.set(false);
        level.getGameRules().getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false, server);
        level.getGameRules().getRule(ChangedSynergyGameRules.CREATURE_LIFE).set(true, server);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, server);
        for (int i = 0; i < 4; i++) {
            int base = i * 32;
            level.setChunkForced(base >> 4, 0, true);
            for (int x = base; x < base + 16; x++) for (int z = 0; z < 16; z++) for (int y = 79; y < 84; y++)
                level.setBlock(new BlockPos(x, y, z), (y == 79 ? i == 2 ? Blocks.GLASS : Blocks.STONE : Blocks.AIR).defaultBlockState(), 18);
            var lamp = new BlockPos(base + 10, 80, 6); lamps.add(lamp);
            level.setBlock(lamp, (i < 2 ? Blocks.TORCH : Blocks.GLOWSTONE).defaultBlockState(), 3);
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null) return;
        try {
            var level = server.overworld();
            if (stage == 0 && ++ticks >= 80) {
                for (int i = 0; i < 4; i++) {
                    var moth = ChangedSynergyEntities.LATEX_SILK_MOTH.get().create(level);
                    moth.setId(32000 + i); moth.moveTo(i * 32 + 4.5, 80, 6.5); level.addFreshEntity(moth);
                    CreatureLifeMemory.setRole(moth, CreatureLifeMemory.GroupRole.WANDERER); moths.add(moth);
                    check(moth.goalSelector.getAvailableGoals().size() > 4, "native and social goals remain installed");
                }
                stage = 1; ticks = 0;
            } else if (stage == 1) {
                ticks++;
                for (int i = 0; i < 4; i++) {
                    var moth = moths.get(i);
                    if (running(moth) && moth.position().distanceToSqr(Vec3.atBottomCenterOf(lamps.get(i))) < 9
                            && level.getBrightness(LightLayer.BLOCK, moth.blockPosition()) >= 12) rested[i]++;
                }
                if (ticks < 400 && java.util.Arrays.stream(rested).anyMatch(n -> n < 10)) return;
                for (int i = 0; i < 4; i++) {
                    var moth = moths.get(i);
                    System.out.println("MOTH_LIGHT_OBSERVATION id=" + moth.getId() + " visits=" + rested[i] + " position=" + moth.position()
                            + " role=" + CreatureLifeMemory.role(moth) + " ticks=" + moth.tickCount
                            + " running=" + moth.goalSelector.getRunningGoals().map(g -> g.getGoal().getClass().getSimpleName()).toList());
                }
                for (int i = 0; i < 4; i++) check(rested[i] >= 10, "full AI moth " + i + " rests by its light, including both ID phases and a glass floor");
                safety();
                var enemy = EntityType.ZOMBIE.create(level); enemy.moveTo(moths.get(0).position().add(2, 0, 0)); moths.get(0).setTarget(enemy);
                for (var lamp : lamps) level.setBlock(lamp, Blocks.AIR.defaultBlockState(), 3);
                stage = 2; ticks = 0;
            } else if (stage == 2 && ++ticks >= 40) {
                for (var moth : moths) check(!running(moth), "combat or an extinguished light safely ends the light rest");
                prepareCampfires();stage=3;ticks=0;
            } else if (stage == 3) {
                ticks++;
                for(int i=0;i<2;i++) {
                    var moth=moths.get(i==0?1:3);
                    escapeRan[i]|=moth.goalSelector.getRunningGoals().anyMatch(g->g.getGoal() instanceof PlayerOutpostGoal.FireEscape);
                    if(!PlayerOutpostGoal.onCampfire(moth)&&!fireEscaped[i]) {
                        fireEscaped[i]=true;System.out.println("MOTH_CAMPFIRE_ESCAPE type="+(i==0?"normal":"soul")+" ticks="+ticks+" health="+moth.getHealth());
                    }
                    check(!running(moth),"camp assignment prevents light attraction from reclaiming escape navigation");
                }
                if(ticks<40&&(!fireEscaped[0]||!fireEscaped[1]))return;
                for(int i=0;i<2;i++) {
                    check(escapeRan[i],"existing priority-zero campfire escape actually runs");
                    check(fireEscaped[i],"moth walks off its lit campfire promptly");
                }
                System.out.println("MOTH_LIGHT_REGRESSION_PASS: " + checks + " full-selector attraction, glass floor, interruption, hazards and normal/soul campfire escape checks");
                server.halt(false); server = null;
            }
        } catch (Throwable failure) {
            System.err.println("MOTH_LIGHT_REGRESSION_FAIL"); failure.printStackTrace(); server.halt(false); server = null;
        }
    }

    private static void prepareCampfires() {
        var level=server.overworld();var data=PlayerOutpostData.get(server);var owner=java.util.UUID.randomUUID();
        var marker=new BlockPos(40,80,12);level.setBlock(marker,Blocks.CAMPFIRE.defaultBlockState(),3);
        check(data.claim(level,owner,marker).equals("claimed"),"private camp established");var home=data.byOwner(owner).orElseThrow();
        for(int i=0;i<2;i++) {
            var moth=moths.get(i==0?1:3);var fire=new BlockPos(i==0?36:44,80,6);
            level.setBlock(fire,(i==0?Blocks.CAMPFIRE:Blocks.SOUL_CAMPFIRE).defaultBlockState(),3);
            moth.getNavigation().stop();moth.setTarget(null);moth.moveTo(fire.getX()+.5,80.4375,6.5);moth.setOnGround(true);
            check(data.add(home,moth.getUUID(),"Moth fire safety",level.dimension().location().toString(),fire,PlayerOutpostSupplyType.of(moth)),"moth registered as camp resident");
            moth.getPersistentData().putUUID("ChangedSynergyPlayerOutpostOwner",owner);PlayerOutpostService.ensureGoal(moth);
            check(PlayerOutpostGoal.onCampfire(moth)&&PlayerOutpostService.assigned(moth),"real assigned moth stands on a lit campfire");
            check(!new MothLightRestGoal(moth).canUse(),"light goal yields completely to assigned camp duties");
        }
    }

    private static void safety() throws Exception {
        var level = server.overworld(); var moth = moths.get(1); var goal = new MothLightRestGoal(moth);
        var method = MothLightRestGoal.class.getDeclaredMethod("safeStand", BlockPos.class); method.setAccessible(true);
        var feet = new BlockPos(36, 80, 10);
        for (var block : List.of(Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.LAVA, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.MAGMA_BLOCK)) {
            var pos = feet.offset(1, block == Blocks.MAGMA_BLOCK ? -1 : 0, 0);
            if (block == Blocks.SOUL_FIRE) level.setBlock(pos.below(), Blocks.SOUL_SOIL.defaultBlockState(), 3);
            level.setBlock(pos, block.defaultBlockState(), 3);
            check(!(boolean)method.invoke(goal, feet), "light rest avoids " + block);
            level.setBlock(pos, (pos.getY() == 79 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 3);
            if (block == Blocks.SOUL_FIRE) level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        }
    }
    private static boolean running(LatexSilkMoth moth) {
        return moth.goalSelector.getRunningGoals().anyMatch(g -> g.getGoal() instanceof MothLightRestGoal);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); checks++; }
}
