package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.TransfurCause;
import net.ltxprogrammer.changed.entity.TransfurContext;
import net.ltxprogrammer.changed.entity.ai.LatexAssimilationDecision;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedGameRules;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.compat.ChangedAddonCompat;
import net.parkabird.changedsynergy.init.ChangedSynergyAbilities;
import net.parkabird.changedsynergy.init.ChangedSynergyTransfurVariants;

final class SilkAndPunishmentRegression {
    private static int checks;
    static void run(MinecraftServer server)throws Exception {
        silk(server);punishment(server);
        System.out.println("SILK_AND_PUNISHMENT_REGRESSION_PASS: "+checks+" checks");
    }
    private static void silk(MinecraftServer server) {
        var player=player(server);var form=ChangedSynergyTransfurVariants.LATEX_SILK_MOTH.get();
        try {
            var transformed=ProcessTransfur.setPlayerTransfurVariant(player,form,null,1,false);
            var abstraction=IAbstractChangedEntity.forPlayer(player);var ability=ChangedSynergyAbilities.SPIN_SILK.get();
            var instance=transformed.getAbilityInstance(ability);check(instance!=null,"moth has its registered silk ability");
            check(ability.getChargeTime(abstraction)==20&&ability.getCoolDown(abstraction)==20
                    &&ability.getUseType(abstraction)==ChangedAbilities.CREATE_COBWEB.get().getUseType(abstraction),"native spider charge and cooldown are retained");
            var controller=instance.getController();
            for(int tick=0;tick<19;tick++)instance.getUseType().check(true,false,true,controller);
            check(count(player)==0,"incomplete charge produces nothing");
            check(!completed(player,"spin_silk"),"incomplete silk charge cannot grant its advancement");
            instance.getUseType().check(true,false,true,controller);
            check(count(player)==4&&controller.isCoolingDown()&&exhaustion(player)==5F,"one native charge yields four string and charges the native five exhaustion");
            check(completed(player,"spin_silk"),"actual silk production grants its advancement");
            for(int tick=0;tick<20;tick++)controller.tickCoolDown();
            check(!controller.isCoolingDown(),"native cooldown ends after twenty ticks");
            player.getFoodData().setFoodLevel(6);controller.activateAbility();
            check(!instance.canUse()&&count(player)==4,"low hunger cannot create free string");
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);float before=exhaustion(player);
            controller.activateAbility();check(count(player)==8&&exhaustion(player)==before,"creative creation does not spend food");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.getFoodData().setFoodLevel(20);
            for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.DIRT,64));
            int dropped=drops(player);controller.activateAbility();
            check(drops(player)==dropped+4,"full inventory drops exactly the four produced string");
        } finally {player.discard();}
    }
    @SuppressWarnings("unchecked")
    private static void punishment(MinecraftServer server)throws Exception {
        var config=ChangedSynergyConfig.COMMON;var rules=server.overworld().getGameRules();
        boolean kept=rules.getBoolean(ChangedGameRules.RULE_KEEP_BRAIN),enabled=config.takeoverEnabled.get();
        boolean punitive=config.takeoverPunitive.get(),finale=config.takeoverHostileFinale.get();
        var method=config.creatureTransfurMethod.get();
        var entryType=Class.forName("net.parkabird.changedsynergy.ai.TakeoverService$Entry");
        var activeField=TakeoverService.class.getDeclaredField("ACTIVE");activeField.setAccessible(true);
        var active=(Map<UUID,Object>)activeField.get(null);
        var finish=TakeoverService.class.getDeclaredMethod("tryRelease",entryType,ServerPlayer.class,ChangedEntity.class,boolean.class);finish.setAccessible(true);
        var sessionField=entryType.getDeclaredField("session");sessionField.setAccessible(true);
        var fatalField=entryType.getDeclaredField("hostileFatalOutcome");fatalField.setAccessible(true);
        var release=TakeoverSession.class.getDeclaredMethod("release",long.class,TakeoverSession.ReleaseReason.class);release.setAccessible(true);
        try {
            rules.getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(false,server);
            config.takeoverEnabled.set(true);config.takeoverPunitive.set(true);config.takeoverHostileFinale.set(true);
            config.creatureTransfurMethod.set(ChangedSynergyConfig.CreatureTransfurMethod.NATIVE);
            for(int scenario=0;scenario<4;scenario++) {
                if(scenario==1&&!ChangedAddonCompat.isLoaded())continue;
                var player=player(server);var carrier=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(server.overworld());
                carrier.setPos(player.position().add(.7,0,0));server.overworld().addFreshEntity(carrier);
                try {
                    FactionReputation.set(carrier,player,0);
                    check(!TakeoverService.authorizeOrdinary(carrier,player,InvoluntaryTransfurNegotiation.Reason.HOST_SEEKING),"ordinary host-seeking remains blocked with Keep Conscious off");
                    FactionReputation.set(carrier,player,-100);
                    config.takeoverPunitive.set(false);
                    check(!TakeoverService.allowsOrdinaryWithCurrentRule(carrier,player),"disabled punitive setting removes the false-rule exception");
                    config.takeoverPunitive.set(true);config.takeoverHostileFinale.set(false);
                    check(!TakeoverService.allowsOrdinaryWithCurrentRule(carrier,player),"disabled fatal outcomes remove the false-rule exception");
                    config.takeoverHostileFinale.set(true);
                    check(!TakeoverService.authorizeOrdinary(carrier,player,InvoluntaryTransfurNegotiation.Reason.HOST_SEEKING),"hostility cannot turn friendly intent into a punitive authorization");
                    if(scenario==3) {
                        rules.getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(true,server);
                        config.takeoverHostileFinale.set(false);
                    }
                    if(scenario==1)player.getInventory().setItem(9,new ItemStack(ForgeRegistries.ITEMS.getValue(ChangedAddonCompat.TRANSFUR_TOTEM)));
                    var decision=LatexAssimilationDecision.strong(LatexAssimilationDecision.Method.ABSORPTION,carrier.getSelfVariant(),TransfurContext.npcLatexHazard(carrier,TransfurCause.GRAB_ABSORB),100F);
                    ProcessTransfur.progressTransfur(player,decision);
                    var entry=active.get(player.getUUID());
                    check(entry!=null&&fatalField.getBoolean(entry)==(scenario!=3)&&TakeoverService.contains(carrier,player),"Native hostile absorption starts the requested fatal or nonfatal takeover");
                    var session=(TakeoverSession)sessionField.get(entry);long now=server.overworld().getGameTime();
                    if(scenario>=2)FactionHostilityGrace.beginSecondary(carrier,player);
                    else check(session.failPunitiveNegotiation(now),"punitive negotiation can reach its existing fatal sleep outcome");
                    release.invoke(session,now,scenario>=2?TakeoverSession.ReleaseReason.BREAKOUT:TakeoverSession.ReleaseReason.SLEEP);finish.invoke(null,entry,player,carrier,true);
                    if(scenario==0)check(!player.isAlive()&&carrier.isAlive(),"unprotected punitive finale still kills its host");
                    else if(scenario==1)check(player.isAlive()&&carrier.isRemoved()&&ProcessTransfur.getPlayerTransfurVariantSafe(player).orElseThrow().getFormId().equals(carrier.getSelfVariant().getFormId()),"totem reverses the real false-rule punitive chain and claims the original body");
                    else if(scenario==2)check(player.isAlive()&&!FactionHostilityGrace.active(carrier,player)
                            &&!FactionHostilityGrace.damageLocked(carrier,player)&&carrier.getTarget()==player,"Fatal punitive breakout removes capture-time grace and faction damage lock and restores the carrier's hostility");
                    else check(player.isAlive()&&FactionHostilityGrace.active(carrier,player),"Ordinary nonfatal breakout retains its normal release protection");
                    check(!TakeoverService.active(player)&&!player.noPhysics,"finale clears capture and physics locks");
                    check(completed(player,"totem_reversal")== (scenario==1),"only a real totem reversal grants its advancement");
                } finally {active.remove(player.getUUID());player.discard();carrier.discard();}
            }
        } finally {
            rules.getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(kept,server);config.takeoverEnabled.set(enabled);
            config.takeoverPunitive.set(punitive);config.takeoverHostileFinale.set(finale);config.creatureTransfurMethod.set(method);
        }
    }
    private static ServerPlayer player(MinecraftServer server) {
        var player=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),"SilkPunish"));
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);
        net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class,player,0,"f_8921_");
        player.setPos(128.5,160,128.5);player.setOnGround(true);return player;
    }
    private static int count(ServerPlayer player){return player.getInventory().items.stream().filter(s->s.is(Items.STRING)).mapToInt(ItemStack::getCount).sum();}
    private static boolean completed(ServerPlayer player,String name){var advancement=player.server.getAdvancements().getAdvancement(new net.minecraft.resources.ResourceLocation("changed_synergy",name));return advancement!=null&&player.getAdvancements().getOrStartProgress(advancement).isDone();}
    private static int drops(ServerPlayer player){return player.serverLevel().getEntitiesOfClass(ItemEntity.class,player.getBoundingBox().inflate(4),e->e.getItem().is(Items.STRING)).stream().mapToInt(e->e.getItem().getCount()).sum();}
    private static float exhaustion(ServerPlayer player){var tag=new CompoundTag();player.getFoodData().addAdditionalSaveData(tag);return tag.getFloat("foodExhaustionLevel");}
    private static void check(boolean ok,String description){if(!ok)throw new AssertionError(description);checks++;}
}
