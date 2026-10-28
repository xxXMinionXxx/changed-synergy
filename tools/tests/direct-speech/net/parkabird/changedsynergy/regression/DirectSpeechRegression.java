package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.dialogue.NpcDialogue;
import net.parkabird.changedsynergy.dialogue.NpcDialogue.Cue;
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;
import net.parkabird.changedsynergy.network.HypnosisQteSyncPacket;

@Mod.EventBusSubscriber(modid="changed_synergy")
public final class DirectSpeechRegression {
    private static int checks;
    private static ServerLevel level;
    private static final List<ChangedEntity> creatures = new ArrayList<>();
    private static final Map<UUID,List<Component>> messages = new HashMap<>();
    @SubscribeEvent public static void run(ServerStartedEvent event) {
        try {
            level=event.getServer().overworld();
            ChangedSynergyConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
            level.getGameRules().getRule(ChangedSynergyGameRules.NPC_DIALOGUE).set(true,event.getServer());
            level.getGameRules().getRule(ChangedSynergyGameRules.HYPNOSIS_QTE).set(true,event.getServer());
            for(var p:BlockPos.betweenClosed(-4,159,-4,4,163,4))level.setBlock(p,(p.getY()==159?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
            var victim=player("SpeechListener");var observer=player("SpeechObserver");
            ChangedSynergyConfig.COMMON.npcDialogueUsesTranslator.set(false);
            ChangedSynergyConfig.COMMON.personalityDialogueChance.set(0.0);
            ChangedSynergyConfig.COMMON.telepathyEnabled.set(false);
            for(String id:List.of("changed:dark_latex_wolf_partial","changed:latex_human","changed_addon:latex_snow_leopard_partial")) {
                var type=ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(id));
                if(type==null||!ResourceLocation.parse(id).equals(ForgeRegistries.ENTITY_TYPES.getKey(type))) {
                    check(id.startsWith("changed_addon:")&&!net.minecraftforge.fml.ModList.get().isLoaded("changed_addon"),"only absent optional Addon speaker is skipped");continue;
                }
                var speaker=mob(id,victim,observer);
                check(NpcDialogue.canUnderstand(speaker,victim),id+" speaks without telepathy or translator");
                check(!TelepathyService.canUnderstand(victim),"direct speech grants no permanent understanding");
                messages.get(victim.getUUID()).clear();NpcDialogue.context(speaker,victim,"boundary.greeting");
                check(last(victim).contains("dialogue.changed_synergy.context.boundary.greeting"),"context dialogue uses direct speech: "+id+" line="+last(victim)
                        +" profile="+CreatureSocialProfile.of(speaker)+" trusted="+CreaturePersonality.hasTrustedRelationship(speaker,victim)
                        +" body="+NativeRoleplayService.validBody(victim,net.parkabird.changedsynergy.compat.TrueTransfurCompat.interactionBody(victim)));
            }
            var ordinary=mob("changed:dark_latex_wolf_male",victim,observer);
            var cat=mob("changed:latex_hypno_cat",victim,observer);
            var otherCat=mob("changed:latex_hypno_cat",victim,observer);
            check(!NpcDialogue.canUnderstand(ordinary,victim)&&!NpcDialogue.canUnderstand(cat,victim),"untranslated idle creatures remain unintelligible");
            ChangedSynergyConfig.COMMON.telepathyEnabled.set(true);
            check(!NpcDialogue.canUnderstand(cat,victim),"idle hypno cat does not bypass translation");
            check(HypnosisQteService.startFriendlyPractice(cat,victim),"real hypnosis practice starts");
            check(NpcDialogue.canUnderstand(cat,victim),"hypnosis target understands its active hypnotist");
            check(!NpcDialogue.canUnderstand(cat,observer)&&!NpcDialogue.canUnderstand(otherCat,victim),"no understanding leaks to bystanders or other hypno cats");
            ChangedSynergyConfig.COMMON.telepathyEnabled.set(false);
            check(!NpcDialogue.canUnderstand(cat,victim),"mental speech still respects the telepathy switch");
            ChangedSynergyConfig.COMMON.telepathyEnabled.set(true);
            var field=HypnosisQteService.class.getDeclaredField("SESSIONS");field.setAccessible(true);
            var sessions=(Map<?,?>)field.get(null);var session=sessions.get(victim.getUUID());
            var finish=HypnosisQteService.class.getDeclaredMethod("finish",ServerPlayer.class,session.getClass(),int.class);finish.setAccessible(true);
            messages.get(victim.getUUID()).clear();messages.get(observer.getUUID()).clear();
            finish.invoke(null,victim,session,HypnosisQteSyncPacket.SUCCESS);
            check(!HypnosisQteService.isHypnotizing(cat,victim)&&!NpcDialogue.canUnderstand(cat,victim),"understanding ends with the actual hypnosis session");
            check(last(victim).contains("dialogue.changed_synergy.hypnosis.hypno_cat.hypnosis_play_success"),"hypnosis result remains readable after session removal: "+last(victim));
            check(!last(observer).contains("dialogue.changed_synergy.hypnosis"),"result understanding remains private to the target");
            victim.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG)
                    .getCompound("ChangedSynergyTelepathy").putBoolean("UnderstandsNetwork",true);
            check(NpcDialogue.canUnderstand(cat,victim)&&NpcDialogue.canUnderstand(ordinary,victim),"earned telepathy still understands idle creatures");
            ChangedSynergyConfig.COMMON.telepathyEnabled.set(false);
            check(!NpcDialogue.canUnderstand(cat,victim)&&!NpcDialogue.canUnderstand(ordinary,victim),"disabled telepathy keeps its ordinary restrictions");
            if(net.minecraftforge.fml.ModList.get().isLoaded("changed_addon")) {
                var translator=new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("changed_addon:translator"))));
                translator.getOrCreateTag().putBoolean("Enabled",true);victim.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,translator);ChangedSynergyConfig.COMMON.npcDialogueUsesTranslator.set(true);
                check(NpcDialogue.canUnderstand(cat,victim)&&NpcDialogue.canUnderstand(ordinary,victim),"enabled translator remains an independent path");
                translator.getOrCreateTag().putBoolean("Enabled",false);
                check(!NpcDialogue.canUnderstand(cat,victim),"disabled translator cannot make idle hypno cat intelligible");
            }
            System.out.println("COMMUNITY_REGRESSION_PASS: "+checks+" direct speech, hypnosis recipient/result isolation and translator checks");
        } catch(Throwable failure){System.err.println("DIRECT_SPEECH_REGRESSION_FAIL");failure.printStackTrace();}
        finally{for(var creature:creatures)creature.discard();event.getServer().halt(false);}
    }
    @SuppressWarnings("unchecked") private static ServerPlayer player(String name)throws Exception {
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),name));
        messages.put(player.getUUID(),new ArrayList<>());
        player.connection=new ServerGamePacketListenerImpl(level.getServer(),new Connection(PacketFlow.SERVERBOUND),player){
            @Override public void send(Packet<?> packet,net.minecraft.network.PacketSendListener listener){
                if(packet instanceof ClientboundSystemChatPacket chat)messages.get(player.getUUID()).add(chat.content());
                super.send(packet,listener);
            }
        };
        var map=(Map<UUID,ServerPlayer>)ObfuscationReflectionHelper.findField(net.minecraft.server.players.PlayerList.class,"f_11197_").get(level.getServer().getPlayerList());
        map.put(player.getUUID(),player);level.addNewPlayer(player);player.moveTo(.5,160,.5);return player;
    }
    private static ChangedEntity mob(String id,ServerPlayer...players) {
        var mob=(ChangedEntity)Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(id))).create(level);
        mob.moveTo(2.5,160,2.5);level.addFreshEntity(mob);creatures.add(mob);
        for(var player:players){CreaturePersonality.establishRelationship(mob,player);CreaturePersonality.setFamiliarity(mob,player,20);}
        return mob;
    }
    private static String last(ServerPlayer player){var lines=messages.get(player.getUUID());return lines.isEmpty()?"":Component.Serializer.toJson(lines.get(lines.size()-1));}
    private static void check(boolean value,String description){if(!value)throw new AssertionError(description);checks++;System.out.println("DIRECT_SPEECH_PASS: "+description);}
}
