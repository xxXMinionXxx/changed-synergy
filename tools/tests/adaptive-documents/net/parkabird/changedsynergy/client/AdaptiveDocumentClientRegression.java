package net.parkabird.changedsynergy.client;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.ltxprogrammer.changed.client.gui.*;
import net.ltxprogrammer.changed.world.inventory.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;

@Mod.EventBusSubscriber(modid="changed_synergy", value=Dist.CLIENT)
public final class AdaptiveDocumentClientRegression {
    private record Document(String name, String kind, String text) {}
    private static int stage, checks;
    private static final long START=System.nanoTime();
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||stage==99)return;
        var mc=Minecraft.getInstance();
        try{
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
            if(System.nanoTime()-START>240_000_000_000L)throw new AssertionError("document fixture timed out");
            if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
                check(mc.gameDirectory.getName().startsWith("author-feedback-client-"),"private world only");
                mc.options.pauseOnLostFocus=false;
                var rules=new GameRules();rules.getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false,null);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("documents",new LevelSettings("documents",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),
                    new WorldOptions(1932,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());stage=1;
            }
            if(stage==1&&mc.player!=null&&mc.level!=null&&mc.player.tickCount>20&&mc.getOverlay()==null){
                if(mc.screen instanceof NativeOriginScreen s){s.keyPressed(256,0,0);return;}
                var docs=documents();
                for(var size:new int[][]{{320,240},{640,360},{960,540}})for(var doc:docs){
                    var screen=open(mc,doc);
                    screen.init(mc,size[0],size[1]);
                    check(screen.getBackgroundWidth()<=size[0]-24&&screen.getBackgroundHeight()+52<=size[1],"sheet and controls fit "+doc.name());
                    int body=mc.font.wordWrapHeight(doc.text(),screen.getTextAreaWidth());
                    check(body<=screen.getTextAreaHeight()||screen.maxScroll()>0,"all text fits or scrolls "+doc.name());
                    check(screen.getMenu().textCopy.equals(doc.text()),"layout preserves every character "+doc.name());
                    screen.render(new GuiGraphics(mc,mc.renderBuffers().bufferSource()),0,0,1);
                    if(size[0]==640&&(doc.name().equals("CRISSCROSS")||doc.name().equals("TEMPLE")||doc.name().equals("SHARK")))capture(mc,screen,doc.name().toLowerCase());
                }
                editing(mc);
                System.out.println("ADAPTIVE_DOCUMENT_CLIENT_PASS: "+checks+" checks; "+docs.size()+" full documents at three GUI sizes; native editing, selection, scroll, cancel and save");
                stage=99;mc.stop();
            }
        }catch(Throwable failure){stage=99;System.err.println("ADAPTIVE_DOCUMENT_CLIENT_FAIL");failure.printStackTrace();mc.stop();}
    }
    private static List<Document> documents()throws Exception{
        var result=new ArrayList<Document>();
        var mod=net.minecraftforge.fml.ModList.get().getModFileById("changed_synergy").getFile();
        for(var name:new String[]{"white_lab_tower","red_lab_tower","green_lab","water_tower_changed"}){
            try(var input=Files.newInputStream(mod.findResource("data","changed_synergy","structures",name+".nbt"))){
                var nbt=NbtIo.readCompressed(input);var palette=nbt.getList("palette",Tag.TAG_COMPOUND);
                for(var entry:nbt.getList("blocks",Tag.TAG_COMPOUND)){
                    var block=(CompoundTag)entry;String kind=palette.getCompound(block.getInt("state")).getString("Name");
                    if(kind.equals("changed:note")||kind.equals("changed:clipboard")){
                        String text=block.getCompound("nbt").getString("Text");
                        String id=text.contains("MAINTENANCE MEMO")?"CRISSCROSS":text.contains("Raid-the-Pantry")?"TEMPLE":name+block.get("pos");
                        result.add(new Document(id,kind,text));
                    }
                }
                discs(nbt,result,name);
            }
        }
        check(result.size()>=29,"all notes, clipboards and data discs discovered");
        result.add(new Document("newlines","changed:clipboard","line\n".repeat(150)));
        result.add(new Document("unicode","changed:note","这是一份保留完整内容的观察记录。\n".repeat(25)));
        return result;
    }
    private static void discs(Tag tag,List<Document> docs,String name){
        if(tag instanceof CompoundTag compound){
            if(compound.getString("id").equals("minecraft:written_book")){
                var data=compound.getCompound("tag");String title=data.getString("title");
                int page=title.equals("A Doctor's Note")?5:title.equals("Farewell Note")?1:-1;
                if(page>=0){
                    var text=Component.Serializer.fromJson(data.getList("pages",Tag.TAG_STRING).getString(page)).getString();
                    int height=Minecraft.getInstance().font.wordWrapHeight(text,114);
                    check(height<=126,"revised book page fits native book: "+title+" height="+height);
                    System.out.println("REVISED_BOOK_LAYOUT "+title+"="+height+"/126");
                }
            }
            if(compound.getString("id").equals("changed:compact_disc")){
                var text=compound.getCompound("tag").getString("Text");
                if(!text.isEmpty())docs.add(new Document(text.contains("Aquatic Lineage")||text.contains("Latex Sharks may look relaxed")?"SHARK":name+"-disc-"+docs.size(),"disc",text));
            }
            for(var key:compound.getAllKeys())discs(compound.get(key),docs,name);
        }else if(tag instanceof ListTag list)for(var element:list)discs(element,docs,name);
    }
    private static AdaptiveDocumentScreen<?> open(Minecraft mc,Document doc){
        var inventory=mc.player.getInventory();TextMenu menu;TextMenuScreen<?> old;
        if(doc.kind().equals("changed:clipboard")){var m=new ClipboardMenu(0,inventory,(net.minecraft.network.FriendlyByteBuf)null);menu=m;old=new ClipboardScreen(m,inventory,Component.empty());}
        else if(doc.kind().equals("changed:note")){var m=new NoteMenu(0,inventory,(net.minecraft.network.FriendlyByteBuf)null);menu=m;old=new NoteScreen(m,inventory,Component.empty());}
        else {var m=new ComputerMenu(0,inventory,(net.minecraft.network.FriendlyByteBuf)null);menu=m;old=new ComputerTextScreen(m,inventory,Component.empty());}
        menu.textCopy=menu.textCopyLastReceived=doc.text();mc.setScreen(old);
        check(mc.screen instanceof AdaptiveDocumentScreen<?>,"native opening is adapted "+doc.name());
        return (AdaptiveDocumentScreen<?>)mc.screen;
    }
    private static final class Writable extends ClipboardMenu{
        String saved;
        Writable(Inventory inv){super(0,inv,(net.minecraft.network.FriendlyByteBuf)null);}
        @Override public boolean canEditExisting(){return true;}
        @Override public void setText(String value){saved=value;}
    }
    private static void editing(Minecraft mc){
        var menu=new Writable(mc.player.getInventory());menu.textCopy="old";
        var screen=new AdaptiveDocumentScreen<>(menu,mc.player,Component.empty());mc.setScreen(screen);screen.init(mc,320,240);
        var editor=(AdaptiveDocumentScreen.NativeEditor)screen;
        mc.keyboardHandler.setClipboard("paragraph\n".repeat(90));editor.changedSynergy$editor().selectAll();editor.changedSynergy$editor().paste();
        screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_END,0,0);
        check(menu.textCopy.equals("paragraph\n".repeat(90)),"paste is no longer rejected by fixed paper height");
        check(screen.maxScroll()>0&&screen.textScroll()>0,"caret scrolls into view after multiline paste");
        editor.changedSynergy$editor().selectAll();editor.changedSynergy$editor().copy();
        check(mc.keyboardHandler.getClipboard().equals(menu.textCopy),"native selection and clipboard copy retain all text");
        editor.changedSynergy$editor().setCursorToStart();
        int x=(320-screen.getBackgroundWidth())/2+screen.getTextAreaX()+2;
        int y=Math.max(8,(240-screen.getBackgroundHeight()-30)/2)+screen.getTextAreaY()+2;
        screen.mouseClicked(x,y,0);
        check(editor.changedSynergy$editor().getCursorPos()>0,"clicks in scrolled text include its scroll offset");
        int oldScroll=screen.textScroll();check(screen.mouseScrolled(x,y,1)&&screen.textScroll()<oldScroll,"wheel scroll works");
        screen.keyPressed(256,0,0);check(menu.saved==null,"Esc cancels without saving");
        mc.setScreen(screen);screen.init(mc,320,240);
        var done=screen.children().stream().filter(c->c instanceof Button b&&b.visible&&b.getMessage().equals(net.minecraft.network.chat.CommonComponents.GUI_DONE)).map(c->(Button)c).findFirst().orElseThrow();
        done.onPress();check(menu.saved.equals(menu.textCopy),"native Done saves the entire untruncated document");
    }
    private static void capture(Minecraft mc,AdaptiveDocumentScreen<?> screen,String name)throws Exception{
        int w=640,h=360;
        var target=new com.mojang.blaze3d.pipeline.TextureTarget(w*2,h*2,true,Minecraft.ON_OSX);
        var mv=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();mv.pushPose();
        try{
            mv.setIdentity();mv.translate(0,0,-11000);com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,w,h,0,1000,21000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            target.setClearColor(.11F,.13F,.15F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());screen.render(graphics,0,0,1);graphics.flush();
            try(var image=Screenshot.takeScreenshot(target)){image.writeToFile(mc.gameDirectory.toPath().resolve(name+"-adaptive.png"));}
        }finally{mv.popPose();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
    }
}
