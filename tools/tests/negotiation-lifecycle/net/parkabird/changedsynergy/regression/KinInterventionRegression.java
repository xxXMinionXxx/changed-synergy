package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.ability.GrabEntityAbility;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedGameRules;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.compat.TrueTransfurCompat;
import net.parkabird.changedsynergy.event.HuntAIEvents;

final class KinInterventionRegression {
    private static int checks;
    @SuppressWarnings({"rawtypes","unchecked"})
    static void run(MinecraftServer server) throws Exception {
        var level=server.overworld();var config=ChangedSynergyConfig.COMMON;
        boolean oldEnabled=config.bondedKinConfinement.get(),oldTakeover=config.takeoverEnabled.get();
        int oldLimit=config.bondedKinKillBaseLimit.get();
        boolean conscious=level.getGameRules().getBoolean(ChangedGameRules.RULE_KEEP_BRAIN);
        var players=(Map<UUID,ServerPlayer>)net.minecraftforge.fml.util.ObfuscationReflectionHelper
                .findField(net.minecraft.server.players.PlayerList.class,"f_11197_").get(server.getPlayerList());
        try {
            config.bondedKinConfinement.set(true);config.takeoverEnabled.set(true);config.bondedKinKillBaseLimit.set(1);
            for(int scenario=0;scenario<(TrueTransfurCompat.supportsSeparateInstinct()?6:5);scenario++) {
                var owner=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"KinOwner"+scenario));
                owner.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),owner);
                net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class,owner,0,"f_8921_");
                owner.setPos(128.5,160,128.5);owner.tickCount=1000;
                players.put(owner.getUUID(),owner);level.addNewPlayer(owner);
                if(scenario==0) {
                    NativeRescueRegression.run(server,owner);
                    owner.getPersistentData().remove("ChangedSynergyRescueGraceUntil");
                    owner.getPersistentData().remove("ChangedSynergyRescueReleaseSuppressed");
                    owner.setPos(128.5,160,128.5);
                }
                boolean organic=scenario==3||scenario==4;
                boolean changed=scenario==2||scenario==4||scenario==5;
                var pet=(ChangedEntity)(organic?ChangedEntities.BEIFENG.get():ChangedEntities.WHITE_LATEX_WOLF_MALE.get()).create(level);
                pet.setPos(129.5,160,128.5);pet.tickCount=1000;pet.setNoAi(true);level.addFreshEntity(pet);
                OrganicOwnerEvacuationGoal goal=null;
                java.lang.reflect.Method switchMode=null;Class mode=null;
                try {
                    config.bondedKinConfinement.set(true);config.takeoverEnabled.set(true);
                    level.getGameRules().getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(true,server);
                    LatexSocialMemory.addBond(pet,owner);LatexSocialMemory.setFollowingOwner(pet,false);
                    BondedSuitService.ensureGrabAbility(pet);
                    if(changed)ProcessTransfur.setPlayerTransfurVariant(owner,ChangedTransfurVariants.DARK_LATEX_WOLF_MALE.get(),null,1,false);
                    if(scenario==1) {
                        check(BondedSuitService.suitOwner(pet,owner,BondedSuitService.SuitReason.MANUAL),"Temporary friendly suit starts before intervention");
                    }
                    if(scenario==5) {
                        var identity=new net.parkabird.changedsynergy.origin.NativeOriginData.Identity();identity.state="native";
                        identity.form=identity.current=ChangedTransfurVariants.DARK_LATEX_WOLF_MALE.getId().toString();identity.faction="dark";
                        net.parkabird.changedsynergy.origin.NativeOriginData.get(server).players.put(owner.getUUID(),identity);
                        var cap=(net.minecraftforge.common.capabilities.Capability)Class.forName("com.lugert.true_transfur.transfur.TransfurCapabilities").getField("TRANSFUR_DATA").get(null);
                        Object data=owner.getCapability(cap).orElse(null);
                        data.getClass().getMethod("setTransfurred",boolean.class).invoke(data,true);
                        mode=Class.forName("com.lugert.true_transfur.transfur.ControlMode");
                        switchMode=Class.forName("com.lugert.true_transfur.transfur.TransfurController").getMethod("switchControlMode",ServerPlayer.class,mode);
                        switchMode.invoke(null,owner,Enum.valueOf(mode,"INSTINCT"));
                    }
                    LivingEntity body=TrueTransfurCompat.interactionBody(owner);body.setPos(owner.position());
                    var original=ProcessTransfur.getPlayerTransfurVariant(owner);
                    boolean carry=organic||changed;
                    check(BondedKinIntervention.usesEvacuation(pet,owner)==carry,"Intervention uses the real form, not a temporary suit: "+scenario);
                    int limit=1+Math.max(0,CreaturePersonality.familiarity(pet,owner)-40)/10;
                    for(int i=0;i<limit;i++) {
                        ChangedEntity victim=(ChangedEntity)(organic?ChangedEntities.BEIFENG.get():ChangedEntities.WHITE_LATEX_WOLF_FEMALE.get()).create(level);
                        victim.setPos(128.5,160,129.5);victim.setNoAi(true);level.addFreshEntity(victim);
                        HuntAIEvents.onHunterDeath(new LivingDeathEvent(victim,body==owner?level.damageSources().playerAttack(owner):level.damageSources().mobAttack(body)));
                        victim.discard();
                        check((BondedKinIntervention.pendingOwner(pet)==owner)==(i+1>=limit),"Witnessed kin kills schedule intervention only at the threshold");
                    }
                    if(!carry) {
                        BondedKinIntervention.tick(pet);
                        var hold=BondedSuitService.ability(pet);
                        check(TakeoverService.contains(pet,owner)&&hold.suited&&!hold.grabbedHasControl,"Human owner receives real bonded takeover: "+scenario);
                        check(BondedKinIntervention.pendingOwner(pet)==null&&!BondedKinIntervention.request(pet,owner),"Successful takeover consumes pending work and starts cooldown");
                        check(TakeoverService.forceReleaseByCommand(owner)&&!TakeoverService.active(owner),"Confinement can release cleanly");
                        check(ProcessTransfur.getPlayerTransfurVariant(owner)==null,"Human release restores the human rather than retaining a temporary suit");
                    } else {
                        config.takeoverEnabled.set(false);
                        level.getGameRules().getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(false,server);
                        goal=(OrganicOwnerEvacuationGoal)pet.goalSelector.getAvailableGoals().stream().map(g->g.getGoal())
                                .filter(g->g instanceof OrganicOwnerEvacuationGoal).findFirst().orElseThrow();
                        check(goal.canUse(),"Healthy wandering bonded owner can enter kin evacuation independently of takeover and Keep Conscious");
                        goal.start();
                        if(scenario==2) {
                            pet.setPos(owner.getX()+4,owner.getY(),owner.getZ());goal.tick();
                            check(BondedSuitService.ability(pet).grabbedEntity==null&&BondedKinIntervention.pendingOwner(pet)==owner,"Failed approach remains pending without capturing at a distance");
                            pet.setPos(owner.getX()+2,owner.getY(),owner.getZ());
                            var wall=owner.blockPosition().east();var oldWall=level.getBlockState(wall);var oldTop=level.getBlockState(wall.above());
                            level.setBlock(wall,Blocks.STONE.defaultBlockState(),2);level.setBlock(wall.above(),Blocks.STONE.defaultBlockState(),2);
                            try {goal.tick();check(BondedSuitService.ability(pet).grabbedEntity==null,"Kin evacuation cannot grab through a closed wall");}
                            finally {level.setBlock(wall,oldWall,2);level.setBlock(wall.above(),oldTop,2);}
                            check(pet.getPersistentData().getLong("ChangedSynergyBondConfinementCooldown")==0,"Failed physical contact does not consume the intervention cooldown");
                            pet.setPos(owner.getX()+1,owner.getY(),owner.getZ());
                        }
                        goal.tick();
                        var hold=BondedSuitService.ability(pet);
                        check(hold.grabbedEntity==body&&!hold.suited&&!TakeoverService.active(owner),"Transformed owner is carried in arms without a takeover: "+scenario);
                        check(LatexSocialMemory.isOrganicEvacuationActive(pet,body),"Safe evacuation record follows the actual player body");
                        check(BondedKinIntervention.pendingOwner(pet)==null&&!BondedKinIntervention.request(pet,owner),"Cooldown starts only after successful arm capture");
                        if(net.minecraftforge.fml.ModList.get().isLoaded("changed_addon")) {
                            Class.forName("net.parkabird.changedsynergy.compat.addon.ChangedAddonSocialEvents")
                                    .getMethod("onLivingTick",net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent.class)
                                    .invoke(null,new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(pet));
                            check(hold.grabbedEntity==body&&!hold.suited,"Actual Addon hold maintenance preserves the safe actual-body carry");
                        }
                        hold.attackDown=hold.useDown=true;hold.tickIdle();
                        var decision=net.ltxprogrammer.changed.ability.GrabEntityAbilityInstance.class.getDeclaredMethod("makeAssimilationDecision");decision.setAccessible(true);
                        check(hold.grabbedEntity==body&&!hold.attackDown&&!hold.useDown&&decision.invoke(hold)==null,"Native safe carry cannot assimilate or damage its passenger");
                        check(ProcessTransfur.getPlayerTransfurVariant(owner)==original&&owner.isAlive()&&body.isAlive(),"Carry preserves the owner's exact original form and living body");
                        if(scenario==4) {
                            config.bondedKinConfinement.set(false);
                            check(!goal.canContinueToUse(),"Disabling kin intervention interrupts an active carry");goal.stop();
                        } else for(int t=0;t<80&&hold.grabbedEntity!=null;t++) {pet.tickCount++;goal.tick();}
                        check(hold.grabbedEntity==null&&GrabEntityAbility.getGrabber(body)==null&&!body.noPhysics,"Stuck carry automatically releases both native references and the movement lock");
                        check(ProcessTransfur.getPlayerTransfurVariant(owner)==original&&!LatexSocialMemory.hasOrganicEvacuation(pet),"Automatic release retains the form and clears safety state");
                        goal.stop();
                    }
                } finally {
                    if(goal!=null)goal.stop();
                    TakeoverService.forceReleaseByCommand(owner);
                    var hold=BondedSuitService.ability(pet);if(hold!=null&&hold.grabbedEntity!=null)hold.releaseEntity(false);
                    if(switchMode!=null)switchMode.invoke(null,owner,Enum.valueOf(mode,"PLAYER"));
                    net.parkabird.changedsynergy.origin.NativeOriginData.get(server).players.remove(owner.getUUID());
                    ProcessTransfur.removePlayerTransfurVariant(owner);pet.discard();players.remove(owner.getUUID());owner.discard();
                }
            }
            System.out.println("KIN_INTERVENTION_REGRESSION_PASS: "+checks+" witnessed kill, human takeover, unchanged-form carry, native safety and automatic release checks");
        } finally {
            config.bondedKinConfinement.set(oldEnabled);config.takeoverEnabled.set(oldTakeover);config.bondedKinKillBaseLimit.set(oldLimit);
            level.getGameRules().getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(conscious,server);
        }
    }
    private static void check(boolean ok,String message) {if(!ok)throw new AssertionError(message);checks++;}
}
