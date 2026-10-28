package net.parkabird.changedsynergy.ai;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.event.LatexSocialEvents;
import net.parkabird.changedsynergy.init.ChangedSynergyEntities;
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;
import static net.parkabird.changedsynergy.network.ChasePlayStatePacket.*;

/** Real native goals, navigation and swimming physics in a disposable server world. */
@Mod.EventBusSubscriber(modid="changed_synergy", value=Dist.DEDICATED_SERVER)
public final class AquaticPeerPlayRegression {
    private static final Class<?> SERVICE = CreaturePeerInteractionService.class;
    private static int checks;
    private static ServerLevel level;
    private static Vec3 origin;
    private static ChangedEntity shark, squid;
    private static ServerPlayer player;

    @SubscribeEvent public static void run(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            ChangedSynergyConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
            level = server.overworld();
            level.getGameRules().getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            PeerPlayRegression.run(server);
            ChangedSynergyConfig.COMMON.ambientPeerInteractions.set(true);
            ChangedSynergyConfig.COMMON.chasePlayGames.set(true);
            ChangedSynergyConfig.COMMON.chasePlayShortHugs.set(true);
            ChangedSynergyConfig.COMMON.distantAiThrottling.set(false);
            level.getGameRules().getRule(ChangedSynergyGameRules.CREATURE_LIFE).set(true, server);
            origin = new Vec3(level.getSharedSpawnPos().getX() + 100.5, 148, level.getSharedSpawnPos().getZ() + .5);
            for (int x = -32; x <= 32; x++) for (int z = -22; z <= 22; z++) for (int y = -9; y <= 8; y++)
                level.setBlock(BlockPos.containing(origin.add(x, y, z)), (y == -9 ? Blocks.STONE : Blocks.WATER).defaultBlockState(), 18);
            shark = creature(ChangedEntities.LATEX_SHARK_MALE.get().create(level), origin);
            squid = creature(ChangedEntities.LATEX_SQUID_DOG_MALE.get().create(level), origin.add(1, 0, 0));
            player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "SwimPlayTest"));
            player.setPos(origin.add(1, 0, 0));
            player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player);
            ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, player, 0, "f_8921_");
            var players = (Map<UUID, ServerPlayer>)ObfuscationReflectionHelper.findField(net.minecraft.server.players.PlayerList.class, "f_11197_").get(server.getPlayerList());
            players.put(player.getUUID(), player);
            level.addNewPlayer(player);
            ((List<ServerPlayer>)ObfuscationReflectionHelper.findField(net.minecraft.server.players.PlayerList.class, "f_11196_").get(server.getPlayerList())).add(player);
            LatexSocialMemory.addBond(shark, player);
            LatexSocialMemory.addBond(squid, player);
            reset();
            System.out.println("AQUATIC_INITIAL_STATE water=" + shark.isInWater() + " pose=" + shark.getPose()
                    + " ground=" + shark.onGround() + " position=" + shark.position() + " navigation=" + shark.getNavigation().getClass().getSimpleName());
            check(shark.isInWater() && !shark.onGround(), "native shark really enters water without standing on a floor");
            check((boolean)method("available", ChangedEntity.class, sessionClass()).invoke(null, shark, null), "swimming posture can accept an invitation");
            playerRoles();
            waterDestinations();
            npcMovement();
            interruptions();
            automaticInvites();
            var swordfish = creature(ChangedSynergyEntities.LATEX_SWORDFISH.get().create(level), origin.add(0, -2, 3));
            check((boolean)method("canChaseTogether", ChangedEntity.class, LivingEntity.class).invoke(null, swordfish, shark), "swordfish can join water games without using its damaging rush");
            swordfish.discard();
            System.out.println("AQUATIC_PLAY_REGRESSION_PASS: " + checks + " aquatic checks, plus existing ground-game regressions");
        } catch (Throwable failure) {
            System.err.println("AQUATIC_PLAY_REGRESSION_FAIL"); failure.printStackTrace();
        } finally {
            if (shark != null) CreaturePeerInteractionService.cancel(shark);
            if (squid != null) CreaturePeerInteractionService.cancel(squid);
            server.halt(false);
        }
    }

    private static void playerRoles() throws Exception {
        Object session = invite(0);
        check((boolean)get(session, "water") && get(session, "chaser") == player, "crouch selects player chaser in water");
        start(session);
        check(stage(session) == HEAD_START && !shark.onGround(), "underwater roles begin without a ground check");
        time((long)get(session, "deadline")); advance(session);
        check(stage(session) == CHASING, "same three-second head start transitions into swimming chase");
        check(CreaturePeerInteractionService.active(player), "proximity alone cannot catch a swimming NPC");
        player.setPos(shark.getX() + .2, shark.getY(), shark.getZ()); player.baseTick();
        float health = shark.getHealth();
        check(CreaturePeerInteractionService.playerTouch(player, shark) && !CreaturePeerInteractionService.active(player), "player right-click catches the aquatic runner");
        check(shark.getHealth() == health && player.getHealth() == player.getMaxHealth(), "catch is harmless");

        reset(); session = invite(1);
        check(get(session, "chaser") == shark && get(session, "runner") == player, "jump selects player runner underwater");
        check(shark.isShiftKeyDown() && shark.getPose() == Pose.SWIMMING, "NPC crouch signal keeps its swimming body");
        start(session); time((long)get(session, "deadline"));
        shark.setPos(player.getX() + .1, player.getY(), player.getZ()); shark.baseTick(); advance(session);
        check(stage(session) == HUG || !CreaturePeerInteractionService.active(player), "NPC water catch either hugs safely or finishes with touch");
        if (stage(session) == HUG) {
            check(CreaturePeerInteractionService.isHeld(player), "native catch hold is active underwater");
            time((long)get(session, "deadline")); advance(session);
            check(!CreaturePeerInteractionService.active(player) && net.ltxprogrammer.changed.ability.GrabEntityAbility.getGrabberSafe(player).isEmpty(), "two-second water hug releases cleanly");
        }
        check(player.getHealth() == player.getMaxHealth(), "water hug does not cause damage or transfur");

        reset(); shark.setPos(origin.add(0, 1.5, 0)); shark.baseTick(); session = invite(0); start(session);
        player.setPos(player.position().add(0, .7, 0)); player.baseTick(); advance(session);
        check(stage(session) == FALSE_START, "upward head-start pursuit is also a false start");
        CreaturePeerInteractionService.cancel(player);

        reset(); session = invite(0); start(session);
        int id = (int)get(session, "id");
        CreaturePeerInteractionService.gesture(player, id, 0); CreaturePeerInteractionService.gesture(player, id, 0);
        check(CreaturePeerInteractionService.active(player), "two crouch presses do not quit water play");
        CreaturePeerInteractionService.gesture(player, id, 0);
        check(!CreaturePeerInteractionService.active(player), "three fresh crouch presses exit the water game");
    }

    private static void waterDestinations() throws Exception {
        reset(); var waterPoint = method("waterPoint", ChangedEntity.class, Vec3.class, boolean.class);
        check(waterPoint.invoke(null, shark, origin.add(8, 3, 0), false) != null, "open water supports destinations at another depth");
        check(waterPoint.invoke(null, shark, origin.add(40, 0, 0), false) == null, "water game never selects dry land");
        check(waterPoint.invoke(null, shark, origin.add(0, 30, 0), false) == null, "air above the water is rejected");
        for (int y = -8; y < 9; y++) level.setBlock(BlockPos.containing(origin.add(8, y, 0)), Blocks.STONE.defaultBlockState(), 18);
        check(waterPoint.invoke(null, shark, origin.add(8, 0, 0), false) == null, "solid underwater obstruction is rejected");
        for (int y = -8; y < 9; y++) level.setBlock(BlockPos.containing(origin.add(8, y, 0)), Blocks.WATER.defaultBlockState(), 18);
        Object session = pair(shark, squid);
        set(session, "runner", shark); set(session, "chaser", squid);
        var flee = method("fleePoint", sessionClass(), ChangedEntity.class, LivingEntity.class);
        for (int i = 0; i < 20; i++) {
            Vec3 target = (Vec3)flee.invoke(null, session, shark, squid);
            check(target != null && level.getFluidState(BlockPos.containing(target)).is(FluidTags.WATER), "flee samples stay in water");
        }
        CreaturePeerInteractionService.cancel(shark);
    }

    private static void npcMovement() throws Exception {
        reset(); shark.setPos(origin.add(-7, -2, 0)); squid.setPos(origin.add(2, 1, 0)); shark.tick(); squid.tick();
        Object session = pair(shark, squid); set(session, "chaser", shark); set(session, "runner", squid);
        set(session, "rolesChosen", true); set(session, "stage", CHASING); set(session, "deadline", level.getGameTime() + 300);
        Vec3 start = shark.position(), runner = squid.position(); int ticks = 0; double maxVertical = 0;
        for (; ticks < 90 && CreaturePeerInteractionService.active(shark); ticks++) {
            time(level.getGameTime() + 1); shark.tick(); squid.tick();
            maxVertical = Math.max(maxVertical, Math.abs(shark.getY() - start.y));
            if (CreaturePeerInteractionService.active(shark)) {
                check((boolean)invoke("valid", session), "native swimming goals leave the paired game valid");
                advance(session);
            }
        }
        System.out.println("AQUATIC_NATIVE_MOVEMENT ticks=" + ticks + " chaser=" + shark.position() + " runner=" + squid.position() + " maxVertical=" + maxVertical);
        check(shark.position().distanceToSqr(start) > .5, "NPC chaser really swims with native navigation");
        check(squid.position().distanceToSqr(runner) > .5, "NPC runner really flees with native swimming physics");
        check(shark.getTarget() == null && squid.getTarget() == null, "play never installs combat targets or triggers swordfish attacks");
        check(shark.isInWaterOrBubble() && squid.isInWaterOrBubble(), "native water chase stays submerged");
        CreaturePeerInteractionService.cancel(shark);

        // Native A* can travel horizontally before climbing; a moving runner need not change that route in 90 ticks.
        reset(); shark.setPos(origin.add(-7, -2, 0)); shark.tick();
        player.setPos(origin.add(2, 1, 0)); player.baseTick();
        session = pair(shark, player); set(session, "chaser", shark); set(session, "runner", player);
        set(session, "rolesChosen", true); set(session, "stage", CHASING); set(session, "deadline", level.getGameTime() + 300);
        start = shark.position(); maxVertical = 0;
        for (int step = 0; step < 180 && CreaturePeerInteractionService.active(shark); step++) {
            time(level.getGameTime() + 1); shark.tick();
            maxVertical = Math.max(maxVertical, shark.getY() - start.y);
            advance(session);
        }
        check(maxVertical > 1, "native water pursuit climbs to a stationary runner at a different depth");
        CreaturePeerInteractionService.cancel(shark);
    }

    private static void interruptions() throws Exception {
        reset(); Object session = invite(0); start(session);
        player.setPos(origin.add(0, 30, 0)); player.baseTick();
        check(!(boolean)invoke("valid", session), "leaving water interrupts safely instead of dragging the NPC ashore");
        CreaturePeerInteractionService.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, level.getServer()));
        check(!CreaturePeerInteractionService.active(shark), "leaving-water interruption clears ownership");
        reset(); session = invite(0); start(session); player.setAirSupply(20);
        check(!(boolean)invoke("valid", session), "a human needing air safely stops water play");
        CreaturePeerInteractionService.cancel(shark);
        reset(); session = invite(0); start(session);
        var enemy = net.minecraft.world.entity.EntityType.DROWNED.create(level);
        enemy.setPos(origin.add(3, 0, 0)); level.addFreshEntity(enemy); shark.setLastHurtByMob(enemy);
        check(!(boolean)invoke("valid", session), "combat retains priority in water"); shark.setLastHurtByMob(null); enemy.discard(); CreaturePeerInteractionService.cancel(shark);
        reset(); session = invite(0); start(session); ChangedSynergyConfig.COMMON.chasePlayGames.set(false);
        check(!(boolean)invoke("valid", session), "existing chase configuration disables water games too");
        CreaturePeerInteractionService.cancel(shark); ChangedSynergyConfig.COMMON.chasePlayGames.set(true);
    }

    private static void automaticInvites() throws Exception {
        reset(); CreaturePersonality.setDominantTrait(shark, CreaturePersonality.Trait.PLAYFUL); CreaturePersonality.setDominantTrait(squid, CreaturePersonality.Trait.PLAYFUL);
        squid.setPos(origin.add(2, 0, 0)); squid.tick();
        shark.getRandom().setSeed(27181); squid.getRandom().setSeed(71828);
        for (var mob : List.of(shark, squid)) System.out.println("AQUATIC_AUTO_ELIGIBILITY " + mob.getType()
                + " available=" + method("available", ChangedEntity.class, sessionClass()).invoke(null, mob, null)
                + " pose=" + mob.getPose() + " active=" + CreaturePeerInteractionService.active(mob)
                + " life=" + CreatureLifeMemory.enabled(mob) + " goals="
                + mob.goalSelector.getRunningGoals().map(g -> g.getGoal().getClass().getSimpleName()).toList());
        System.out.println("AQUATIC_AUTO_NEIGHBOURS " + level.getEntitiesOfClass(ChangedEntity.class, shark.getBoundingBox().inflate(8, 3, 8)).size()
                + " feature=" + net.parkabird.changedsynergy.performance.SynergyPerformanceTracker.featureEnabled(
                        net.parkabird.changedsynergy.performance.SynergyPerformanceTracker.Feature.SOCIAL));
        Object found = null;
        for (int i = 0; i < 1600 && found == null; i++) {
            time(level.getGameTime() + 1);
            shark.tickCount++;
            for (var mob : List.of(shark, squid)) {
                CreaturePeerMemory.data(mob).putLong("NextPeerInteraction", 0); CreaturePeerMemory.data(mob).putLong("NextPeerChase", 0);
            }
            CreaturePeerInteractionService.ambient(new LivingEvent.LivingTickEvent(shark));
            Object active = session(shark);
            if (active != null) {
                if ((boolean)get(active, "chase")) found = active;
                else CreaturePeerInteractionService.cancel(shark);
            }
        }
        check(found != null && (boolean)get(found, "water") && session(squid) == found, "real automatic NPC pairing selects a water chase");
        for (int i = 0; i < 180 && stage(found) == READY && CreaturePeerInteractionService.active(shark); i++) {
            time(level.getGameTime() + 1); shark.tick(); squid.tick();
            CreaturePeerInteractionService.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, level.getServer()));
        }
        System.out.println("AQUATIC_AUTO_SIGNALS stage=" + stage(found) + " signal=" + get(found, "signalStep")
                + " distance=" + shark.distanceTo(squid) + " first=" + shark.position() + " second=" + squid.position()
                + " active=" + CreaturePeerInteractionService.active(shark));
        check(stage(found) == HEAD_START && get(found, "chaser") != get(found, "runner"),
                "automatic NPC signals choose complementary roles and start while swimming");
        CreaturePeerInteractionService.cancel(shark);
    }

    private static ChangedEntity creature(ChangedEntity mob, Vec3 position) {
        mob.moveTo(position); level.addFreshEntity(mob); CreatureLifeMemory.setRole(mob, CreatureLifeMemory.GroupRole.WANDERER);
        LatexSocialEvents.ensureSocialGoals(mob); mob.tick(); return mob;
    }
    private static void reset() {
        player.setPos(origin.add(1, 0, 0)); player.setDeltaMovement(Vec3.ZERO); player.setAirSupply(300); player.baseTick();
        for (var mob : List.of(shark, squid)) {
            CreaturePeerInteractionService.cancel(mob); mob.setTarget(null); mob.setShiftKeyDown(false);
            mob.setDeltaMovement(Vec3.ZERO); mob.setPos(origin.add(mob == shark ? 0 : 12, 0, 0)); mob.setAirSupply(300); mob.tick();
            CreaturePeerMemory.data(mob).putLong("NextPeerInteraction", level.getGameTime() + 100000);
        }
    }
    private static Object invite(int gesture) throws Exception {
        check(CreaturePeerInteractionService.invitePlayer(shark, player), "player play branch handled");
        Object session = session(shark);
        if (session == null) System.out.println("AQUATIC_INVITE_REJECTION available="
                + method("available", ChangedEntity.class, sessionClass()).invoke(null, shark, null)
                + " player=" + method("playerAvailable", ServerPlayer.class).invoke(null, player)
                + " environment=" + method("canChaseTogether", ChangedEntity.class, LivingEntity.class).invoke(null, shark, player)
                + " distance=" + shark.distanceTo(player) + " pose=" + shark.getPose()
                + " water=" + shark.isInWater() + " position=" + shark.position()
                + " goals=" + shark.goalSelector.getRunningGoals().map(g -> g.getGoal().getClass().getSimpleName()).toList());
        check(session != null, "aquatic player invitation reserves one shared session");
        CreaturePeerInteractionService.gesture(player, (int)get(session, "id"), gesture); return session;
    }
    private static void start(Object session) throws Exception {
        time(level.getGameTime() + 20); CreaturePeerInteractionService.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, level.getServer()));
        if (stage(session) == READY) advance(session);
        check(stage(session) == HEAD_START, "accepted water roles start the head-start phase");
    }
    private static void time(long value) { ((ServerLevelData)level.getLevelData()).setGameTime(value); }
    private static Class<?> sessionClass() throws Exception { return Class.forName(SERVICE.getName() + "$Session"); }
    private static Object pair(ChangedEntity first, LivingEntity second) throws Exception {
        var constructor = sessionClass().getDeclaredConstructor(ChangedEntity.class, LivingEntity.class, boolean.class); constructor.setAccessible(true);
        Object result = constructor.newInstance(first, second, true); invoke("reserve", result); return result;
    }
    private static Object session(LivingEntity entity) throws Exception { var field = SERVICE.getDeclaredField("PARTICIPANTS");field.setAccessible(true);return ((Map<?,?>)field.get(null)).get(entity.getUUID()); }
    private static Method method(String name, Class<?>... types) throws Exception { var method = SERVICE.getDeclaredMethod(name, types); method.setAccessible(true);return method; }
    private static Object invoke(String name, Object session) throws Exception { return method(name,sessionClass()).invoke(null,session); }
    private static void advance(Object session) throws Exception { invoke("advance",session); }
    private static int stage(Object session) throws Exception { return (int)get(session,"stage"); }
    private static Object get(Object object, String name) throws Exception { Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object); }
    private static void set(Object object, String name, Object value) throws Exception { Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value); }
    private static void check(boolean condition, String message) { checks++; if(!condition)throw new AssertionError(message); }
}
