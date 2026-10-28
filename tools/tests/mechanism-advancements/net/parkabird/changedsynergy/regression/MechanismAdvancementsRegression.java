package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Dynamic;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedGameRules;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.init.ChangedSynergyAbilities;
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;
import net.parkabird.changedsynergy.init.ChangedSynergyTransfurVariants;
import net.parkabird.changedsynergy.world.ResearchLabStructure;
import static net.parkabird.changedsynergy.network.ChasePlayStatePacket.*;

@Mod.EventBusSubscriber(modid = "changed_synergy", value = Dist.DEDICATED_SERVER)
public final class MechanismAdvancementsRegression {
    private static int checks;
    private static ServerLevel level;
    private static ServerPlayer player;
    private static final List<String> IDS = List.of("chase_game", "aquatic_chase", "companion_boarding", "spin_silk",
            "swordfish_rush", "research_laboratory", "voluntary_transfur", "totem_reversal");

    @SubscribeEvent public static void run(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            level = server.overworld();
            ChangedSynergyConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
            level.getGameRules().getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            player = player(server);
            for (String id : IDS) {
                if (id.equals("totem_reversal") && !ModList.get().isLoaded("changed_addon")) {
                    check(adv(id) == null, "optional totem advancement is hidden without Addon Plus");
                } else check(adv(id) != null && !done(player, id), "new advancement loaded and starts locked: " + id);
            }
            conscious(server); chase(); boarding(); voluntary(); rush(); laboratories();
            SilkAndPunishmentRegression.run(server);
            System.out.println("MECHANISM_ADVANCEMENTS_REGRESSION_PASS: " + checks + " checks; new-world conscious=true, saved false preserved; TT="
                    + ModList.get().isLoaded("true_transfur") + " addon=" + ModList.get().isLoaded("changed_addon"));
        } catch (Throwable failure) {
            System.err.println("MECHANISM_ADVANCEMENTS_REGRESSION_FAIL"); failure.printStackTrace();
        } finally { server.halt(false); }
    }
    private static void conscious(MinecraftServer server) throws Exception {
        var rules = new GameRules();
        check(rules.getBoolean(ChangedGameRules.RULE_KEEP_BRAIN), "fresh GameRules keeps consciousness enabled");
        check(!rules.getBoolean(ChangedGameRules.RULE_KEEP_FORM) && !rules.getBoolean(ChangedGameRules.RULE_DO_PALE), "other Changed defaults are untouched");
        for (boolean enabled : new boolean[]{false, true, false}) {
            rules.getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(enabled, server);
            var tag = rules.createTag();
            check(tag.getString("changed:keepConscious").equals(Boolean.toString(enabled)), "explicit value is saved");
            var restored = new GameRules(new Dynamic<>(NbtOps.INSTANCE, tag));
            check(restored.getBoolean(ChangedGameRules.RULE_KEEP_BRAIN) == enabled, "explicit value survives reload");
            level.getGameRules().getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(enabled, server);
            var victim = player(server);
            var source = ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
            ProcessTransfur.transfur(victim, net.ltxprogrammer.changed.entity.ai.ImmediateTransfurDecision.unsafe(
                    source.getSelfVariant(), net.ltxprogrammer.changed.entity.TransfurCause.ATTACK_REPLICATE_LEFT));
            check(ProcessTransfur.getPlayerTransfurVariantSafe(victim).orElseThrow().willSurviveTransfur == enabled,
                    "ordinary unsafe transfur respects explicit gamerule=" + enabled);
            victim.discard();
        }
        level.getGameRules().getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(true, server);
        check(new GameRules().getBoolean(ChangedGameRules.RULE_KEEP_BRAIN), "changing a world's value never changes the default");
    }
    private static void chase() throws Exception {
        var npc = friend(); LatexSocialMemory.addBond(npc, player);
        Class<?> service = CreaturePeerInteractionService.class;
        Class<?> session = Class.forName(service.getName() + "$Session");
        var constructor = session.getDeclaredConstructor(ChangedEntity.class, net.minecraft.world.entity.LivingEntity.class, boolean.class);
        constructor.setAccessible(true);
        for (int scenario = 0; scenario < 5; scenario++) {
            revoke(player, "chase_game"); revoke(player, "aquatic_chase");
            Object game = constructor.newInstance(npc, player, true);
            call(service, "reserve", session, game);
            set(game, "chaser", player); set(game, "runner", npc); set(game, "stage", CHASING);
            set(game, "water", scenario == 4);
            check(!done(player, "chase_game"), "starting a game is not completion");
            if (scenario == 0) CreaturePeerInteractionService.cancel(player);
            else if (scenario == 1) { call(service, "falseStart", session, game); CreaturePeerInteractionService.cancel(player); }
            else if (scenario == 2) { call(service, "missed", session, game); CreaturePeerInteractionService.cancel(player); }
            else call(service, "caught", session, game);
            check(done(player, "chase_game") == (scenario >= 2), "only normally finished games unlock chase achievement");
            check(done(player, "aquatic_chase") == (scenario == 4), "water completion is distinct from a land game");
        }
        npc.discard();
    }
    private static void boarding() throws Exception {
        revoke(player, "companion_boarding"); var npc = friend();
        var boat = new Boat(EntityType.BOAT, level); boat.setPos(player.position().add(2, 0, 2)); level.addFreshEntity(boat);
        CompanionSeatingService.command(player, npc, "social_seat");
        var field = CompanionSeatingService.class.getDeclaredField("SELECTIONS"); field.setAccessible(true);
        Object selection = ((Map<?, ?>)field.get(null)).get(player.getUUID());
        var token = selection.getClass().getDeclaredMethod("token"); token.setAccessible(true);
        var delta = boat.getBoundingBox().getCenter().subtract(player.getEyePosition());
        player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
        CompanionSeatingService.select(player, (int)token.invoke(selection), boat.getId(), null, false);
        check(CompanionSeatingService.pending(npc) && !done(player, "companion_boarding"), "request alone does not unlock boarding");
        var walk = CompanionSeatingService.class.getDeclaredMethod("walk", ChangedEntity.class); walk.setAccessible(true); walk.invoke(null, npc);
        check(npc.getVehicle() == boat && done(player, "companion_boarding"), "successful native boat boarding unlocks achievement");
        npc.stopRiding(); npc.discard(); boat.discard();
    }
    private static void voluntary() {
        var npc = friend(); revoke(player, "voluntary_transfur");
        check(VoluntaryBondTransfurService.canOfferFriendly(npc, player), "established friend offers voluntary transfur");
        check(!VoluntaryBondTransfurService.select(npc, player, true) && !done(player, "voluntary_transfur"), "confirmation alone never grants transfur achievement");
        check(VoluntaryBondTransfurService.select(npc, player, true) && done(player, "voluntary_transfur"), "accepted transfur grants achievement");
        ProcessTransfur.removePlayerTransfurVariant(player); npc.discard();
    }
    private static void rush() throws Exception {
        player = player(level.getServer());
        revoke(player, "swordfish_rush");
        var instance = ProcessTransfur.setPlayerTransfurVariant(player, ChangedSynergyTransfurVariants.LATEX_SWORDFISH.get(), null, 1, false);
        for (int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) for(int y=0;y<=3;y++)
            level.setBlock(player.blockPosition().offset(x,y,z), Blocks.WATER.defaultBlockState(), 18);
        player.setYRot(0); player.setXRot(0); player.baseTick();
        var ability = instance.getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());
        check(ability.canUse(), "swimming swordfish can rush"); ability.startUsing();
        check(!done(player, "swordfish_rush"), "starting an empty rush never grants a hit achievement");
        var target = EntityType.COW.create(level); target.setPos(player.position().add(0,0,1)); level.addFreshEntity(target);
        ability.tickIdle(); check(done(player, "swordfish_rush") && target.getHealth() < target.getMaxHealth(), "actual contact damage unlocks rush achievement");
        target.discard(); ProcessTransfur.removePlayerTransfurVariant(player);
    }
    private static void laboratories() {
        revoke(player, "research_laboratory"); player.setPos(240,160,240); CriteriaTriggers.LOCATION.trigger(player);
        check(!done(player, "research_laboratory"), "ordinary terrain never grants structure advancement");
        for (String name : List.of("water_tower_changed", "white_lab_tower", "red_lab_tower", "green_lab")) {
            if ((name.equals("red_lab_tower") || name.equals("green_lab")) && !ModList.get().isLoaded("changed_addon")) continue;
            revoke(player, "research_laboratory");
            var origin = new BlockPos(256,160,256);
            var piece = new ResearchLabStructure.Piece(level.getServer().getStructureManager(), id(name), origin, Rotation.NONE, false);
            var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id(name));
            var chunk = level.getChunkAt(origin);
            var start = new StructureStart(structure, chunk.getPos(), 0, new PiecesContainer(List.of(piece)));
            chunk.setStartForStructure(structure, start); chunk.addReferenceForStructure(structure, chunk.getPos().toLong());
            player.setPos(Vec3.atCenterOf(origin.offset(1,1,1))); CriteriaTriggers.LOCATION.trigger(player);
            check(done(player, "research_laboratory"), "native structure predicate unlocks inside " + name);
            chunk.setStartForStructure(structure, StructureStart.INVALID_START);
        }
    }
    private static ChangedEntity friend() {
        var npc = ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
        npc.setPos(player.position().add(1,0,1)); level.addFreshEntity(npc); npc.setOnGround(true);
        CreaturePersonality.establishRelationship(npc, player); CreaturePersonality.setFamiliarity(npc, player, 25);
        return npc;
    }
    @SuppressWarnings("unchecked") private static ServerPlayer player(MinecraftServer server) throws Exception {
        var p = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "MilestoneTest"));
        p.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), p);
        ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, p, 0, "f_8921_");
        ((Map<UUID,ServerPlayer>)ObfuscationReflectionHelper.findField(net.minecraft.server.players.PlayerList.class,"f_11197_").get(server.getPlayerList())).put(p.getUUID(),p);
        p.setPos(128.5,160,128.5); level.addNewPlayer(p); p.setOnGround(true);
        for(int x=124;x<138;x++)for(int z=124;z<138;z++)level.setBlock(new BlockPos(x,159,z),Blocks.STONE.defaultBlockState(),18);
        return p;
    }
    private static net.minecraft.advancements.Advancement adv(String name) { return level.getServer().getAdvancements().getAdvancement(id(name)); }
    static boolean done(ServerPlayer p, String name) { return p.getAdvancements().getOrStartProgress(adv(name)).isDone(); }
    private static void revoke(ServerPlayer p, String name) { for(String criterion : adv(name).getCriteria().keySet())p.getAdvancements().revoke(adv(name),criterion); }
    private static ResourceLocation id(String name) { return new ResourceLocation("changed_synergy",name); }
    private static void call(Class<?> service,String name,Class<?> type,Object arg)throws Exception { Method m=service.getDeclaredMethod(name,type);m.setAccessible(true);m.invoke(null,arg); }
    private static void set(Object o,String name,Object value)throws Exception { var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value); }
    private static void check(boolean ok,String description) { checks++;if(!ok)throw new AssertionError(description); }
}
