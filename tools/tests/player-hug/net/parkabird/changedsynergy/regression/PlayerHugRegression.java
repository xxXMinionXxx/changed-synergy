package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.ability.GrabEntityAbilityInstance;
import net.ltxprogrammer.changed.ability.AbstractAbilityInstance.KeyReference;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.LivingEntityDataExtension;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.network.PlayerHugActionPacket;

@Mod.EventBusSubscriber(modid="changed_synergy",value=Dist.DEDICATED_SERVER)
public final class PlayerHugRegression {
    private static int checks;
    private static void check(boolean value,String description){if(!value)throw new AssertionError(description);checks++;}
    private static ResourceLocation id(String name){return ResourceLocation.parse(name);}
    private static ServerPlayer player(MinecraftServer server,String name,String form){
        var player=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),name));
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);
        player.setPos(0,100,0);player.setOnGround(true);
        if(form!=null)check(ProcessTransfur.setPlayerTransfurVariant(player,ChangedRegistry.TRANSFUR_VARIANT.get().getValue(id(form)),null,1,false)!=null,"instantiate "+form);
        return player;
    }
    @SubscribeEvent public static void run(ServerStartedEvent event){
        var server=event.getServer();
        try{
            // Keep fixture toggles independent of the on-disk config watcher.
            ChangedSynergyConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
            FriendlyInputChecks.run(server);
            var actor=player(server,"HugActor","changed:form_white_latex_wolf/male");
            var grab=PlayerHugService.ability(actor);
            check(grab!=null,"native ability is present");
            check(!PlayerHugService.mode(grab),"combat default");
            PlayerHugService.action(actor,PlayerHugActionPacket.TOGGLE,-1);
            check(PlayerHugService.mode(grab),"server toggle enables friendly mode");
            PlayerHugService.action(actor,PlayerHugActionPacket.TOGGLE,-1);
            check(PlayerHugService.mode(grab),"duplicate packet cannot toggle twice");
            checkAddonPermission(grab, true);
            ready(grab);
            PlayerHugService.action(actor,PlayerHugActionPacket.TOGGLE,-1);
            check(!PlayerHugService.mode(grab),"toggle returns to ordinary mode");
            checkAddonPermission(grab, false);
            if(net.parkabird.changedsynergy.compat.ChangedAddonCompat.isLoaded()) {
                grab.getClass().getMethod("setAllowGrabTransfurred",boolean.class).invoke(grab,true);
                PlayerHugService.sync(grab);
                checkAddonPermission(grab, false);
            }
            ready(grab);
            PlayerHugService.action(actor,PlayerHugActionPacket.TOGGLE,-1);
            checkAddonPermission(grab, true);
            for(String name:new String[]{"dark_latex_wolf_pup","pure_white_latex_wolf_pup","gas_wolf_pup","milk_pudding","latex_shark_feral","headless_knight","dark_latex_wolf_male","pure_white_latex_cerberus","latex_stiger"}){
                ready(grab);
                var target=(ChangedEntity)ForgeRegistries.ENTITY_TYPES.getValue(id("changed:"+name)).create(server.overworld());
                target.setPos(.2,100,0);target.setOnGround(true);
                target.getPersistentData().putString("HugHostilitySentinel","unchanged");
                target.setLastHurtByMob(actor);
                check(PlayerHugService.validTarget(grab,target),"target allowed: "+name);
                check(grab.grabEntity(target)&&PlayerHugService.active(grab),"friendly hold: "+name);
                float health=target.getHealth();
                grab.attackDown=grab.useDown=true;grab.suitTransition=5;
                grab.tickIdle();
                check(grab.grabbedEntity==target&&!grab.suited&&!grab.grabbedHasControl,"no suit or lost target: "+name);
                var decision=GrabEntityAbilityInstance.class.getDeclaredMethod("makeAssimilationDecision");decision.setAccessible(true);
                check(target.getHealth()==health&&decision.invoke(grab)==null&&!grab.suitEntity(target),"no damage/transfur/suit: "+name);
                PlayerHugService.action(actor,PlayerHugActionPacket.TOGGLE,-1);
                check(PlayerHugService.mode(grab),"mode locked during hold");
                float strength=grab.grabStrength;
                boolean carryStyle=((PlayerHugAccess)grab).changedSynergy$isUprightCarry();
                check(grab.grabEntity(target)&&grab.grabStrength==strength,"repeat grab cannot reset resistance");
                check(((PlayerHugAccess)grab).changedSynergy$isUprightCarry()==carryStyle,"same hold retains carry style");
                grab.tickIdle();check(grab.grabStrength==strength,"shared body update cannot tick resistance twice");
                check(target.getPersistentData().getString("HugHostilitySentinel").equals("unchanged"),"unrelated memory retained");
                grab.grabStrength=0;float holderHealth=actor.getHealth();grab.releaseEntity(true);
                check(grab.grabbedEntity==null&&!PlayerHugService.active(grab)&&!target.noPhysics,"safe release: "+name);
                check(actor.getHealth()==holderHealth&&!actor.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&!actor.hasEffect(MobEffects.WEAKNESS),"no release damage/debuffs");
                check(((LivingEntityDataExtension)target).getGrabbedBy()==null,"capture link cleared");
                check(target.getLastHurtByMob()==actor,"retaliation memory remains after release");
                check(!PlayerHugService.validTarget(grab,target),"NPC release has a friendly cooldown");
                for(int i=0;i<80;i++)grab.getUseType().check(true,true,true,grab.getController());
                check(grab.grabbedEntity==null&&grab.getController().getHoldTicks()==0,"holding release does not restart the activation");
            }
            ready(grab);
            var victim=player(server,"HugVictim","changed:form_dark_latex_wolf/female");
            check(grab.grabEntity(victim),"permanent transformed player may be hugged");
            check(!grab.isGrabbedInvalid(),"transformed player remains a valid target");
            grab.tickIdle();check(PlayerHugService.escapePlayer(grab)==victim,"real player uses player escape");
            check(((LivingEntityDataExtension)victim).getGrabbedBy()==actor,"player QTE link set");
            var form=ProcessTransfur.getPlayerTransfurVariant(victim);
            for(int i=0;i<12;i++){grab.entity.getEntity().tickCount++;grab.handleEscape();}
            check(grab.currentEscapeKey!=null,"native QTE initializes");
            for(int i=0;i<12&&grab.grabStrength>0;i++){
                grab.entity.getEntity().tickCount++;grab.ticksUnpressed=5;grab.escapeKeys.queueKeyState(grab.currentEscapeKey,true);grab.handleEscape();
                for(var key:new KeyReference[]{KeyReference.MOVE_FORWARD,KeyReference.MOVE_BACKWARD,KeyReference.MOVE_LEFT,KeyReference.MOVE_RIGHT})grab.escapeKeys.queueKeyState(key,false);
                grab.entity.getEntity().tickCount++;grab.handleEscape();
            }
            check(grab.grabStrength<=0,"correct QTE input escapes");grab.tickIdle();
            check(grab.grabbedEntity==null&&!victim.noPhysics&&((LivingEntityDataExtension)victim).getGrabbedBy()==null,"QTE reliably releases player");
            check(ProcessTransfur.getPlayerTransfurVariant(victim)==form,"original species/sex instance preserved");
            check(!PlayerHugService.validTarget(grab,victim),"escaped player has a brief friendly recapture cooldown");
            victim.getPersistentData().remove("ChangedSynergyFriendlyHugAfter");
            ready(grab);
            var shark=player(server,"HugShark","changed:form_latex_shark_feral");
            var sharkGrab=PlayerHugService.ability(shark);
            if(sharkGrab!=null)check(!sharkGrab.canUse()&&!sharkGrab.grabEntity(victim),"feral shark cannot initiate a grab");
            var pup=player(server,"HugPup","changed:form_dark_latex_wolf_pup");
            check(PlayerHugService.ability(pup)==null,"pup not granted a grab");
            ChangedSynergyConfig.COMMON.playerFriendlyHugPlayers.set(false);
            check(!PlayerHugService.validTarget(grab,victim),"player-hug config enforced");
            ChangedSynergyConfig.COMMON.playerFriendlyHugPlayers.set(true);
            Changed.config.server.isGrabEnabled.set(false);
            check(!PlayerHugService.validTarget(grab,victim),"Changed player-grab config enforced");
            Changed.config.server.isGrabEnabled.set(true);
            var zombie=new Zombie(EntityType.ZOMBIE,server.overworld());zombie.setPos(.2,100,0);
            check(grab.grabEntity(zombie),"humanoid monsters retained");
            ChangedSynergyConfig.COMMON.playerFriendlyHugs.set(false);grab.tickIdle();
            check(grab.grabbedEntity==null&&!PlayerHugService.active(grab),"disabling feature releases instead of becoming combat");
            ChangedSynergyConfig.COMMON.playerFriendlyHugs.set(true);
            ready(grab);
            victim.setPos(30,100,0);check(!grab.grabEntity(victim),"out-of-range rejected");victim.setPos(0,100,0);
            var saved=new net.minecraft.nbt.CompoundTag();check(grab.grabEntity(victim),"hold for save check");grab.saveData(saved);
            check(!saved.hasUUID("GrabbedEntity")&&!saved.getBoolean("Suited"),"active hug not saved");PlayerHugService.release(grab);
            ready(grab);
            check(grab.grabEntity(victim),"hold for original manual release");
            var holdTicks=grab.getController().getClass().getDeclaredField("holdTicks");holdTicks.setAccessible(true);holdTicks.setInt(grab.getController(),40);grab.tick();
            check(grab.grabbedEntity==null&&grab.getController().getHoldTicks()==0,"native forty-tick ability hold releases");
            ((PlayerHugAccess)grab).changedSynergy$setPlayerHug(true);
            grab.releaseEntity(false);
            check(!PlayerHugService.active(grab),"release also clears friendly state when the native target is already absent");
            var packet=new net.parkabird.changedsynergy.network.PlayerHugStatePacket(actor.getUUID(),actor.getId(),actor.getUUID(),victim.getUUID(),victim.getId(),victim.getUUID(),true,true);
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try{net.parkabird.changedsynergy.network.PlayerHugStatePacket.encode(packet,buffer);check(net.parkabird.changedsynergy.network.PlayerHugStatePacket.decode(buffer).equals(packet)&&buffer.readableBytes()==0,"hug state packet round-trips");}finally{buffer.release();}
            if(net.parkabird.changedsynergy.compat.TrueTransfurCompat.supportsSeparateInstinct()){ready(grab);instinct(server,actor,victim);}
            System.out.println("PLAYER_HUG_SERVER_PASS: "+checks+" native capability, safety, QTE and lifecycle checks");
        }catch(Throwable failure){System.err.println("PLAYER_HUG_SERVER_FAIL");failure.printStackTrace();}
        finally{server.halt(false);}
    }
    private static void ready(GrabEntityAbilityInstance grab){
        var level=(net.minecraft.server.level.ServerLevel)grab.entity.getLevel();
        ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(level.getGameTime()+40);
        for(int i=0;i<40;i++)grab.getController().tickCoolDown();
    }
    private static void checkAddonPermission(GrabEntityAbilityInstance grab,boolean friendly)throws Exception {
        if(!net.parkabird.changedsynergy.compat.ChangedAddonCompat.isLoaded())return;
        check(grab.getClass().getMethod("allowGrabTransfurred").invoke(grab).equals(friendly),"transformed-target permission matches player mode");
        check(grab.getClass().getMethod("isSafeMode").invoke(grab).equals(friendly),"Addon safe mode matches player mode");
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    private static void instinct(MinecraftServer server,ServerPlayer actor,ServerPlayer victim)throws Exception{
        var players=(java.util.Map<UUID,ServerPlayer>)net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(net.minecraft.server.players.PlayerList.class,"f_11197_").get(server.getPlayerList());
        players.put(actor.getUUID(),actor);players.put(victim.getUUID(),victim);
        server.overworld().getGameRules().getRule(net.parkabird.changedsynergy.init.ChangedSynergyGameRules.NATIVE_ORIGINS).set(true,server);
        Class mode=Class.forName("com.lugert.true_transfur.transfur.ControlMode");
        var change=Class.forName("com.lugert.true_transfur.transfur.TransfurController").getMethod("switchControlMode",ServerPlayer.class,mode);
        for(var player:new ServerPlayer[]{actor,victim}){
            var identity=new net.parkabird.changedsynergy.origin.NativeOriginData.Identity();identity.state="native";
            identity.form=identity.current=ProcessTransfur.getPlayerTransfurVariant(player).getParent().getFormId().toString();identity.faction="light";
            net.parkabird.changedsynergy.origin.NativeOriginData.get(server).players.put(player.getUUID(),identity);
            var cap=(net.minecraftforge.common.capabilities.Capability)Class.forName("com.lugert.true_transfur.transfur.TransfurCapabilities").getField("TRANSFUR_DATA").get(null);
            Object data=player.getCapability(cap).orElse(null);check(data!=null,"TT capability attached");
            data.getClass().getMethod("setTransfurred",boolean.class).invoke(data,true);
            check(Boolean.TRUE.equals(change.invoke(null,player,Enum.valueOf(mode,"INSTINCT"))),"enter actual TT INSTINCT");
        }
        var a=(ChangedEntity)net.parkabird.changedsynergy.compat.TrueTransfurCompat.interactionBody(actor);
        var b=(ChangedEntity)net.parkabird.changedsynergy.compat.TrueTransfurCompat.interactionBody(victim);
        a.setNoAi(true);b.setNoAi(true);a.setPos(0,100,0);b.setPos(.2,100,0);
        var grab=PlayerHugService.ability(actor);check(grab!=null&&grab.entity.getEntity()==a,"actual controlled body owns native ability");
        check(PlayerHugService.mode(grab),"preference follows owner into physical body");
        check(grab.grabEntity(b)&&PlayerHugService.escapePlayer(grab)==victim,"hug body resolves its real player");
        grab.tickIdle();check(((LivingEntityDataExtension)victim).getGrabbedBy()==a,"real TT owner is linked for QTE packets");
        float start=grab.grabStrength;
        for(int i=0;i<12;i++){grab.entity.getEntity().tickCount++;grab.handleEscape();}
        check(grab.currentEscapeKey!=null&&grab.grabStrength==start,"TT body uses player QTE, not NPC automatic decay");
        grab.entity.getEntity().tickCount++;grab.ticksUnpressed=5;grab.escapeKeys.queueKeyState(grab.currentEscapeKey,true);grab.handleEscape();
        check(grab.grabStrength<start,"TT owner key input reduces hold strength");
        PlayerHugService.release(grab);check(((LivingEntityDataExtension)victim).getGrabbedBy()==null&&!b.noPhysics,"TT body and shell released together");
        change.invoke(null,actor,Enum.valueOf(mode,"PLAYER"));change.invoke(null,victim,Enum.valueOf(mode,"PLAYER"));
        players.remove(actor.getUUID());players.remove(victim.getUUID());
    }
}
