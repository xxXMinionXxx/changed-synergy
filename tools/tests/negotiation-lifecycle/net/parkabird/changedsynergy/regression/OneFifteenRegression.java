package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.TransfurCause;
import net.ltxprogrammer.changed.entity.TransfurContext;
import net.ltxprogrammer.changed.entity.ai.LatexAssimilationDecision;
import net.ltxprogrammer.changed.init.*;
import net.ltxprogrammer.changed.item.Syringe;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.process.TransfurEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.compat.ChangedAddonCompat;
import net.parkabird.changedsynergy.compat.ChangedVanillaCompat;
import net.parkabird.changedsynergy.event.AssimilationEcologyEvents;
import net.parkabird.changedsynergy.init.ChangedSynergyEntities;
import net.parkabird.changedsynergy.init.ChangedSynergyTransfurVariants;

/** Real server conversion/release transitions, inventory protection and final biome data. */
final class OneFifteenRegression {
    private static int checks;
    static void run(MinecraftServer server)throws Exception {
        var rules=server.overworld().getGameRules();var config=ChangedSynergyConfig.COMMON;
        boolean kept=rules.getBoolean(ChangedGameRules.RULE_KEEP_BRAIN);
        boolean tokens=config.bondTokensRequired.get(),balanced=config.balancedRelationships.get();
        boolean protect=config.changedVanillaRelationshipAnimalProtection.get();
        try {
            config.bondTokensRequired.set(true);config.balancedRelationships.set(true);
            config.changedVanillaRelationshipAnimalProtection.set(true);
            voluntary(server);animalProtection(server);moth(server);
            if(ChangedAddonCompat.isLoaded()){totem(server);punitive(server);}
            else {
                var player=player(server);
                try {check(!ChangedAddonCompat.hasTransfurTotem(player),"optional totem check works without Addon");}
                finally {player.discard();}
            }
            System.out.println("ONE_FIFTEEN_SERVER_REGRESSION_PASS: "+checks+" checks; addon="+ChangedAddonCompat.isLoaded()+"; ChangedVanilla="+ChangedVanillaCompat.isLoaded());
        } finally {
            rules.getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(kept,server);
            config.bondTokensRequired.set(tokens);config.balancedRelationships.set(balanced);
            config.changedVanillaRelationshipAnimalProtection.set(protect);
        }
    }
    private static void voluntary(MinecraftServer server)throws Exception {
        var player=player(server);var source=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(server.overworld());
        source.setPos(player.position().add(.7,0,0));server.overworld().addFreshEntity(source);
        try {
            FactionReputation.set(source,player,0);
            check(!VoluntaryBondTransfurService.canOfferFriendly(source,player),"strangers cannot offer friendly transfur");
            CreaturePersonality.establishRelationship(source,player);CreaturePersonality.setFamiliarity(source,player,0);
            check(VoluntaryBondTransfurService.canOfferFriendly(source,player),"new acquaintance can offer voluntary transfur");
            ChangedSynergyConfig.COMMON.bondTokensRequired.set(false);ChangedSynergyConfig.COMMON.balancedRelationships.set(false);
            check(!VoluntaryBondTransfurService.canOffer(source,player),"relaxing ordinary transfur does not bypass existing bond closeness");
            ChangedSynergyConfig.COMMON.bondTokensRequired.set(true);ChangedSynergyConfig.COMMON.balancedRelationships.set(true);
            CreaturePersonality.setFamiliarity(source,player,-1);
            check(!VoluntaryBondTransfurService.canOfferFriendly(source,player),"a broken relationship is not voluntary consent");
            CreaturePersonality.setFamiliarity(source,player,0);
            server.overworld().getGameRules().getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(false,server);
            check(!VoluntaryBondTransfurService.select(source,player,true),"first click confirms rather than transforms");
            check(CreaturePersonality.familiarity(source,player)==0&&!ProcessTransfur.isPlayerTransfurred(player),"confirmation grants no affection or form");
            check(VoluntaryBondTransfurService.select(source,player,true),"confirmed acquaintance transfur completes");
            complete(player);int gained=CreaturePersonality.familiarity(source,player);
            check(player.isAlive()&&gained>=10&&gained<=20&&!LatexSocialMemory.isBonded(source,player),"voluntary change survives and grants a substantial one-time reward without a bond");
            check(ProcessTransfur.getPlayerTransfurVariantSafe(player).orElseThrow().getFormId().equals(source.getSelfVariant().getFormId()),"voluntary form matches source");
            ProcessTransfur.removePlayerTransfurVariant(player);
            check(!VoluntaryBondTransfurService.select(source,player,true)&&VoluntaryBondTransfurService.select(source,player,true),"a second voluntary change still requires confirmation");
            complete(player);check(CreaturePersonality.familiarity(source,player)==gained,"reversal/repeating does not refill milestone affection");
            var saved=source.saveWithoutId(new CompoundTag());var restored=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(server.overworld());restored.load(saved);
            check(CreaturePersonality.rewardVoluntaryTransfur(restored,player)==0,"milestone persists in creature save data");
            check(CreaturePersonality.forgetRelationship(restored,player),"ordinary acquaintance can be forgotten");
            CreaturePersonality.establishRelationship(restored,player);
            check(CreaturePersonality.rewardVoluntaryTransfur(restored,player)==0,"forgetting and reintroducing cannot refill milestone affection");
            restored.discard();
        } finally {player.discard();source.discard();}
    }
    private static void animalProtection(MinecraftServer server) {
        if(!ChangedVanillaCompat.isLoaded())return;
        var player=player(server);var cow=EntityType.COW.create(server.overworld());cow.setPos(player.position());
        for(var type:List.of(ChangedEntities.DARK_LATEX_WOLF_MALE.get(),ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse("changedvanilla:latex_cat")))) {
            var source=(ChangedEntity)type.create(server.overworld());source.setPos(player.position().add(1,0,0));
            try {
                CreaturePersonality.establishRelationship(source,player);FactionReputation.set(source,player,0);
                CreaturePersonality.setFamiliarity(source,player,11);
                check(!ChangedVanillaCompat.protectsAnimalNearRespectedHuman(source,cow),"new relation below Familiar does not become global animal protection");
                CreaturePersonality.setFamiliarity(source,player,12);
                check(ChangedVanillaCompat.protectsAnimalNearRespectedHuman(source,cow),"Familiar relation protects animals across both source namespaces");
                var decision=LatexAssimilationDecision.strong(LatexAssimilationDecision.Method.ABSORPTION,source.getSelfVariant(),TransfurContext.npcLatexHazard(source,TransfurCause.GRAB_ABSORB),1F);
                var event=new TransfurEvents.LatexAssimilationDecisionEvent(cow,decision);
                AssimilationEcologyEvents.onLatexAssimilation(event);
                check(event.isCanceled(),"animal assimilation is rejected even when its decision uses the source's native form");
                ChangedSynergyConfig.COMMON.changedVanillaRelationshipAnimalProtection.set(false);
                check(!ChangedVanillaCompat.protectsAnimalNearRespectedHuman(source,cow),"animal protection setting remains effective");
                ChangedSynergyConfig.COMMON.changedVanillaRelationshipAnimalProtection.set(true);
                CreaturePersonality.setFamiliarity(source,player,-1);
                check(!ChangedVanillaCompat.protectsAnimalNearRespectedHuman(source,cow),"lost trust removes relationship animal protection");
            } finally {source.discard();}
        }
        player.discard();cow.discard();
    }
    private static void moth(MinecraftServer server) {
        var level=server.overworld();var type=ChangedSynergyEntities.LATEX_SILK_MOTH.get();var form=ChangedSynergyTransfurVariants.LATEX_SILK_MOTH.get();
        var moth=type.create(level);moth.setPos(128.5,160,128.5);
        check(moth.getSelfVariant()==form&&HunterFaction.of(moth)==HunterFaction.LIGHT,"moth form and faction resolve to own species");
        check(LightFactionGroup.nativeRegions(level.registryAccess(),type).equals(List.of("forest")),"moth has a stable forest affiliation");
        check(SpawnPlacements.getPlacementType(type)==SpawnPlacements.Type.ON_GROUND,"moth has normal on-ground spawn checks");
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        var forest=registry.getHolderOrThrow(Biomes.FOREST).value();
        check(forest.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(s->s.type==type&&s.minCount==1&&s.maxCount==2),"forest final spawn list includes silk moth groups");
        var white=registry.get(ResourceLocation.parse("changed:white_latex_forest"));
        check(white.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().noneMatch(s->s.type==type),"white territory keeps its own population");
        for(var item:List.of(ChangedItems.LATEX_SYRINGE.get(),ChangedItems.LATEX_FLASK.get(),ChangedItems.LATEX_TIPPED_ARROW.get())) {
            var stack=Syringe.setPureVariant(new ItemStack(item),form.getFormId());
            check(Syringe.getVariant(stack)==form,"native variant-bearing item resolves silk moth: "+ForgeRegistries.ITEMS.getKey(item));
        }
        check(ChangedEntities.getEntityColorBack(ResourceLocation.parse("changed_synergy:latex_silk_moth"))==0xEADCC6,"native item renderer uses moth cream palette");
        moth.discard();
    }
    private static void totem(MinecraftServer server)throws Exception {
        var item=ForgeRegistries.ITEMS.getValue(ChangedAddonCompat.TRANSFUR_TOTEM);
        check(item!=null&&item!=net.minecraft.world.item.Items.AIR,"real Addon totem is loaded");
        for(int slot:new int[]{0,9,-1}) {
            var player=player(server);var source=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(server.overworld());
            try {
                var stack=new ItemStack(item);if(slot<0)player.setItemInHand(InteractionHand.OFF_HAND,stack);else player.getInventory().setItem(slot,stack);
                server.overworld().getGameRules().getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(false,server);
                var decision=LatexAssimilationDecision.strong(LatexAssimilationDecision.Method.REPLICATION,source.getSelfVariant(),TransfurContext.npcLatexHazard(source,TransfurCause.GRAB_REPLICATE),100F);
                check(ProcessTransfur.progressTransfur(player,decision),"forced conversion reaches native progression with totem at "+slot);
                complete(player);
                check(player.isAlive()&&ProcessTransfur.getPlayerTransfurVariantSafe(player).orElseThrow().willSurviveTransfur&&!TakeoverService.active(player),"totem preserves consciousness under false rule at "+slot);
                check(stack.getCount()==1,"passive totem protection does not consume it");
                var form=ProcessTransfur.getPlayerTransfurVariantSafe(player).orElseThrow();
                TransfurConsciousness.applyInvoluntaryOutcome(player,form);
                check(form.willSurviveTransfur,"direct secondary-conversion outcome honors totem too");
            } finally {player.discard();source.discard();}
        }
    }
    private static void punitive(MinecraftServer server)throws Exception {
        var type=Class.forName("net.parkabird.changedsynergy.ai.TakeoverService$Entry");
        var constructor=type.getDeclaredConstructor(ServerPlayer.class,ChangedEntity.class,TakeoverSession.class,String.class,CompoundTag.class);constructor.setAccessible(true);
        var release=TakeoverService.class.getDeclaredMethod("tryRelease",type,ServerPlayer.class,ChangedEntity.class,boolean.class);release.setAccessible(true);
        var field=TakeoverService.class.getDeclaredField("ACTIVE");field.setAccessible(true);var active=(Map<UUID,Object>)field.get(null);
        var nativeRelease=TakeoverSession.class.getDeclaredMethod("release",long.class,TakeoverSession.ReleaseReason.class);nativeRelease.setAccessible(true);
        server.overworld().getGameRules().getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(true,server);
        for(int scenario=0;scenario<3;scenario++) {
            var player=player(server);var source=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(server.overworld());
            source.setPos(player.position().add(.7,0,0));server.overworld().addFreshEntity(source);
            try {
                BondedSuitService.ensureGrabAbility(source);check(BondedSuitService.ability(source).suitEntity(player),"punitive fixture uses native wrap");
                long now=server.overworld().getGameTime();var session=TakeoverSession.ordinary(now,1200,false,true,TakeoverSession.SleepOutcome.RELEASE);
                check(session.failPunitiveNegotiation(now),"punitive sleep starts");nativeRelease.invoke(session,now,TakeoverSession.ReleaseReason.SLEEP);
                var entry=constructor.newInstance(player,source,session,"",new CompoundTag());
                var fatal=type.getDeclaredField("hostileFatalOutcome");fatal.setAccessible(true);fatal.setBoolean(entry,true);
                if(scenario==1)player.getInventory().setItem(9,new ItemStack(ForgeRegistries.ITEMS.getValue(ChangedAddonCompat.TRANSFUR_TOTEM)));
                if(scenario==2){var direct=type.getDeclaredField("directOutcome");direct.setAccessible(true);direct.setBoolean(entry,true);}
                active.put(player.getUUID(),entry);release.invoke(null,entry,player,source,true);
                if(scenario==0)check(!player.isAlive()&&source.isAlive(),"fatal punishment remains fatal without a totem");
                else check(player.isAlive()&&source.isRemoved()&&ProcessTransfur.getPlayerTransfurVariantSafe(player).orElseThrow().getFormId().equals(source.getSelfVariant().getFormId()),"totem or committed recovery grants the original carrier form and removes exactly that carrier");
                check(!TakeoverService.active(player)&&!player.noPhysics,"release clears input/physics lock");
            } finally {active.remove(player.getUUID());player.discard();source.discard();}
        }
    }
    private static ServerPlayer player(MinecraftServer server) {
        var player=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),"Feature115"));
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);
        net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class,player,0,"f_8921_");
        player.setPos(128.5,160,128.5);var identity=new net.parkabird.changedsynergy.origin.NativeOriginData.Identity();identity.state="human";
        net.parkabird.changedsynergy.origin.NativeOriginData.get(server).players.put(player.getUUID(),identity);return player;
    }
    private static void complete(ServerPlayer player)throws Exception {
        var form=ProcessTransfur.getPlayerTransfurVariant(player);if(form==null)return;
        var tick=form.getClass().getDeclaredMethod("tickTransfurProgress");tick.setAccessible(true);
        for(int i=0;i<800&&form.transfurProgression<1&&player.isAlive();i++)tick.invoke(form);
    }
    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);checks++;}
}
