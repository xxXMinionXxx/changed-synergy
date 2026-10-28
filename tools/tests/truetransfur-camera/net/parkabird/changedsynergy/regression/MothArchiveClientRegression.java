package net.parkabird.changedsynergy.regression;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.parkabird.changedsynergy.client.NativeOriginScreen;
import net.parkabird.changedsynergy.init.ChangedSynergyTransfurVariants;
import net.parkabird.changedsynergy.network.NativeOriginStatePacket;
import net.parkabird.changedsynergy.origin.NativeOriginCatalog.Entry;

final class MothArchiveClientRegression {
    static void run(Minecraft mc, Path directory) throws Exception {
        int oldScale = mc.options.guiScale().get();
        var projection = new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        try {
            for (int scale : new int[]{2, 3}) {
                mc.options.guiScale().set(scale); mc.resizeDisplay();
                int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
                RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,w,h,0,1000,3000),
                        com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
                var entry = new Entry(ChangedSynergyTransfurVariants.LATEX_SILK_MOTH.getId(), "light", "forest");
                var screen = new NativeOriginScreen(new NativeOriginStatePacket(0, UUID.randomUUID(), true, true,
                        Component.empty(), List.of(entry)));
                mc.setScreen(screen);
                Class<?> page = Class.forName(NativeOriginScreen.class.getName() + "$Page");
                var constructor = page.getDeclaredConstructor(int.class, int.class, Entry.class); constructor.setAccessible(true);
                Object dossier = constructor.newInstance(3,0,entry), roster = constructor.newInstance(3,0,null);
                set(screen,"view", enumValue("View","FILE")); set(screen,"motion",null);
                set(screen,"pose",field(NativeOriginScreen.class,"FOCUS").get(null));
                set(screen,"open",1.0); set(screen,"welcomeAlpha",0.0); set(screen,"desktopAlpha",0.0);
                set(screen,"paper",roster);
                var turn = NativeOriginScreen.class.getDeclaredMethod("changePage",page,int.class,double.class); turn.setAccessible(true);
                var target = new TextureTarget(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true,Minecraft.ON_OSX);
                try {
                    capture(mc,screen,target,directory,"moth-origin-roster-scale-"+scale).close();
                    turn.invoke(screen,dossier,1,.64);
                    progress(screen,.72); capture(mc,screen,target,directory,"moth-origin-notes-turn-in-scale-"+scale).close();
                    progress(screen,1.05);
                    try (var picture = capture(mc,screen,target,directory,"moth-origin-notes-scale-"+scale)) {
                        for (int color : new int[]{0x1F2529,0x08639C,0x2C7572,0x944549,0x9A7835})
                            if (count(picture,color,picture.getWidth()/2)<8) throw new AssertionError("Missing moth note color: "+Integer.toHexString(color));
                    }
                    turn.invoke(screen,roster,-1,.60);
                    progress(screen,.28); capture(mc,screen,target,directory,"moth-origin-notes-turn-out-scale-"+scale).close();
                    progress(screen,1.05);
                    try (var picture = capture(mc,screen,target,directory,"moth-origin-notes-returned-scale-"+scale)) {
                        if (count(picture,0x08639C,picture.getWidth()/2)>0) throw new AssertionError("Moth note leaked onto the register");
                    }
                    set(screen,"paper",dossier); set(screen,"open",0.0);
                    try (var picture = capture(mc,screen,target,directory,"moth-origin-notes-cover-scale-"+scale)) {
                        if (count(picture,0x08639C,picture.getWidth())>0) throw new AssertionError("Moth note leaked through the closed cover");
                    }
                } finally { target.destroyBuffers(); screen.removed(); mc.setScreen(null); }
            }
            System.out.println("MOTH_ARCHIVE_CLIENT_REGRESSION_PASS: five exact ink colors, both printed page-turn directions, register/cover occlusion and GUI scales 2/3");
        } finally {
            mc.options.guiScale().set(oldScale); mc.resizeDisplay(); mc.getMainRenderTarget().bindWrite(true);
            RenderSystem.setProjectionMatrix(projection,sorting);
        }
    }
    private static com.mojang.blaze3d.platform.NativeImage capture(Minecraft mc, NativeOriginScreen screen,
            TextureTarget target, Path directory, String name) throws Exception {
        target.setClearColor(0,0,0,1); target.clear(Minecraft.ON_OSX); target.bindWrite(true);
        var graphics = new GuiGraphics(mc,mc.renderBuffers().bufferSource()); screen.render(graphics,-100,-100,0); graphics.flush();
        var picture = net.minecraft.client.Screenshot.takeScreenshot(target); picture.writeToFile(directory.resolve(name+".png")); return picture;
    }
    private static int count(com.mojang.blaze3d.platform.NativeImage picture,int color,int right) {
        int total=0;
        for(int y=0;y<picture.getHeight();y++) for(int x=0;x<right;x++) {
            int pixel=picture.getPixelRGBA(x,y);
            if(Math.abs((pixel&255)-(color>>16&255))<=2 && Math.abs((pixel>>8&255)-(color>>8&255))<=2
                    && Math.abs((pixel>>16&255)-(color&255))<=2) total++;
        }
        return total;
    }
    private static void progress(NativeOriginScreen screen,double progress) throws Exception {
        Object turn=field(NativeOriginScreen.class,"turn").get(screen);
        double duration=(double)field(turn.getClass(),"duration").get(turn);
        field(turn.getClass(),"begun").setLong(turn,Util.getMillis()-(long)(120+duration*1000*progress));
    }
    private static Object enumValue(String name,String value) throws Exception {
        Class<?> type=Class.forName(NativeOriginScreen.class.getName()+"$"+name);
        return java.util.Arrays.stream(type.getEnumConstants()).filter(v->v.toString().equals(value)).findFirst().orElseThrow();
    }
    private static void set(Object target,String name,Object value) throws Exception {field(target.getClass(),name).set(target,value);}
    private static Field field(Class<?> type,String name) throws Exception {var field=type.getDeclaredField(name);field.setAccessible(true);return field;}
}
