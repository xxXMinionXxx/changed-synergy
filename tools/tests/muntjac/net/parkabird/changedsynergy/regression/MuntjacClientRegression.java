package net.parkabird.changedsynergy.regression;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.concurrent.CompletableFuture;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedItems;
import net.ltxprogrammer.changed.item.Syringe;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.ai.HunterFaction;
import net.parkabird.changedsynergy.ai.LightFactionGroup;
import net.parkabird.changedsynergy.client.LatexMuntjacRenderer;
import net.parkabird.changedsynergy.client.archive.ArchiveOriginModels;
import net.parkabird.changedsynergy.entity.LatexMuntjac;
import net.parkabird.changedsynergy.init.*;
import net.parkabird.changedsynergy.origin.NativeOriginCatalog;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Private real-client + integrated-server registration, spawn and render checks. */
@Mod.EventBusSubscriber(modid="changed_synergy", value=Dist.CLIENT)
public final class MuntjacClientRegression {
    private static int stage,checks,wait;
    private static CompletableFuture<Void> task;
    private static int npcId;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || stage==9)return;
        var mc=Minecraft.getInstance();
        try {
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
            if(stage==0 && mc.screen instanceof TitleScreen && mc.getOverlay()==null) {
                check(mc.gameDirectory.getName().startsWith("author-feedback-client-"),"private test world only");
                mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);mc.options.simulationDistance().set(2);mc.options.guiScale().set(2);
                var rules=new GameRules();rules.getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false,null);rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("muntjac",new LevelSettings("muntjac",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(73451,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());stage=1;
            }
            if(mc.level==null || mc.player==null || mc.player.tickCount<40 || mc.getOverlay()!=null)return;
            if(stage==1) {
                task=CompletableFuture.runAsync(()->prepare(mc),mc.getSingleplayerServer());stage=2;
            }
            if(stage==2 && task.isDone() && ++wait>=40) {
                task.join();task=CompletableFuture.runAsync(()->server(mc),mc.getSingleplayerServer());stage=3;wait=0;
            }
            if(stage==3 && task.isDone()) {
                task.join();var form=ProcessTransfur.getPlayerTransfurVariant(mc.player);
                if(form==null || !(form.getChangedEntity() instanceof LatexMuntjac) || !(mc.level.getEntity(npcId) instanceof LatexMuntjac)) {
                    if(++wait>200)throw new AssertionError("muntjac NPC/player synchronization timed out");return;
                }
                mc.setScreen(null);client(mc,(LatexMuntjac)mc.level.getEntity(npcId));
                task=CompletableFuture.runAsync(()->{
                    var owner=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    var female=ChangedSynergyEntities.LATEX_MUNTJAC_FEMALE.get().create(owner.serverLevel());
                    female.moveTo(owner.getX()+2,owner.getY(),owner.getZ());owner.serverLevel().addFreshEntity(female);npcId=female.getId();
                    ProcessTransfur.setPlayerTransfurVariant(owner,ChangedSynergyTransfurVariants.LATEX_MUNTJAC_FEMALE.get(),null,1,false);
                },mc.getSingleplayerServer());stage=4;wait=0;
            }
            if(stage==4&&task.isDone()) {
                task.join();var form=ProcessTransfur.getPlayerTransfurVariant(mc.player);
                if(form==null||!(form.getChangedEntity() instanceof net.parkabird.changedsynergy.entity.LatexMuntjacFemale)
                        ||!(mc.level.getEntity(npcId) instanceof net.parkabird.changedsynergy.entity.LatexMuntjacFemale)) {
                    if(++wait>200)throw new AssertionError("female NPC/player sync timed out");return;
                }
                femaleClient(mc,(net.parkabird.changedsynergy.entity.LatexMuntjacFemale)mc.level.getEntity(npcId));stage=9;
                PartialSkinLayersClientRegression.run(mc);
                System.out.println("MUNTJAC_CLIENT_REGRESSION_PASS: "+checks+" integrated NPC/player, deer attributes, jungle spawn table/predicate, faction, items, origin and real rendering checks");mc.stop();
            }
        } catch(Throwable failure) {stage=9;System.err.println("MUNTJAC_CLIENT_REGRESSION_FAIL");failure.printStackTrace();mc.stop();}
    }
    private static void prepare(Minecraft mc) {
        var level=mc.getSingleplayerServer().overworld();level.setChunkForced(0,0,true);level.setDayTime(18000);
        for(int x=6;x<=10;x++)for(int z=6;z<=10;z++)for(int y=79;y<=83;y++)level.setBlock(new BlockPos(x,y,z),(y==79?Blocks.GRASS_BLOCK:Blocks.AIR).defaultBlockState(),18);
    }
    private static void server(Minecraft mc) {
        var server=mc.getSingleplayerServer();var level=server.overworld();var owner=server.getPlayerList().getPlayer(mc.player.getUUID());
        var type=ChangedSynergyEntities.LATEX_MUNTJAC.get();var npc=type.create(level);var deer=ChangedEntities.LATEX_DEER.get().create(level);
        for(var attribute:java.util.List.of(Attributes.MAX_HEALTH,Attributes.ATTACK_DAMAGE,Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.MOVEMENT_SPEED,net.minecraftforge.common.ForgeMod.SWIM_SPEED.get()))
            check(npc.getAttributeValue(attribute)==deer.getAttributeValue(attribute),"matches deer attribute "+attribute);
        System.out.println("MUNTJAC_ATTRIBUTES hp="+npc.getMaxHealth()+" attack="+npc.getAttributeValue(Attributes.ATTACK_DAMAGE)+" armour="+npc.getAttributeValue(Attributes.ARMOR)+" movement="+npc.getAttributeValue(Attributes.MOVEMENT_SPEED));
        check(npc.getTransfurMode()==deer.getTransfurMode(),"native deer transfur mode");
        var biomes=level.registryAccess().registryOrThrow(Registries.BIOME);
        for(var key:java.util.List.of(Biomes.JUNGLE,Biomes.SPARSE_JUNGLE,Biomes.BAMBOO_JUNGLE))
            for(var sex:java.util.List.of(type,ChangedSynergyEntities.LATEX_MUNTJAC_FEMALE.get()))
                check(biomes.getOrThrow(key).getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(s->s.type==sex&&s.getWeight().asInt()==4&&s.minCount==1&&s.maxCount==2),"male/female natural spawn table preserves total weight eight "+key);
        check(biomes.getOrThrow(Biomes.PLAINS).getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().noneMatch(s->s.type==type),"jungle-only spawning");
        check(SpawnPlacements.getPlacementType(type)==SpawnPlacements.Type.ON_GROUND,"ground spawn placement");
        var pos=new BlockPos(8,80,8);var random=net.minecraft.util.RandomSource.create(736891L);
        boolean spawned=false;for(int i=0;i<256;i++)spawned|=SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,pos,random);
        check(spawned,"natural spawn predicate accepts suitable ground at night");
        npc.moveTo(owner.getX()+2,owner.getY(),owner.getZ());level.addFreshEntity(npc);npcId=npc.getId();
        check(HunterFaction.of(npc)==HunterFaction.LIGHT&&LightFactionGroup.of(npc).equals("jungle"),"explicit jungle allegiance outside jungle habitat");
        var saved=new net.minecraft.nbt.CompoundTag();npc.saveWithoutId(saved);var loaded=type.create(level);loaded.load(saved);
        check(LightFactionGroup.of(loaded).equals("jungle"),"population survives save/load");
        var variant=ChangedSynergyTransfurVariants.LATEX_MUNTJAC.get();
        check(npc.getSelfVariant()==variant,"NPC transfur resolves its own species");
        check(NativeOriginCatalog.allowed(variant),"selectable native origin");var entry=NativeOriginCatalog.find(level.registryAccess(),variant.getFormId());
        check(entry!=null&&entry.faction().equals("light")&&entry.region().equals("jungle"),"origin dossier faction");
        for(var item:java.util.List.of(ChangedItems.LATEX_SYRINGE.get(),ChangedItems.LATEX_FLASK.get(),ChangedItems.LATEX_TIPPED_ARROW.get())) {
            var stack=Syringe.setPureVariant(new ItemStack(item),variant.getFormId());
            check(Syringe.getVariant(stack)==variant,"transfur item retains muntjac species");
        }
        ProcessTransfur.setPlayerTransfurVariant(owner,variant,null,1,false);
        check(ProcessTransfur.getPlayerTransfurVariant(owner).getChangedEntity() instanceof LatexMuntjac,"server player transformation");
        infuser(owner);
    }
    private static void infuser(net.minecraft.server.level.ServerPlayer owner) {
        var menu=new net.ltxprogrammer.changed.world.inventory.InfuserMenu(155,owner.getInventory(),null);
        var ingredients=java.util.List.of(ChangedItems.LATEX_BASE.get(),ChangedItems.WHITE_LATEX_GOO.get(),Items.JUNGLE_LEAVES,Items.BAMBOO,Items.LEATHER);
        for(int i=0;i<ingredients.size();i++)menu.get().get(i+1).set(new ItemStack(ingredients.get(i)));
        for(var base:net.ltxprogrammer.changed.recipe.InfuserRecipe.getAllowedInputs()) {
            menu.get().get(10).set(base.copy());
            for(boolean female:new boolean[]{false,true,false}) {
                menu.guiState.put("changed:male_female_switch",female);menu.slotsChanged(null);menu.stillValid(owner);
                var result=menu.getResultSlot().getItem();var expected=female?ChangedSynergyTransfurVariants.LATEX_MUNTJAC_FEMALE.get():ChangedSynergyTransfurVariants.LATEX_MUNTJAC.get();
                check(!result.isEmpty()&&Syringe.getVariant(result)==expected,"native synthesizer gender toggle produces the correct "+base);
                check(!result.getOrCreateTag().getBoolean("safe"),"synthesizer preserves native unsafe output flag");
            }
        }
        for(var form:java.util.List.of(ChangedSynergyTransfurVariants.LATEX_MUNTJAC.get(),ChangedSynergyTransfurVariants.LATEX_MUNTJAC_FEMALE.get())) {
            var instance=ProcessTransfur.setPlayerTransfurVariant(owner,form,null,1,false);
            check(instance.getAbilityInstance(net.ltxprogrammer.changed.init.ChangedAbilities.SWITCH_GENDER.get())==null,"muntjac has no white wolf gender-switch ability");
        }
        ProcessTransfur.setPlayerTransfurVariant(owner,ChangedSynergyTransfurVariants.LATEX_MUNTJAC.get(),null,1,false);
        var female=ChangedSynergyEntities.LATEX_MUNTJAC_FEMALE.get().create(owner.serverLevel());
        check(female.getGender()==net.ltxprogrammer.changed.entity.Gender.FEMALE&&LightFactionGroup.of(female).equals("jungle"),"female uses native sex and jungle allegiance");
        check(SpawnPlacements.getPlacementType(female.getType())==SpawnPlacements.Type.ON_GROUND,"female ground spawn placement");
        check(female.getSelfVariant()==ChangedSynergyTransfurVariants.LATEX_MUNTJAC_FEMALE.get(),"female species resolves independently");
    }
    private static void client(Minecraft mc,LatexMuntjac npc)throws Exception {
        var renderer=(LatexMuntjacRenderer)mc.getEntityRenderDispatcher().getRenderer(npc);var model=renderer.getModel();
        check(mc.getResourceManager().getResource(LatexMuntjacRenderer.TEXTURE).isPresent(),"texture resolves");
        check(model.getHead().hasChild("LeftAntler")&&model.getHead().getChild("LeftEar").hasChild("LeftEarPivot"),"authored ear and antler rig baked");
        for(int i=0;i<120;i++){npc.tickCount++;npc.yHeadRot=i%2==0?30:-30;model.prepareMobModel(npc,i*.1F,.4F,.5F);model.setupAnim(npc,i*.1F,.4F,npc.tickCount+.5F,30,10);
            check(Float.isFinite(model.getHead().getChild("LeftEar").xRot),"finite per-entity animation");}
        net.parkabird.changedsynergy.client.model.MuntjacEarChecks.main(new String[]{mc.gameDirectory.toPath().resolve("muntjac-ear-checks.json").toString()});
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(),true,mc.level.registryAccess());
        var ordered=new java.util.ArrayList<>(net.ltxprogrammer.changed.init.ChangedTabs.TAB_CHANGED_ITEMS.get().getDisplayItems());
        for(var item:java.util.List.of(ChangedItems.LATEX_SYRINGE.get(),ChangedItems.LATEX_FLASK.get(),ChangedItems.LATEX_TIPPED_ARROW.get())) {
            int found=-1,last=-1;
            for(int i=0;i<ordered.size();i++)if(ordered.get(i).is(item)){
                last=i;if(Syringe.getVariant(ordered.get(i))==ChangedSynergyTransfurVariants.LATEX_MUNTJAC.get())found=i;
            }
            check(found>0&&ordered.get(found-1).is(item)&&found+1<=last
                    &&Syringe.getVariant(ordered.get(found+1))==ChangedSynergyTransfurVariants.LATEX_MUNTJAC_FEMALE.get(),"male/female items stay together in their native creative group");
            var stack=ordered.get(found);
            check((mc.getItemColors().getColor(stack,0)&0xFFFFFF)==0xB78A5D
                    &&(mc.getItemColors().getColor(stack,1)&0xFFFFFF)==0xE6D4B4,"own brown/cream item tint");
        }
        for(var item:java.util.List.of(ChangedItems.LATEX_SYRINGE.get(),ChangedItems.LATEX_FLASK.get(),ChangedItems.LATEX_TIPPED_ARROW.get(),ChangedSynergyItems.LATEX_MUNTJAC_SPAWN_EGG.get())) {
            var stack=item==ChangedSynergyItems.LATEX_MUNTJAC_SPAWN_EGG.get()?new ItemStack(item):Syringe.setPureVariant(new ItemStack(item),ChangedSynergyTransfurVariants.LATEX_MUNTJAC.getId());
            check(!stack.getHoverName().getString().contains("entity.changed_synergy"),"localized item name");
            check(mc.getItemRenderer().getModel(stack,mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),"item model baked");
        }
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        var target=new TextureTarget(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true,Minecraft.ON_OSX);var mv=RenderSystem.getModelViewStack();mv.pushPose();
        try {
            mv.setIdentity();mv.translate(0,0,-11000);RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,w,h,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            target.setClearColor(.1F,.12F,.15F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            for(int row=0;row<2;row++)for(int col=0;col<3;col++){
                var subject=row==0?npc:mc.player;float yaw=new float[]{180,270,0}[col];subject.yBodyRot=subject.yBodyRotO=subject.yHeadRot=subject.yHeadRotO=yaw;subject.setYRot(yaw);
                int x=(col*2+1)*w/6,y=(row+1)*(h-60)/2-5;
                InventoryScreen.renderEntityInInventory(graphics,x,y,h/9,new Quaternionf().rotationZ((float)Math.PI),new Quaternionf(),subject);
                graphics.drawCenteredString(mc.font,(row==0?"NPC ":"Player ")+new String[]{"front","side","back"}[col],x,y+8,0xFFFFFF);
            }
            int x=w/2-48;for(var item:java.util.List.of(ChangedItems.LATEX_SYRINGE.get(),ChangedItems.LATEX_FLASK.get(),ChangedItems.LATEX_TIPPED_ARROW.get())){graphics.renderItem(Syringe.setPureVariant(new ItemStack(item),ChangedSynergyTransfurVariants.LATEX_MUNTJAC.getId()),x,h-28);x+=32;}
            graphics.flush();capture(mc,target,"muntjac-gallery.png");
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);var archive=new ArchiveOriginModels();
            archive.renderPortrait(graphics,NativeOriginCatalog.find(mc.level.registryAccess(),ChangedSynergyTransfurVariants.LATEX_MUNTJAC.getId()),w/2-120,h/2-115,240,230);
            graphics.flush();capture(mc,target,"muntjac-portrait.png");
            npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));
            npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);InventoryScreen.renderEntityInInventory(graphics,w/2,h-30,h/4,new Quaternionf().rotationZ((float)Math.PI).rotateY(.35F),new Quaternionf(),npc);
            graphics.flush();capture(mc,target,"muntjac-armour.png");
        } finally {mv.popPose();RenderSystem.applyModelViewMatrix();target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
    }
    private static void femaleClient(Minecraft mc,net.parkabird.changedsynergy.entity.LatexMuntjacFemale npc)throws Exception {
        var renderer=(net.parkabird.changedsynergy.client.LatexMuntjacFemaleRenderer)mc.getEntityRenderDispatcher().getRenderer(npc);
        check(mc.getResourceManager().getResource(renderer.TEXTURE).isPresent(),"female texture resolves");
        check(!renderer.getModel().getHead().hasChild("LeftAntler"),"female authored head has no male antlers");
        for(int i=0;i<80;i++){npc.tickCount++;renderer.getModel().prepareMobModel(npc,i*.1F,.4F,.5F);renderer.getModel().setupAnim(npc,i*.1F,.4F,npc.tickCount+.5F,30,10);}
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        var target=new TextureTarget(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true,Minecraft.ON_OSX);var mv=RenderSystem.getModelViewStack();mv.pushPose();
        try {
            mv.setIdentity();mv.translate(0,0,-11000);RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,w,h,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            target.setClearColor(.1F,.12F,.15F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);var g=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            for(int row=0;row<2;row++)for(int col=0;col<3;col++){
                var subject=row==0?npc:mc.player;float yaw=new float[]{180,270,0}[col];subject.yBodyRot=subject.yBodyRotO=subject.yHeadRot=subject.yHeadRotO=yaw;subject.setYRot(yaw);
                int x=(col*2+1)*w/6,y=(row+1)*(h-60)/2-5;
                InventoryScreen.renderEntityInInventory(g,x,y,h/9,new Quaternionf().rotationZ((float)Math.PI),new Quaternionf(),subject);
                g.drawCenteredString(mc.font,(row==0?"NPC ":"Player ")+new String[]{"front","side","back"}[col],x,y+8,0xFFFFFF);
            }
            g.flush();capture(mc,target,"muntjac-female-gallery.png");
            npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);InventoryScreen.renderEntityInInventory(g,w/2,h-30,h/4,new Quaternionf().rotationZ((float)Math.PI).rotateY(.35F),new Quaternionf(),npc);
            g.flush();capture(mc,target,"muntjac-female-armour.png");
        }finally{mv.popPose();RenderSystem.applyModelViewMatrix();target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
    }
    private static void capture(Minecraft mc,TextureTarget target,String name)throws Exception {try(var img=net.minecraft.client.Screenshot.takeScreenshot(target)){img.writeToFile(mc.gameDirectory.toPath().resolve(name));}}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
}
