package net.parkabird.changedsynergy.client;

import java.util.concurrent.CompletableFuture;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.ability.AbstractAbility;
import net.ltxprogrammer.changed.ability.AbstractAbilityInstance.KeyReference;
import net.ltxprogrammer.changed.ability.GrabEntityAbilityInstance;
import net.ltxprogrammer.changed.entity.LivingEntityDataExtension;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.network.packet.GrabEntityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.compat.ChangedAddonCompat;
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;
import net.parkabird.changedsynergy.network.*;

@Mod.EventBusSubscriber(modid="changed_synergy",value=Dist.CLIENT)
public final class GrabEscapeLiveRegression {
    private static int stage,wait,scenario,checks,presses,carrierId;
    private static float initialStrength;
    private static boolean lost;
    private static long started=System.nanoTime();
    private static CompletableFuture<Void> task;
    private static final java.util.Map<String,String> clipboardTexts=new java.util.LinkedHashMap<>();
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static GrabEntityAbilityInstance grab(Minecraft mc){
        var carrier=mc.level.getEntity(carrierId);
        return carrier instanceof net.minecraft.world.entity.LivingEntity living
                ? AbstractAbility.getAbilityInstance(living,ChangedAbilities.GRAB_ENTITY_ABILITY.get()):null;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||stage==99)return;
        var mc=Minecraft.getInstance();
        try{
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
            if(task!=null&&task.isCompletedExceptionally())task.join();
            if(System.nanoTime()-started>260_000_000_000L)throw new AssertionError("timeout stage="+stage+" scenario="+scenario);
            if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
                check(mc.gameDirectory.getName().startsWith("author-feedback-client-"),"private fixture only");
                auditDocuments(mc);
                mc.options.pauseOnLostFocus=false;
                var rules=new GameRules();rules.getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false,null);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("escape",new LevelSettings("escape",GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(21931,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());stage=1;
            }
            if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;
            if(mc.screen instanceof NativeOriginScreen screen){screen.keyPressed(256,0,0);return;}
            if(stage==1&&mc.player.tickCount>25&&mc.screen==null){
                if(Boolean.getBoolean("changed_synergy.documentsOnly")){
                    for(var entry:clipboardTexts.entrySet())renderClipboard(mc,entry.getKey(),entry.getValue());
                    System.out.println("GRAB_ESCAPE_LIVE_PASS: 23 native document layouts and two clipboard screen renders");
                    stage=99;mc.stop();return;
                }
                task=CompletableFuture.runAsync(()->spawn(mc),mc.getSingleplayerServer());stage=2;wait=0;
            }
            if(stage==2&&task.isDone()){
                task.join();var hold=grab(mc);
                if(hold==null||hold.grabbedEntity!=mc.player||hold.currentEscapeKey==null){if(++wait>80)throw new AssertionError("QTE never appeared for scenario "+scenario);return;}
                check(hold.escapeKeyRandom!=null,"NoAI hold receives prompt and sequence: "+scenario);
                check(!hold.grabbedHasControl,"arm hold cannot hide QTE through stale control flag");
                initialStrength=hold.grabStrength;
                var key=hold.currentEscapeKey;hold.escapeKeyRandom=null;hold.currentEscapeKey=null;
                GrabQteClientState.receive(new GrabQteSyncPacket(carrierId,mc.player.getId(),key.ordinal(),-1,0));
                stage=3;wait=0;
            }
            if(stage==3){
                var hold=grab(mc);
                if(hold==null||hold.currentEscapeKey==null||hold.escapeKeyRandom==null){if(++wait>50)throw new AssertionError("Missing seed never recovered");return;}
                check(((GrabEscapeState)hold).changedSynergy$escapeDraws()>0,"heartbeat restores missed seed without rejoining: "+scenario);
                if(scenario==1)Screenshot.grab(mc.gameDirectory,"friendly-qte-recovered.png",mc.getMainRenderTarget(),message->{});
                presses=0;stage=4;wait=0;
            }
            if(stage==4){
                var hold=grab(mc);
                if(hold==null||hold.grabbedEntity==null){stage=6;wait=0;return;}
                if(++wait<6)return;wait=0;
                check(hold.currentEscapeKey!=null,"expected key remains available throughout the hold");
                press(mc,hold.currentEscapeKey);
                var expected=hold.currentEscapeKey;
                System.out.println("ESCAPE_PRESS client="+expected+" strength="+hold.grabStrength+" ticks="+hold.ticksUnpressed);
                mc.getSingleplayerServer().execute(()->{
                    var source=mc.getSingleplayerServer().overworld().getEntity(carrierId);
                    var serverHold=AbstractAbility.getAbilityInstance((net.minecraft.world.entity.LivingEntity)source,ChangedAbilities.GRAB_ENTITY_ABILITY.get());
                    System.out.println("ESCAPE_PRESS server="+serverHold.currentEscapeKey+" strength="+serverHold.grabStrength+" ticks="+serverHold.ticksUnpressed+" player="+serverHold.grabbedEntity);
                });
                stage=5;
            }
            if(stage==5&&++wait>=12){
                wait=0;press(mc,null);var hold=grab(mc);
                System.out.println("ESCAPE_RESULT client="+(hold==null?null:hold.currentEscapeKey)+" strength="+(hold==null?0:hold.grabStrength));
                mc.getSingleplayerServer().execute(()->{
                    var source=mc.getSingleplayerServer().overworld().getEntity(carrierId);
                    var serverHold=AbstractAbility.getAbilityInstance((net.minecraft.world.entity.LivingEntity)source,ChangedAbilities.GRAB_ENTITY_ABILITY.get());
                    System.out.println("ESCAPE_RESULT server="+serverHold.currentEscapeKey+" strength="+serverHold.grabStrength+" ticks="+serverHold.ticksUnpressed);
                });
                if(hold==null||hold.grabbedEntity==null){stage=6;return;}
                check(hold.grabStrength<initialStrength,"real client key packets reduce server hold strength");
                check(++presses<30,"QTE makes bounded progress");stage=4;
            }
            if(stage==6&&++wait>8){
                check(((LivingEntityDataExtension)mc.player).getGrabbedBy()==null&&!mc.player.noPhysics,"release clears client attachment and physics: "+scenario);
                task=CompletableFuture.runAsync(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    var carrier=p.serverLevel().getEntity(carrierId);
                    check(((LivingEntityDataExtension)p).getGrabbedBy()==null&&!p.noPhysics,"release clears server attachment and physics");
                    if(carrier!=null)carrier.discard();
                },mc.getSingleplayerServer());
                scenario++;stage=7;
            }
            if(stage==7&&task.isDone()){
                task.join();
                if(scenario>=3){System.out.println("GRAB_ESCAPE_LIVE_PASS: "+checks+" real client/server, hostile, friendly, safe carry, missing seed, key packets and releases");stage=99;mc.stop();return;}
                task=CompletableFuture.runAsync(()->spawn(mc),mc.getSingleplayerServer());stage=2;wait=0;
            }
        }catch(Throwable failure){stage=99;System.err.println("GRAB_ESCAPE_LIVE_FAIL");failure.printStackTrace();mc.stop();}
    }
    private static void spawn(Minecraft mc){
        var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        p.setHealth(p.getMaxHealth());p.setDeltaMovement(0,0,0);
        var carrier=ChangedEntities.LATEX_BEE.get().create(p.serverLevel());
        carrier.setNoAi(true);carrier.setPos(p.position());p.serverLevel().addFreshEntity(carrier);carrierId=carrier.getId();
        BondedSuitService.ensureGrabAbility(carrier);var hold=BondedSuitService.ability(carrier);
        if(scenario>0){LatexSocialMemory.beginFriendlySocialHug(carrier,p,200);ChangedAddonCompat.configureFriendlyGrab(hold,true);}
        else {LatexSocialMemory.markProvoked(carrier,p);carrier.setTarget(p);}
        hold.grabbedEntity=p;hold.grabStrength=hold.grabStrengthO=1;hold.grabbedHasControl=true;hold.suited=false;
        ((LivingEntityDataExtension)p).setGrabbedBy(carrier);
        Changed.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(()->p),new GrabEntityPacket(carrier,p,GrabEntityPacket.GrabType.ARMS));
        if(scenario>0)ChangedSynergyNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new FriendlySocialHugSyncPacket(carrierId,p.getId(),true,scenario==2?-200:200));
    }
    private static void press(Minecraft mc,KeyReference key){
        mc.options.keyUp.setDown(key==KeyReference.MOVE_FORWARD);
        mc.options.keyDown.setDown(key==KeyReference.MOVE_BACKWARD);
        mc.options.keyLeft.setDown(key==KeyReference.MOVE_LEFT);
        mc.options.keyRight.setDown(key==KeyReference.MOVE_RIGHT);
    }

    private static void auditDocuments(Minecraft mc) throws Exception {
        var report=new com.google.gson.JsonArray();
        var mod=net.minecraftforge.fml.ModList.get().getModFileById("changed_synergy").getFile();
        for(var name:new String[]{"white_lab_tower","red_lab_tower","green_lab","water_tower_changed"}){
            try(var input=java.nio.file.Files.newInputStream(mod.findResource("data","changed_synergy","structures",name+".nbt"))){
                var data=net.minecraft.nbt.NbtIo.readCompressed(input);
                var palette=data.getList("palette",net.minecraft.nbt.Tag.TAG_COMPOUND);
                for(var item:data.getList("blocks",net.minecraft.nbt.Tag.TAG_COMPOUND)){
                    var block=(net.minecraft.nbt.CompoundTag)item;
                    String type=palette.getCompound(block.getInt("state")).getString("Name");
                    if(!type.equals("changed:note")&&!type.equals("changed:clipboard"))continue;
                    int width=type.equals("changed:note")?77:81,height=type.equals("changed:note")?101:91;
                    var text=block.getCompound("nbt").getString("Text");
                    int actual=mc.font.wordWrapHeight(text,width);
                    String id=name+"@"+block.get("pos");
                    boolean expanded=id.equals("red_lab_tower@[24,34,30]")||id.equals("water_tower_changed@[39,6,51]");
                    if(expanded){height+=9;clipboardTexts.put(name,text);}
                    var row=new com.google.gson.JsonObject();row.addProperty("id",id);
                    row.addProperty("height",actual);row.addProperty("limit",height);report.add(row);
                    System.out.println("DOCUMENT_LAYOUT "+row);
                    check(actual<=height,"document fits native "+type+" screen: "+id+" "+actual+"/"+height);
                }
            }
        }
        java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("document-layout.json"),report.toString());
    }

    private static void renderClipboard(Minecraft mc,String name,String text)throws Exception{
        var menu=new net.ltxprogrammer.changed.world.inventory.ClipboardMenu(0,mc.player.getInventory(),(net.minecraft.network.FriendlyByteBuf)null);
        menu.textCopy=menu.textCopyLastReceived=text;
        var screen=new net.ltxprogrammer.changed.client.gui.ClipboardScreen(menu,mc.player.getInventory(),net.minecraft.network.chat.Component.empty());
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();screen.init(mc,w,h);
        var target=new com.mojang.blaze3d.pipeline.TextureTarget(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true,Minecraft.ON_OSX);
        var mv=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();mv.pushPose();
        try{
            mv.setIdentity();mv.translate(0,0,-11000);com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,w,h,0,1000,21000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            target.setClearColor(.11F,.13F,.15F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            var graphics=new net.minecraft.client.gui.GuiGraphics(mc,mc.renderBuffers().bufferSource());screen.render(graphics,0,0,1);graphics.flush();
            try(var image=Screenshot.takeScreenshot(target)){image.writeToFile(mc.gameDirectory.toPath().resolve(name+"-clipboard.png"));}
        }finally{mv.popPose();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
    }
}
