package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.LivingEntityDataExtension;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.BondedSuitService;
import net.parkabird.changedsynergy.ai.LatexSocialMemory;
import net.parkabird.changedsynergy.ai.PureWhiteWolfAdaptation;
import net.parkabird.changedsynergy.dialogue.NpcDialogue;

final class SuitFeedbackRegression {
    private static int checks;
    static void run(MinecraftServer server) throws Exception {
        pickup(server); whiteSuits(server); solvent(server); confinement(server);
        System.out.println("SUIT_FEEDBACK_REGRESSION_PASS: "+checks+" real suit/pickup, temporary White eyes and native solvent damage checks");
    }
    private static void suit(ChangedEntity pet,ServerPlayer player) {
        BondedSuitService.ensureGrabAbility(pet);
        var grab=BondedSuitService.ability(pet);
        check(grab.suitEntity(player),"Native suit starts"); grab.grabbedHasControl=true;
        ((LivingEntityDataExtension)player).setGrabbedBy(pet);
        check(BondedSuitService.getSuitController(pet)==player,"Native suit controller is independent of following");
    }
    @SuppressWarnings("unchecked")
    private static void pickup(MinecraftServer server) throws Exception {
        var level=server.overworld();var player=player(server);
        var players=(java.util.Map<UUID,ServerPlayer>)net.minecraftforge.fml.util.ObfuscationReflectionHelper
                .findField(net.minecraft.server.players.PlayerList.class,"f_11197_").get(server.getPlayerList());
        players.put(player.getUUID(),player);level.addNewPlayer(player);
        var pet=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);pet.setPos(player.position());
        pet.tame(player);pet.setNoAi(true);pet.setCanPickUpLoot(true);level.addFreshEntity(pet);
        var item=new ItemEntity(level,pet.getX(),pet.getY(),pet.getZ(),new ItemStack(Items.IRON_SWORD));
        item.setNoPickUpDelay();level.addFreshEntity(item);
        try {
            check(level.getEntitiesOfClass(ItemEntity.class,pet.getBoundingBox().inflate(1)).contains(item),"Pickup fixture has a visible nearby item");
            LatexSocialMemory.addBond(pet,player);
            check(BondedSuitService.suitOwner(pet,player,BondedSuitService.SuitReason.MANUAL),"Public bonded wrapping establishes the authoritative friendly session");
            LatexSocialMemory.setFollowingOwner(pet,false);
            check(BondedSuitService.getSuitController(pet)==player&&BondedSuitService.ability(pet).grabStrength>0,
                    "Wandering selection preserves the live owner suit before native loot ticking");
            pet.aiStep();
            check(item.isAlive(),"Wandering suit cannot take the player's nearby weapon; controller="+(BondedSuitService.getSuitController(pet)==player)+", suited="+BondedSuitService.ability(pet).suited+", hand="+pet.getMainHandItem());
            check(!NpcDialogue.trigger(pet,player,NpcDialogue.Cue.ROUTINE_ROAM),"Controlled suit cannot issue background roaming dialogue");
            check(pet.canPickUpLoot(),"Pickup preference is preserved for release");
            LatexSocialMemory.beginFriendlySuit(pet,player,false,false);
            BondedSuitService.ability(pet).grabbedHasControl=false;
            check(BondedSuitService.getSuitController(pet)==player,"Friendly session remains authoritative after a control-bit reset");
            pet.aiStep();
            check(BondedSuitService.getSuitController(pet)==player&&item.isAlive(),"A temporary Addon control-bit reset cannot steal items from an authoritative friendly suit; suited="+BondedSuitService.ability(pet).suited+", friendly="+LatexSocialMemory.isFriendlySuitActive(pet,player)+", item="+item.isAlive());
            LatexSocialMemory.endFriendlySuit(pet,player);
            BondedSuitService.ability(pet).releaseEntity(false);
            item.setPos(pet.position());pet.aiStep();
            check(item.isRemoved()&&pet.getInventory().countItem(Items.IRON_SWORD)==1,"Released creature resumes native equipment pickup");
        } finally {item.discard();pet.discard();ProcessTransfur.removePlayerTransfurVariant(player);players.remove(player.getUUID());player.discard();}
    }
    private static void whiteSuits(MinecraftServer server) {
        boolean enabled=ChangedSynergyConfig.COMMON.pureWhiteWolfAdaptation.get();
        var level=server.overworld();
        try {
            for(var type:List.of(ChangedEntities.PURE_WHITE_LATEX_WOLF.get(),ChangedEntities.PURE_WHITE_LATEX_WOLF_PUP.get(),
                    ChangedEntities.PURE_WHITE_LATEX_CERBERUS.get(),ChangedEntities.LATEX_MUTANT_BLOODCELL_WOLF.get())) {
                ChangedSynergyConfig.COMMON.pureWhiteWolfAdaptation.set(true);
                var player=player(server);var pet=type.create(level);pet.setPos(player.position());level.addFreshEntity(pet);
                try {
                    var data=new CompoundTag();data.putBoolean("Eligible",true);data.putLong("AwaySince",level.getGameTime()-1000);
                    pet.getPersistentData().put(PureWhiteWolfAdaptation.DATA_KEY,data);
                    suit(pet,player);player.tickCount=Math.floorMod(player.getId(),100);
                    PureWhiteWolfAdaptation.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,player));
                    var variant=ProcessTransfur.getPlayerTransfurVariant(player);
                    check(variant!=null&&variant.isTemporaryFromSuit()&&PureWhiteWolfAdaptation.isAdaptedPlayer(variant)
                            &&PureWhiteWolfAdaptation.isAdaptedForm(variant.getChangedEntity()),"Temporary White suit receives the wrapping creature's adapted eyes: "+type);
                    check(PureWhiteWolfAdaptation.isAdaptedForm(pet)&&pet.getType()==type&&variant.getChangedEntity().getType()==type,
                            "Suit adaptation retains both original species");
                    check(variant.save().getCompound(PureWhiteWolfAdaptation.PLAYER_DATA_KEY).getBoolean("Adapted"),"Temporary eyes travel in the native variant sync payload");
                    ChangedSynergyConfig.COMMON.pureWhiteWolfAdaptation.set(false);
                    PureWhiteWolfAdaptation.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,player));
                    check(!PureWhiteWolfAdaptation.isAdaptedPlayer(variant)&&!PureWhiteWolfAdaptation.isAdaptedForm(pet),"Disabling adaptation clears both suit and NPC eyes");
                } finally {BondedSuitService.ability(pet).releaseEntity(false);pet.discard();ProcessTransfur.removePlayerTransfurVariant(player);player.discard();}
            }
        } finally {ChangedSynergyConfig.COMMON.pureWhiteWolfAdaptation.set(enabled);}
    }
    private static void solvent(MinecraftServer server) throws Exception {
        var attribute=ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.parse("changed_addon:latex_solvent_damage_multiplier"));
        if(attribute==null) {check(!net.minecraftforge.fml.ModList.get().isLoaded("changed_addon"),"Optional solvent integration is absent without Addon");return;}
        var enchantment=ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.parse("changed_addon:latex_solvent"));
        var level=server.overworld();var attacker=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
        var target=ChangedEntities.WHITE_LATEX_WOLF_MALE.get().create(level);
        attacker.setNoAi(true);level.addFreshEntity(attacker);level.addFreshEntity(target);
        var apply=Class.forName("net.foxyas.changedaddon.enchantment.LatexSolventEnchantment$LatexSolventEnchantmentEventHandle")
                .getMethod("onEntityAttacked",LivingHurtEvent.class);
        try {
            check(attacker.getAttribute(attribute)!=null,"Latex NPC receives Addon's native weapon attribute");
            var sword=new ItemStack(Items.IRON_SWORD);sword.enchant(enchantment,3);attacker.setItemSlot(EquipmentSlot.MAINHAND,sword);attacker.tick();
            check(Math.abs(attacker.getAttributeValue(attribute)-.6)<.0001,"Equipped solvent III applies the real 0.6 weapon modifier to the NPC");
            var damage=new LivingHurtEvent(target,level.damageSources().mobAttack(attacker),10);apply.invoke(null,damage);
            check(Math.abs(damage.getAmount()-16)<.001,"Addon's unchanged damage handler applies the NPC's solvent bonus exactly once");
            attacker.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));attacker.tick();
            damage=new LivingHurtEvent(target,level.damageSources().mobAttack(attacker),10);apply.invoke(null,damage);
            check(attacker.getAttributeValue(attribute)==0&&damage.getAmount()==10,"Changing to an unenchanted weapon removes the bonus");
            attacker.setItemSlot(EquipmentSlot.OFFHAND,sword);attacker.tick();
            check(attacker.getAttributeValue(attribute)==0,"An offhand enchantment cannot add a main-hand bonus");
        } finally {attacker.discard();target.discard();}
    }
    private static ServerPlayer player(MinecraftServer server) {
        var player=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),"SuitFeedback"));
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);
        net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class,player,0,"f_8921_");
        player.setPos(128.5,160,128.5);player.setOnGround(true);return player;
    }
    @SuppressWarnings("unchecked")
    private static void confinement(MinecraftServer server) throws Exception {
        var config=ChangedSynergyConfig.COMMON;
        boolean enabled=config.takeoverEnabled.get(),kin=config.bondedKinConfinement.get();
        var rule=server.overworld().getGameRules().getRule(net.ltxprogrammer.changed.init.ChangedGameRules.RULE_KEEP_BRAIN);
        boolean kept=rule.get();
        var activeField=net.parkabird.changedsynergy.ai.TakeoverService.class.getDeclaredField("ACTIVE");activeField.setAccessible(true);
        var active=(java.util.Map<UUID,Object>)activeField.get(null);
        try {
            config.takeoverEnabled.set(true);config.bondedKinConfinement.set(true);rule.set(true,server);
            for(int scenario=0;scenario<3;scenario++) {
                var player=player(server);var pet=ChangedEntities.WHITE_LATEX_WOLF_MALE.get().create(server.overworld());
                pet.setPos(player.position());server.overworld().addFreshEntity(pet);
                try {
                    check(!net.parkabird.changedsynergy.ai.TakeoverService.beginBondedConfinement(pet,player),"Unbonded NPC cannot use bonded confinement");
                    LatexSocialMemory.addBond(pet,player);
                    String original="";
                    if(scenario==1) {
                        suit(pet,player);LatexSocialMemory.beginFriendlySuit(pet,player,false,false);
                        net.parkabird.changedsynergy.compat.ChangedAddonCompat.configureFriendlyGrab(BondedSuitService.ability(pet),true);
                        pet.setPos(player.getX()+5,player.getY(),player.getZ());
                        check(!net.parkabird.changedsynergy.ai.TakeoverService.beginBondedConfinement(pet,player)
                                &&LatexSocialMemory.isFriendlySuitActive(pet,player),"Out-of-reach confinement preserves the existing friendly suit");
                        pet.setPos(player.position());
                    } else if(scenario==2) {
                        var permanent=ProcessTransfur.setPlayerTransfurVariant(player,
                                net.ltxprogrammer.changed.init.ChangedTransfurVariants.DARK_LATEX_WOLF_MALE.get(),null,1,false);
                        original=permanent.getParent().getFormId().toString();
                        check(!net.parkabird.changedsynergy.ai.TakeoverService.beginBondedConfinement(pet,player),"Already transformed owner must use evacuation, never confinement takeover");
                        check(ProcessTransfur.getPlayerTransfurVariant(player)==permanent,"Rejecting takeover preserves the original transformed body");
                        continue;
                    }
                    check(net.parkabird.changedsynergy.ai.TakeoverService.beginBondedConfinement(pet,player),"Bond confinement establishes real capture after reaching its owner: "+scenario);
                    var entry=active.get(player.getUUID());
                    var originalField=entry.getClass().getDeclaredField("originalForm");originalField.setAccessible(true);
                    check(originalField.get(entry).equals(original),"Confinement preserves a real original form but never makes a temporary suit permanent");
                    check(!BondedSuitService.ability(pet).grabbedHasControl&&!LatexSocialMemory.isFriendlySuitActive(pet,player),"Confinement owns control and ends the old friendly suit state");
                } finally {
                    active.remove(player.getUUID());var grab=BondedSuitService.ability(pet);if(grab!=null)grab.releaseEntity(false);
                    pet.discard();ProcessTransfur.removePlayerTransfurVariant(player);player.discard();
                }
            }
        } finally {config.takeoverEnabled.set(enabled);config.bondedKinConfinement.set(kin);rule.set(kept,server);}
    }
    private static void check(boolean ok,String message) {if(!ok)throw new AssertionError(message);checks++;}
}
