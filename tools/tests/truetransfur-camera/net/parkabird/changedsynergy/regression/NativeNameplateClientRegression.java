package net.parkabird.changedsynergy.regression;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event.Result;
import net.parkabird.changedsynergy.ChangedSynergyClientConfig;
import net.parkabird.changedsynergy.client.NameVisibilityClientEvents;
import net.parkabird.changedsynergy.origin.NativeOriginPolicy;

final class NativeNameplateClientRegression {
    private static int checks;
    static void run(Minecraft mc) {
        var config=ChangedSynergyClientConfig.CLIENT.showNativePlayerNames;
        boolean old=config.get();var natives=NativeOriginPolicy.clientNatives;
        var previous=ProcessTransfur.getPlayerTransfurVariant(mc.player);
        var npc=ChangedEntities.WHITE_LATEX_WOLF_MALE.get().create(mc.level);
        npc.setPos(mc.player.position());npc.setCustomName(Component.literal("Named companion"));
        try {
            var form=ProcessTransfur.setPlayerTransfurVariant(mc.player,ChangedTransfurVariants.DARK_LATEX_WOLF_MALE.get());
            NativeOriginPolicy.clientNatives=Set.of(mc.player.getUUID());
            config.set(false);
            check(NameVisibilityClientEvents.hidesNativePlayerName(mc.player),"Disabled nameplate setting covers the actual native player");
            check(NameVisibilityClientEvents.hidesNativePlayerName(form.getChangedEntity()),"Disabled setting also covers the player's rendered Changed body");
            var event=event(mc,mc.player);MinecraftForge.EVENT_BUS.post(event);
            check(event.getResult()==Result.DENY,"Full Forge and TrueTransfur render event cannot restore a disabled head-name label");
            check(!NameVisibilityClientEvents.hidesNativePlayerName(npc),"Named NPC companion is unaffected");
            var npcEvent=event(mc,npc);NameVisibilityClientEvents.onRenderNameTag(npcEvent);
            check(npcEvent.getResult()!=Result.DENY&&npc.getCustomName().getString().equals("Named companion"),"NPC custom name and ordinary nearby label remain intact");
            NativeOriginPolicy.clientNatives=Set.of();
            check(!NameVisibilityClientEvents.hidesNativePlayerName(mc.player),"Ordinarily transformed human-origin player is unaffected");
            NativeOriginPolicy.clientNatives=Set.of(mc.player.getUUID());
            config.set(true);
            check(!NameVisibilityClientEvents.hidesNativePlayerName(mc.player),"Enabling the setting restores the normal TT visibility decision");
            config.set(false);ProcessTransfur.removePlayerTransfurVariant(mc.player);
            check(!NameVisibilityClientEvents.hidesNativePlayerName(mc.player),"Human body is unaffected even when the saved origin is native");
            System.out.println("NATIVE_NAMEPLATE_CLIENT_REGRESSION_PASS: "+checks+" native-player render-event, rendered-body, NPC, human-origin and configuration checks");
        } finally {
            config.set(old);NativeOriginPolicy.clientNatives=natives;npc.discard();
            if(previous==null)ProcessTransfur.removePlayerTransfurVariant(mc.player);
            else ProcessTransfur.setPlayerTransfurVariant(mc.player,previous.getParent());
        }
    }
    private static RenderNameTagEvent event(Minecraft mc,Entity entity) {
        return new RenderNameTagEvent(entity,entity.getDisplayName(),mc.getEntityRenderDispatcher().getRenderer(entity),
                new PoseStack(),mc.renderBuffers().bufferSource(),15728880,0);
    }
    private static void check(boolean ok,String message) {if(!ok)throw new AssertionError(message);checks++;}
}
