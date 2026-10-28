package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.LatexCreatureCombatRules;

final class VillageCoexistenceRegression {
    private static int checks;
    static void run(MinecraftServer server) throws Exception {
        var level=server.overworld();var setting=ChangedSynergyConfig.COMMON.peacefulVillageRelations;
        boolean old=setting.get();
        var player=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"VillagePeace"));
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);
        player.setPos(128.5,160,128.5);
        var villager=EntityType.VILLAGER.create(level);var golem=EntityType.IRON_GOLEM.create(level);
        var wolf=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
        var skeleton=EntityType.SKELETON.create(level);var zombie=EntityType.ZOMBIE.create(level);
        for(var mob:new net.minecraft.world.entity.Mob[]{villager,golem,wolf,skeleton,zombie}) {
            mob.setPos(player.position());mob.setNoAi(true);level.addFreshEntity(mob);
        }
        var hostile=java.util.Arrays.stream(VillagerHostilesSensor.class.getDeclaredMethods())
                .filter(m->(m.getName().equals("isMatchingEntity")||m.getName().equals("m_142628_"))
                        &&m.getParameterCount()==2&&m.getParameterTypes()[0]==LivingEntity.class)
                .findFirst().orElseThrow();hostile.setAccessible(true);
        var sensor=new VillagerHostilesSensor();
        try {
            setting.set(true);
            int baseline=villager.getPlayerReputation(player);
            check(!LatexCreatureCombatRules.mustRejectVillageTarget(golem,player),"Human village rules remain unchanged");
            var form=ChangedTransfurVariants.DARK_LATEX_WOLF_MALE.get();
            ProcessTransfur.setPlayerTransfurVariant(player,form,null,1,false);
            check(!TransfurVariant.shouldScareVillager(wolf,villager)&&!form.scares.test(wolf,villager),"Player form no longer causes direct village flight");
            check(villager.getPlayerReputation(player)==baseline,"Transfur does not add the native -120 village reputation penalty");
            check(form.scares.test(wolf,skeleton),"Unrelated skeleton fear remains active");
            check(!(boolean)hostile.invoke(sensor,villager,wolf),"Village hostile sensor ignores latex");
            check((boolean)hostile.invoke(sensor,villager,zombie),"Village hostile sensor still detects zombies");
            check(LatexCreatureCombatRules.mustRejectVillageTarget(golem,player)&&!golem.canAttack(player),"Unprovoked golem rejects transformed player during target selection");
            golem.setTarget(player);check(golem.getTarget()==null,"Actual Forge target assignment cannot retain the peaceful player");
            var attack=new LivingAttackEvent(player,level.damageSources().mobAttack(golem),4);
            MinecraftForge.EVENT_BUS.post(attack);check(attack.isCanceled(),"Species-only golem damage is rejected");
            golem.setLastHurtByMob(player);
            check(!LatexCreatureCombatRules.mustRejectVillageTarget(golem,player)&&golem.canAttack(player),"Direct player aggression still permits self defence");
            golem.setLastHurtByMob(null);
            golem.setPersistentAngerTarget(player.getUUID());golem.setRemainingPersistentAngerTime(400);
            check(!LatexCreatureCombatRules.mustRejectVillageTarget(golem,player),"Real persistent golem anger remains effective");
            golem.setRemainingPersistentAngerTime(0);golem.setPersistentAngerTarget(null);
            if(net.parkabird.changedsynergy.compat.TrueTransfurCompat.supportsSeparateInstinct())
                instinct(player,golem,villager,sensor,hostile);
            villager.getGossips().add(player.getUUID(),GossipType.MAJOR_NEGATIVE,100);
            check(villager.getPlayerReputation(player)<=-100&&!LatexCreatureCombatRules.mustRejectVillageTarget(golem,player),"Genuine negative village reputation still permits defence");
            setting.set(false);
            check(TransfurVariant.shouldScareVillager(wolf,villager)&&form.scares.test(wolf,villager),"Disabling peace restores native player fear");
            check((boolean)hostile.invoke(sensor,villager,wolf),"Disabling peace restores native latex hostile sensing");
            check(!LatexCreatureCombatRules.mustRejectVillageTarget(golem,player),"Disabling peace restores golem targeting policy");
            scareSnapshot(player,form);
            System.out.println("VILLAGE_COEXISTENCE_REGRESSION_PASS: "+checks+" real village fear, reputation, target assignment, attack, retaliation and config checks");
        } finally {
            setting.set(old);ProcessTransfur.removePlayerTransfurVariant(player);player.discard();
            for(var mob:new net.minecraft.world.entity.Mob[]{villager,golem,wolf,skeleton,zombie})mob.discard();
        }
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void scareSnapshot(ServerPlayer player,TransfurVariant form) throws Exception {
        var level=player.serverLevel();var members=new java.util.ArrayList<net.minecraft.world.entity.PathfinderMob>();
        var predicate=TransfurVariant.class.getDeclaredField("scares");predicate.setAccessible(true);
        Object original=predicate.get(form);int[] visited={0};
        try {
            for(int i=0;i<12;i++) {
                var zombie=EntityType.ZOMBIE.create(level);zombie.setPos(player.getX()+.1*i,player.getY(),player.getZ());
                zombie.setNoAi(true);level.addFreshEntity(zombie);members.add(zombie);
            }
            predicate.set(form,(java.util.function.BiPredicate<net.ltxprogrammer.changed.entity.ChangedEntity,net.minecraft.world.entity.PathfinderMob>)(body,target)->{
                if(members.contains(target)) {
                    visited[0]++;
                    if(visited[0]==1)members.get(11).discard();
                }
                return false;
            });
            var instance=(net.ltxprogrammer.changed.server.ServerTransfurVariantInstance<?>)ProcessTransfur.getPlayerTransfurVariant(player);
            instance.tickScare();
            check(visited[0]>=11,"Native player scare scan tolerates entity removal inside the scare predicate");
        } finally {predicate.set(form,original);members.forEach(net.minecraft.world.entity.Entity::discard);}
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void instinct(ServerPlayer player,net.minecraft.world.entity.animal.IronGolem golem,
            net.minecraft.world.entity.npc.Villager villager,VillagerHostilesSensor sensor,java.lang.reflect.Method hostile) throws Exception {
        var mapField=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(net.minecraft.server.players.PlayerList.class,"f_11197_");
        var players=(java.util.Map<UUID,ServerPlayer>)mapField.get(player.server.getPlayerList());
        players.put(player.getUUID(),player);player.serverLevel().addNewPlayer(player);
        var identity=new net.parkabird.changedsynergy.origin.NativeOriginData.Identity();identity.state="native";
        identity.form=identity.current=ChangedTransfurVariants.DARK_LATEX_WOLF_MALE.getId().toString();identity.faction="dark";
        var origins=net.parkabird.changedsynergy.origin.NativeOriginData.get(player.server);origins.players.put(player.getUUID(),identity);
        var cap=(net.minecraftforge.common.capabilities.Capability)Class.forName("com.lugert.true_transfur.transfur.TransfurCapabilities").getField("TRANSFUR_DATA").get(null);
        Object data=player.getCapability(cap).orElse(null);
        check(data!=null,"Real TT capability is present");
        data.getClass().getMethod("setTransfurred",boolean.class).invoke(data,true);
        Class mode=Class.forName("com.lugert.true_transfur.transfur.ControlMode");
        var change=Class.forName("com.lugert.true_transfur.transfur.TransfurController").getMethod("switchControlMode",ServerPlayer.class,mode);
        try {
            change.invoke(null,player,Enum.valueOf(mode,"INSTINCT"));
            var body=(net.ltxprogrammer.changed.entity.ChangedEntity)net.parkabird.changedsynergy.compat.TrueTransfurCompat.interactionBody(player);
            check(body!=null&&body!=(Object)player&&net.parkabird.changedsynergy.compat.TrueTransfurCompat.isInstinctBody(body),"Real TT Instinct body is active");
            body.setNoAi(true);body.setPos(golem.position());
            check(!golem.canAttack(body)&&LatexCreatureCombatRules.mustRejectVillageTarget(golem,body),"Golem keeps peace with the player's real Instinct body");
            golem.setTarget(body);check(golem.getTarget()==null&&!(boolean)hostile.invoke(sensor,villager,body),"Actual Instinct body is neither a golem target nor a village fear target");
            golem.setLastHurtByMob(body);
            check(!LatexCreatureCombatRules.mustRejectVillageTarget(golem,body),"Instinct-body aggression still permits village retaliation");
            golem.setLastHurtByMob(null);
        } finally {
            change.invoke(null,player,Enum.valueOf(mode,"PLAYER"));origins.players.remove(player.getUUID());players.remove(player.getUUID());
        }
    }
    private static void check(boolean ok,String message) {if(!ok)throw new AssertionError(message);checks++;}
}
