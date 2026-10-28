package net.parkabird.changedsynergy.regression;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.init.*;
import net.parkabird.changedsynergy.client.LatexSwordfishRenderer;
import net.parkabird.changedsynergy.client.model.LatexSwordfishModel;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Mod.EventBusSubscriber(modid="changed_synergy",value=Dist.CLIENT)
public final class SwordfishClientRegression {
    private static boolean loading,finished;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("swordfish.client.regression")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try {
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
            if(!loading&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
                loading=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);mc.options.simulationDistance().set(2);mc.options.guiScale().set(2);
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                var rules=new GameRules();rules.getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false,null);rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                var settings=new LevelSettings("swordfish",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("swordfish",settings,new WorldOptions(73451,false,false),
                    r->r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());
            }
            if(mc.level==null||mc.player==null||mc.player.tickCount<40||mc.getOverlay()!=null)return;
            mc.setScreen(null);run(mc);finished=true;
            System.out.println("SWORDFISH_CLIENT_REGRESSION_PASS: native geometry, 300 blended swim/spin recoveries, archive portrait, hidden catch HUD, transfur items and grayscale ability icon");mc.stop();
        }catch(Throwable failure){finished=true;System.err.println("SWORDFISH_CLIENT_REGRESSION_FAIL");failure.printStackTrace();mc.stop();}
    }
    private static void run(Minecraft mc)throws Exception {
        var fish=ChangedSynergyEntities.LATEX_SWORDFISH.get().create(mc.level);
        var renderer=(LatexSwordfishRenderer)mc.getEntityRenderDispatcher().getRenderer(fish);
        var model=renderer.getModel();
        check(mc.getResourceManager().getResource(LatexSwordfishRenderer.TEXTURE).isPresent(),"texture loaded");
        check(model.getHead().getAllParts().count()>=10,"head, fins and sword geometry baked");
        var rush=fish.getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());check(rush!=null,"NPC ability created");
        var sound=mc.getSoundManager().getSoundEvent(ChangedSynergySoundEvents.SWORDFISH_RUSH.getId());
        check(sound!=null&&sound.getSound(net.minecraft.util.RandomSource.create()).getLocation().equals(
                net.minecraft.resources.ResourceLocation.parse("changed_synergy:ability/swordfish_rush")),"rush resolves its own trimmed sound, not the vanilla throwing intro");
        CompoundTag tag=new CompoundTag();tag.putInt("RushTicks",20);tag.putDouble("Z",1);
        for(int i=0;i<300;i++){
            rush.onRemove();rush.acceptPayload(tag);
            check(rush.visualAmount(1)==0,"starting payload does not snap straight into the rush pose");
            for(int tick=0;tick<3;tick++)rush.tickIdle();
            check(Math.abs(rush.visualAmount(1)-1)<.00001&&rush.visualAmount(.5F)<1,"three-tick entry interpolates between ticks");
            check(rush.spin(1)!=0,"Riptide body spin advances");
            model.prepareMobModel(fish,0,0,1);model.setupAnim(fish,0,0,i,45,25);
            check(Math.abs(model.getHead().xRot+(float)Math.PI/2)<.00001&&model.getHead().yRot==0,"bill aligned with spine during charge");
            tag.putInt("RushTicks",0);rush.acceptPayload(tag);
            check(rush.visualAmount(1)>.99,"stop payload retains the current pose for recovery");
            for(int tick=0;tick<2;tick++)rush.tickIdle();
            check(rush.visualAmount(1)==0&&Math.abs(net.minecraft.util.Mth.wrapDegrees(rush.spin(1)))<.0001,"recovery restores swimming and neutral rotation");
            model.prepareMobModel(fish,0,0,1);model.setupAnim(fish,0,0,i,0,0);
            check(Math.abs(model.getHead().xRot)<1.0,"head recovers after charge");tag.putInt("RushTicks",20);
        }
        rush.onRemove();rush.acceptPayload(tag);
        for(int tick=0;tick<22;tick++)rush.tickIdle();
        check(!rush.isDashing()&&rush.visualAmount(1)==0&&Math.abs(net.minecraft.util.Mth.wrapDegrees(rush.spin(1)))<.0001,
                "client also clears pose and spin after local expiry without a stop payload");
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        var target=new TextureTarget(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true,Minecraft.ON_OSX);
        var stack=RenderSystem.getModelViewStack();stack.pushPose();
        try{
            stack.setIdentity();stack.translate(0,0,-11000);RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,w,h,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            for(int armor=0;armor<2;armor++) {
            fish.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,armor==0?ItemStack.EMPTY:new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
            fish.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,armor==0?ItemStack.EMPTY:new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
            target.setClearColor(.10F,.12F,.15F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            for(int row=0;row<2;row++)for(int col=0;col<3;col++){
                rush.onRemove();tag.putInt("RushTicks",row==0?0:20);tag.putDouble("X",0);tag.putDouble("Z",1);rush.acceptPayload(tag);
                if(row>0)for(int tick=0;tick<4;tick++)rush.tickIdle();
                float yaw=new float[]{180,270,0}[col];fish.yBodyRot=fish.yBodyRotO=fish.yHeadRot=fish.yHeadRotO=yaw;fish.setYRot(yaw);
                int x=(col*2+1)*w/6,y=(row+1)*(h-60)/2-10;
                // Rotate the view for dash side/rear samples; rush yaw remains the authoritative travel direction.
                Quaternionf view=new Quaternionf().rotationZ((float)Math.PI).rotateY(row==0?0:(float)Math.toRadians(col*90));
                InventoryScreen.renderEntityInInventory(graphics,x,y,h/9,view,new Quaternionf(),fish);
                graphics.drawCenteredString(mc.font,(row==0?"Idle ":"Rush ")+new String[]{"front","side","back"}[col],x,y+8,0xFFFFFF);
            }
            int x=w/2-44;
            for(var item:java.util.List.of(net.ltxprogrammer.changed.init.ChangedItems.LATEX_SYRINGE.get(),net.ltxprogrammer.changed.init.ChangedItems.LATEX_FLASK.get(),net.ltxprogrammer.changed.init.ChangedItems.LATEX_TIPPED_ARROW.get())){
                var itemStack=net.ltxprogrammer.changed.item.Syringe.setPureVariant(new ItemStack(item),ChangedSynergyTransfurVariants.LATEX_SWORDFISH.getId());
                graphics.renderItem(itemStack,x,h-32);x+=28;
                check(!mc.getItemRenderer().getModel(itemStack,mc.level,mc.player,0).isCustomRenderer(),"transfur item model available");
            }
            net.ltxprogrammer.changed.client.ChangedClient.abilityRenderer.getOrThrow().renderGuiAbility(graphics,rush,x+8,h-32,24);
            graphics.flush();
            try(var image=net.minecraft.client.Screenshot.takeScreenshot(target)){image.writeToFile(mc.gameDirectory.toPath().resolve(armor==0?"swordfish-gallery.png":"swordfish-armour-gallery.png"));}
            }
            rush.onRemove();
            var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            var archive=new net.parkabird.changedsynergy.client.archive.ArchiveOriginModels();
            archive.renderPortrait(graphics,new net.parkabird.changedsynergy.origin.NativeOriginCatalog.Entry(
                    ChangedSynergyTransfurVariants.LATEX_SWORDFISH.getId(),"aquatic","open_ocean"),w/2-107,h/2-96,214,192);
            graphics.flush();
            try(var image=net.minecraft.client.Screenshot.takeScreenshot(target)){image.writeToFile(mc.gameDirectory.toPath().resolve("swordfish-portrait.png"));}
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            net.parkabird.changedsynergy.client.ChasePlayClient.receive(new net.parkabird.changedsynergy.network.ChasePlayStatePacket(
                    1,net.parkabird.changedsynergy.network.ChasePlayStatePacket.HUG,40,false,fish.getId()));
            var overlay=net.parkabird.changedsynergy.client.ChasePlayOverlay.class.getDeclaredMethod("render",GuiGraphics.class,int.class,int.class);overlay.setAccessible(true);
            overlay.invoke(null,graphics,w,h);graphics.flush();
            try(var image=net.minecraft.client.Screenshot.takeScreenshot(target)){
                int base=image.getPixelRGBA(0,0);
                for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)
                    check(image.getPixelRGBA(x,y)==base,"caught-runner hug has no top chase HUD text");
            }
            net.parkabird.changedsynergy.client.ChasePlayClient.receive(new net.parkabird.changedsynergy.network.ChasePlayStatePacket(
                    1,net.parkabird.changedsynergy.network.ChasePlayStatePacket.CLEAR,0,false,fish.getId()));
        }finally{stack.popPose();RenderSystem.applyModelViewMatrix();target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
        check(mc.getModelManager().getModel(new net.minecraft.client.resources.model.ModelResourceLocation(
                net.minecraft.resources.ResourceLocation.parse("changed_synergy:swordfish_rush"),"ability"))!=mc.getModelManager().getMissingModel(),"ability icon baked");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
