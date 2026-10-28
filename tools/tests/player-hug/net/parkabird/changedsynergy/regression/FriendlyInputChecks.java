package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.LivingEntity;
import net.parkabird.changedsynergy.ai.PatAnimationService;
import net.parkabird.changedsynergy.ability.SwordfishRushAbility;
import net.parkabird.changedsynergy.init.ChangedSynergyEntities;

final class FriendlyInputChecks {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void run(MinecraftServer server)throws Exception{
        var level=server.overworld();
        var human=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"Oroshiagito"));
        human.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),human);
        human.setPos(0,100,0);
        var mob=net.ltxprogrammer.changed.init.ChangedEntities.LATEX_BEE.get().create(level);
        mob.setPos(.2,100,0);mob.setNoAi(true);
        PatAnimationService.refreshContinuous(human,mob);
        check(PatAnimationService.isContinuous(human,mob),"human pat session starts");
        var sessions=PatAnimationService.class.getDeclaredField("SESSIONS");sessions.setAccessible(true);
        var map=(java.util.Map<?,?>)sessions.get(null);Object session=map.get(human.getUUID());
        var snapshot=mob.getPersistentData().copy();
        if(net.minecraftforge.fml.ModList.get().isLoaded("changed_addon")){
            Class<?> eventType=Class.forName("net.foxyas.changedaddon.process.features.ProcessPatFeature$GlobalPatReactionEvent");
            var constructor=eventType.getConstructor(net.minecraft.world.level.LevelAccessor.class,LivingEntity.class,
                    net.minecraft.world.InteractionHand.class,LivingEntity.class,net.minecraft.world.phys.Vec3.class);
            var bridge=Class.forName("net.parkabird.changedsynergy.compat.addon.ChangedAddonSocialEvents").getMethod("onPat",eventType);
            for(int i=0;i<120;i++){
                ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(level.getGameTime()+1);
                if(i%6==0)PatAnimationService.refreshContinuous(human,mob);
                bridge.invoke(null,constructor.newInstance(level,human,net.minecraft.world.InteractionHand.MAIN_HAND,mob,null));
                check(map.get(human.getUUID())==session,"Addon repeats must not replace the continuous animation");
                check(snapshot.equals(mob.getPersistentData()),"Addon repeats must not repeat dialogue or relationship effects");
            }
        }
        PatAnimationService.stop(human);
        check(!PatAnimationService.isContinuous(human,mob),"release ends the continuous stroke");
        PatAnimationService.refreshContinuous(human,mob);
        check(map.get(human.getUUID())!=session,"a fresh press starts a new session");
        PatAnimationService.stop(human);

        var fish=ChangedSynergyEntities.LATEX_SWORDFISH.get().create(level);
        fish.setCustomName(Component.literal("Latex Swordfish"));
        var source=new SwordfishRushAbility.RushDamageSource(level.damageSources().mobAttack(fish));
        var message=source.getLocalizedDeathMessage(human);
        var translated=(net.minecraft.network.chat.contents.TranslatableContents)message.getContents();
        check(translated.getKey().equals("death.attack.changed_synergy.swordfish_rush"),"rush has its own death message");
        check(((Component)translated.getArgs()[0]).getString().equals("Latex Swordfish")
                &&((Component)translated.getArgs()[1]).getString().equals("Oroshiagito"),"death message uses actual killer and victim names");
        check(source.typeHolder()==level.damageSources().mobAttack(fish).typeHolder()&&source.getEntity()==fish,
                "rush preserves native attack attribution and damage type");
        human.setCustomName(Component.literal("Visitor"));fish.setCustomName(Component.literal("Pointy"));
        message=source.getLocalizedDeathMessage(human);
        translated=(net.minecraft.network.chat.contents.TranslatableContents)message.getContents();
        check(((Component)translated.getArgs()[0]).getString().equals("Pointy"),"custom swordfish names are retained");
        System.out.println("FRIENDLY_INPUT_SERVER_PASS: continuous human pats, optional Addon bridge, fresh strokes and attributed Swordfish deaths");
    }
}
