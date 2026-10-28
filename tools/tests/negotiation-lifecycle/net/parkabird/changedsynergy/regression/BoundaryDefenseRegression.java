package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.parkabird.changedsynergy.ai.*;

final class BoundaryDefenseRegression {
    static void run(MinecraftServer server) {
        var level=server.overworld();
        var player=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"BoundaryCheck"));
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);
        player.setPos(192.5,160,192.5);level.getChunkAt(player.blockPosition());
        var victim=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
        var ally=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
        victim.setPos(player.position().add(1,0,0));ally.setPos(player.position().add(2,0,0));
        victim.tickCount=1000;victim.setNoAi(true);level.addFreshEntity(victim);level.addFreshEntity(ally);
        var personality=new CompoundTag();personality.putInt("Version",7);
        personality.putInt("Traits",(1<<CreaturePersonality.Trait.POLITE.ordinal())|(1<<CreaturePersonality.Trait.CALM.ordinal()));
        personality.putInt("DominantTrait",CreaturePersonality.Trait.POLITE.ordinal());
        victim.getPersistentData().put("ChangedSynergyPersonality",personality);
        var intent=new CompoundTag();intent.putInt("Value",HumanIntent.GREET.ordinal());victim.getPersistentData().put("ChangedSynergyHumanIntent",intent);
        var otherIntent=new CompoundTag();otherIntent.putInt("Value",HumanIntent.SEEK_HOST.ordinal());ally.getPersistentData().put("ChangedSynergyHumanIntent",otherIntent);
        var identity=new net.parkabird.changedsynergy.origin.NativeOriginData.Identity();identity.state="human";
        net.parkabird.changedsynergy.origin.NativeOriginData.get(server).players.put(player.getUUID(),identity);
        try {
            FactionReputation.set(victim,player,0);
            check(PoliteHumanInteraction.beginApproach(victim,player),"Real polite greeting begins");
            var revenge=new HurtByTargetGoal(victim).setAlertOthers();
            for(int hit=0;hit<2;hit++) {
                victim.invulnerableTime=0;
                check(victim.hurt(level.damageSources().playerAttack(player),1),"Tolerated empty-hand hit follows real Forge damage events");
                check(HumanBoundaryService.isBackingOff(victim,player),"The approached creature accepts the boundary and backs off");
                check(ally.getTarget()==null&&!ally.isAggressive(),"Nearby same-species creatures do not become hostile to a tolerated interruption");
                check(!revenge.canUse(),"Native revenge AI cannot alert allies after an accepted boundary");
                check(FactionReputation.score(victim,player)==0,"Tolerated interruptions retain the existing reputation exemption");
            }
            victim.invulnerableTime=0;
            check(victim.hurt(level.damageSources().playerAttack(player),1),"A third punch still follows normal combat handling");
            check(!HumanBoundaryService.isBackingOff(victim,player)&&LatexSocialMemory.isProvoked(victim,player),"Repeated provocation is not an unlimited safe interruption");
            check(ally.getTarget()==player,"Actual repeated provocation still alerts nearby compatriots");
            System.out.println("BOUNDARY_DEFENSE_REGRESSION_PASS: real Forge damage, two tolerated warnings, native revenge suppression and repeated-provocation ally response");
        } finally {
            victim.discard();ally.discard();player.discard();
            net.parkabird.changedsynergy.origin.NativeOriginData.get(server).players.remove(player.getUUID());
        }
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
