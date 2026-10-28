package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.ltxprogrammer.changed.entity.TransfurCause;
import net.ltxprogrammer.changed.entity.TransfurContext;
import net.ltxprogrammer.changed.entity.ai.LatexAssimilationDecision;
import net.ltxprogrammer.changed.fluid.AbstractLatexFluid;
import net.ltxprogrammer.changed.init.ChangedBlocks;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.process.TransfurEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.event.FactionReputationEvents;

final class DarkWhiteEnvironmentRegression {
    private static int checks;
    static void run(MinecraftServer server) {
        var config=ChangedSynergyConfig.COMMON;boolean peace=config.peacefulDarkWhiteRelations.get(),protection=config.darkWhiteEnvironmentProtection.get();
        var level=server.overworld();var pos=new BlockPos(1200,160,1200);level.getChunkAt(pos);
        var dark=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
        var white=ChangedEntities.PURE_WHITE_LATEX_WOLF.get().create(level);
        dark.setPos(1200.5,160,1200.5);white.setPos(dark.position());dark.setNoAi(true);white.setNoAi(true);
        var player=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"EnvironmentCheck"));
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);player.setPos(dark.position());
        net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class,player,0,"f_8921_");
        try {
            for(boolean peaceful:new boolean[]{true,false}) for(boolean protectedEnvironment:new boolean[]{true,false}) {
                config.peacefulDarkWhiteRelations.set(peaceful);config.darkWhiteEnvironmentProtection.set(protectedEnvironment);
                check(LatexCreatureCombatRules.arePeacefulCompetitors(dark,white)==peaceful,"Peaceful competitor toggle is independent");
                check(LatexCreatureCombatRules.areRivals(dark,white)!=peaceful,"Disabled peace restores native rivalry");
                level.setBlock(pos,ChangedBlocks.WHITE_LATEX_FLUID.get().defaultBlockState(),2);
                contact(dark,protectedEnvironment,()->level.getBlockState(pos).entityInside(level,pos,dark),"Dark NPC in White fluid");
                ProcessTransfur.setPlayerTransfurVariant(player,ChangedTransfurVariants.DARK_LATEX_WOLF_MALE.get(),null,1,false);
                contact(player,protectedEnvironment,()->level.getBlockState(pos).entityInside(level,pos,player),"Dark player in White fluid");
                var exposure=new TransfurEvents.LatexAssimilationDecisionEvent(dark,LatexAssimilationDecision.fromBlockOrItem(
                        ChangedTransfurVariants.PURE_WHITE_LATEX_WOLF.get(),TransfurContext.hazard(TransfurCause.FLOOR_HAZARD),10F));
                FactionReputationEvents.onLatexEnvironmentalHazard(exposure);
                check(exposure.isCanceled()==protectedEnvironment,"White environmental transfur respects its switch");
                var intentional=new TransfurEvents.LatexAssimilationDecisionEvent(dark,LatexAssimilationDecision.fromBlockOrItem(
                        ChangedTransfurVariants.PURE_WHITE_LATEX_WOLF.get(),TransfurContext.hazard(TransfurCause.SYRINGE),10F));
                FactionReputationEvents.onLatexEnvironmentalHazard(intentional);
                check(!intentional.isCanceled(),"Deliberate syringe use is unchanged");
                level.setBlock(pos,ChangedBlocks.DARK_LATEX_FLUID.get().defaultBlockState(),2);
                contact(white,protectedEnvironment,()->AbstractLatexFluid.onEntityTick(new LivingEvent.LivingTickEvent(white)),"White NPC in Dark fluid");
                ProcessTransfur.setPlayerTransfurVariant(player,ChangedTransfurVariants.PURE_WHITE_LATEX_WOLF.get(),null,1,false);
                contact(player,protectedEnvironment,()->AbstractLatexFluid.onEntityTick(new LivingEvent.LivingTickEvent(player)),"White player in Dark fluid");
                float health=dark.getHealth();dark.invulnerableTime=0;dark.hurt(dark.damageSources().generic(),2);
                check(dark.getHealth()<health,"Generic damage remains active");
            }
            ProcessTransfur.removePlayerTransfurVariant(player);
            config.darkWhiteEnvironmentProtection.set(true);
            var human=new TransfurEvents.LatexAssimilationDecisionEvent(player,LatexAssimilationDecision.fromBlockOrItem(
                    ChangedTransfurVariants.PURE_WHITE_LATEX_WOLF.get(),TransfurContext.hazard(TransfurCause.FLOOR_HAZARD),10F));
            FactionReputationEvents.onLatexEnvironmentalHazard(human);
            check(!human.isCanceled(),"An untransformed human does not gain Dark/White environment immunity");
            System.out.println("DARK_WHITE_ENVIRONMENT_REGRESSION_PASS: "+checks+" independent switch combinations, actual NPC/player fluid damage, environmental exposure and unchanged item/human/generic damage checks");
        } finally {
            config.peacefulDarkWhiteRelations.set(peace);config.darkWhiteEnvironmentProtection.set(protection);
            ProcessTransfur.removePlayerTransfurVariant(player);player.discard();dark.discard();white.discard();level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
    }
    private static void contact(LivingEntity entity,boolean protectedEnvironment,Runnable action,String name) {
        entity.setHealth(entity.getMaxHealth());entity.invulnerableTime=0;float health=entity.getHealth();action.run();
        check((entity.getHealth()==health)==protectedEnvironment,name+" expected protection="+protectedEnvironment+", health="+health+" -> "+entity.getHealth());
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
}
