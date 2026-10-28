package net.parkabird.changedsynergy.regression;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.parkabird.changedsynergy.client.ChasePlayClient;
import net.parkabird.changedsynergy.client.ChasePlayOverlay;
import net.parkabird.changedsynergy.network.ChasePlayStatePacket;
import static net.parkabird.changedsynergy.network.ChasePlayStatePacket.*;

/** Actual native HUD rendering at compact/large GUI widths, without a web mockup. */
final class ChasePlayHudRegression {
    static void run(Minecraft mc, ChasePlayStatePacket original) throws Exception {
        var countdown=ChasePlayClient.class.getDeclaredMethod("countdownTicks");countdown.setAccessible(true);
        var fill=ChasePlayOverlay.class.getDeclaredMethod("distanceProgress",double.class);fill.setAccessible(true);
        ChasePlayClient.receive(new ChasePlayStatePacket(original.sessionId(),HEAD_START,60,false,original.partnerId()));
        check((int)countdown.invoke(null)==60,"Runner sees the chaser's three-second countdown");
        var hint=ChasePlayClient.class.getDeclaredMethod("hint");hint.setAccessible(true);
        check(((net.minecraft.network.chat.Component)hint.invoke(null)).getString().contains("Run now"),"Runner is told to run immediately");
        ChasePlayClient.receive(new ChasePlayStatePacket(original.sessionId(),HEAD_START,60,true,original.partnerId()));
        check((int)countdown.invoke(null)==60,"Both participants see the same three-second countdown");
        ChasePlayClient.receive(new ChasePlayStatePacket(original.sessionId(),CHASING,300,true,original.partnerId()));
        check((int)countdown.invoke(null)==0,"No extra delay follows the chaser countdown");
        check((float)fill.invoke(null,1D)==1F && (float)fill.invoke(null,16D)==0F,"Distance bar has bounded close/far endpoints");
        check((float)fill.invoke(null,4D)>(float)fill.invoke(null,10D),"Closing distance fills rather than drains the bar");
        check((float)fill.invoke(null,Double.NaN)==0F,"Invalid positions never produce an invalid draw width");
        for(String key:new String[]{"title","chaser","runner","go","time","chaser_starts","distance"})
            check(I18n.exists("hud.changed_synergy.chase."+key),"HUD key is translated: "+key);
        var directory=mc.gameDirectory.toPath().resolve("screenshots");java.nio.file.Files.createDirectories(directory);
        var render=ChasePlayOverlay.class.getDeclaredMethod("render",GuiGraphics.class,int.class,int.class);render.setAccessible(true);
        var age=ChasePlayClient.class.getDeclaredField("phaseAge");age.setAccessible(true);
        var projection=new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();
        var model=RenderSystem.getModelViewStack();model.pushPose();model.setIdentity();model.translate(0,0,-2000);RenderSystem.applyModelViewMatrix();
        int[] states={INVITED,READY,HEAD_START,CHASING,HUG,COMFORT,FALSE_START};
        try {
            for(int width:new int[]{320,375,414,768}) {
                int height=270;
                RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,width,height,0,1000,3000),VertexSorting.ORTHOGRAPHIC_Z);
                var target=new TextureTarget(width*2,height*2,true,Minecraft.ON_OSX);
                try {
                    for(int stage:states) for(boolean chases:new boolean[]{false,true}) {
                        target.setClearColor(.18F,.18F,.18F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
                        ChasePlayClient.receive(new ChasePlayStatePacket(original.sessionId(),stage,stage==HEAD_START?60:160,chases,original.partnerId()));
                        age.setInt(null,20);
                        var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());render.invoke(null,graphics,width,height);graphics.flush();
                        try(var image=net.minecraft.client.Screenshot.takeScreenshot(target)) {
                            image.writeToFile(directory.resolve("chase-hud-"+stage+"-"+(chases?"chaser":"runner")+"-"+width+".png"));
                        }
                        if(stage==CHASING) {
                            age.setInt(null,0);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
                            render.invoke(null,graphics,width,height);graphics.flush();
                            try(var image=net.minecraft.client.Screenshot.takeScreenshot(target)) {
                                image.writeToFile(directory.resolve("chase-hud-go-"+(chases?"chaser":"runner")+"-"+width+".png"));
                            }
                        }
                    }
                } finally {target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
            }
            System.out.println("CHASE_PLAY_HUD_REGRESSION_PASS: 14 timing/distance/localization checks; 64 native state screenshots at 320/375/414/768 GUI pixels");
        } finally {
            ChasePlayClient.receive(original);model.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);
        }
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
