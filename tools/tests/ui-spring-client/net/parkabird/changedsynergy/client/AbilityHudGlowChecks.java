package net.parkabird.changedsynergy.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.ltxprogrammer.changed.ability.*;
import net.ltxprogrammer.changed.client.gui.AbilityOverlay;
import net.ltxprogrammer.changed.client.gui.AbstractRadialScreen;
import net.ltxprogrammer.changed.entity.variant.TransfurVariantInstance;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.util.Color3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import org.joml.Matrix4f;

/** Real renderer comparisons; probe controllers never activate an ability or send a gameplay packet. */
final class AbilityHudGlowChecks {
    private static int checks;
    static void run(Minecraft mc)throws Exception {
        boolean reduced=net.parkabird.changedsynergy.ChangedSynergyClientConfig.CLIENT.reducedUiMotion.get();
        var target=new TextureTarget(64,64,true,Minecraft.ON_OSX);var view=RenderSystem.getModelViewStack();view.pushPose();
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());
        var variant=ProcessTransfur.getPlayerTransfurVariant(mc.player);
        var originalPrimary=variant.selectedAbility;
        var scheme=new AbstractRadialScreen.ColorScheme(Color3.fromInt(0x70513C),Color3.fromInt(0xE8D8B0));
        try {
            view.setIdentity();view.translate(0,0,-11000);RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,64,64,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            reduced(true);field(AbilityHudMotion.class,"level").set(null,new Object());AbilityHudMotion.beginFrame();
            java.lang.reflect.Field second=null;Object previousSecond=null,secondSetting=null,previousSecondSetting=null;
            if(net.minecraftforge.fml.ModList.get().isLoaded("changed_addon")) {
                var api=Class.forName("net.foxyas.changedaddon.variant.TransfurVariantInstanceExtensor");
                secondSetting=Class.forName("net.foxyas.changedaddon.configuration.ChangedAddonServerConfiguration").getField("ALLOW_SECOND_ABILITY_USE").get(null);
                previousSecondSetting=secondSetting.getClass().getMethod("get").invoke(secondSetting);
                secondSetting.getClass().getMethod("set",Object.class).invoke(secondSetting,true);
                second=field(variant,"secondSelectedAbility");previousSecond=second.get(variant);
                api.getMethod("setSecondSelectedAbility",AbstractAbility.class).invoke(variant,variant.selectedAbility);
            }
            try {
                target.setClearColor(.1F,.15F,.2F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
                AbilityOverlay.renderSelectedAbility(mc.gui,new GuiGraphics(mc,mc.renderBuffers().bufferSource()),0,64,64);
                var routed=(AbstractAbilityInstance[])field(AbilityHudMotion.class,"HIGHLIGHT_FOR").get(null);
                check(routed[0]==variant.getSelectedAbility(),"actual native HUD routes its activation layer through the new renderer");
                if(second!=null)check(routed[1]==variant.getSelectedAbility(),"actual merged Addon secondary HUD is routed independently");
                else check(routed[1]==null,"native HUD works with Addon absent");
            } finally {
                if(second!=null)second.set(variant,previousSecond);
                if(secondSetting!=null)secondSetting.getClass().getMethod("set",Object.class).invoke(secondSetting,previousSecondSetting);
            }
            reduced(false);
            var ability=new Probe();var selected=ability.makeInstance(IAbstractChangedEntity.forPlayer(mc.player));
            var controller=selected.getController();var springs=(UiSpring[])field(AbilityHudMotion.class,"HIGHLIGHT").get(null);
            int[] idle=draw(mc,target,scheme,variant,selected,false,true);
            check(java.util.Arrays.equals(idle,draw(mc,target,scheme,variant,selected,false,false)),"untriggered HUD retains every native pixel");
            field(controller,"coolDownTicksRemaining").setInt(controller,10);
            check(java.util.Arrays.equals(draw(mc,target,scheme,variant,selected,false,true),draw(mc,target,scheme,variant,selected,false,false)),"cooldown fill remains identical to native rendering");
            field(controller,"coolDownTicksRemaining").setInt(controller,0);field(controller,"keyStateO").setBoolean(controller,true);
            var before=save(controller);
            int[] onset=draw(mc,target,scheme,variant,selected,false,false);
            check(java.util.Arrays.equals(onset,idle),"activation starts from the original dim HUD instead of flashing bright");
            age(springs[0],60_000_000L);int[] middle=draw(mc,target,scheme,variant,selected,false,false);
            int[] active=draw(mc,target,scheme,variant,selected,false,true);int pixel=32*64+32;
            check((middle[pixel]&255)>(idle[pixel]&255)+3&&(middle[pixel]&255)<(active[pixel]&255)-3,"activation brightness has a visible intermediate frame");
            check(save(controller).equals(before)&&field(controller,"keyStateO").getBoolean(controller),"visual easing cannot change charge/cooldown or input state");
            field(controller,"keyStateO").setBoolean(controller,false);
            int[] tail=draw(mc,target,scheme,variant,selected,false,false);
            check((tail[pixel]&255)>(idle[pixel]&255)+3,"activation glow fades out after release rather than disappearing immediately");
            age(springs[0],600_000_000L);
            check(java.util.Arrays.equals(idle,draw(mc,target,scheme,variant,selected,false,false)),"fade-out restores the exact native untriggered HUD");
            ability.charging=true;field(controller,"keyStateO").setBoolean(controller,true);field(controller,"chargeTicks").setInt(controller,10);reduced(true);
            check(java.util.Arrays.equals(draw(mc,target,scheme,variant,selected,false,true),draw(mc,target,scheme,variant,selected,false,false)),"reduced motion preserves exact native half-charge geometry without easing its value");
            check(field(controller,"chargeTicks").getInt(controller)==10,"charge progress remains authoritative");
            ability.charging=false;draw(mc,target,scheme,variant,selected,false,false);
            var other=ability.makeInstance(IAbstractChangedEntity.forPlayer(mc.player));draw(mc,target,scheme,variant,other,true,false);
            check(springs[0].value(System.nanoTime())==1&&springs[1].value(System.nanoTime())==0,"secondary idle HUD cannot consume the primary glow");
            check(java.util.Arrays.equals(idle,draw(mc,target,scheme,variant,other,false,false)),"changing selected abilities cannot carry the old glow onto the new icon");
            reduced(false);field(controller,"keyStateO").setBoolean(controller,false);springs[0].snap(1,System.nanoTime());
            ((AbstractAbilityInstance[])field(AbilityHudMotion.class,"HIGHLIGHT_FOR").get(null))[0]=selected;
            ((long[])field(AbilityHudMotion.class,"LAST_DRAWN_AT").get(null))[0]=System.nanoTime()-2_000_000_000L;
            check(java.util.Arrays.equals(idle,draw(mc,target,scheme,variant,selected,false,false)),"an inactive HUD returning after being hidden has no stale glow");
            field(AbilityHudMotion.class,"level").set(null,new Object());AbilityHudMotion.beginFrame();
            for(int i=0;i<2;i++)check(springs[i].value(System.nanoTime())==0&&((AbstractAbilityInstance[])field(AbilityHudMotion.class,"HIGHLIGHT_FOR").get(null))[i]==null,"world/form context changes clear activation visuals");
            System.out.println("ABILITY_HUD_GLOW_PASS: "+checks+" native pixels, activation/release fades, independent secondary HUD, exact cooldown/charge, state cleanup and live merged renderer checks");
        } finally {
            variant.selectedAbility=originalPrimary;
            reduced(reduced);view.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,VertexSorting.ORTHOGRAPHIC_Z);RenderSystem.setShaderColor(1,1,1,1);target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);
        }
    }
    private static int[] draw(Minecraft mc,TextureTarget target,AbstractRadialScreen.ColorScheme scheme,TransfurVariantInstance<?> variant,AbstractAbilityInstance selected,boolean secondary,boolean nativeRenderer)throws Exception {
        target.setClearColor(.1F,.15F,.2F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);var g=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
        if(nativeRenderer)AbilityOverlay.renderBackground(16,16,g,scheme,mc.player,variant,selected);
        else AbilityHudMotion.renderBackground(16,16,g,scheme,mc.player,variant,selected,secondary);
        g.flush();try(var image=Screenshot.takeScreenshot(target)){int[] pixels=new int[4096];for(int y=0;y<64;y++)for(int x=0;x<64;x++)pixels[y*64+x]=image.getPixelRGBA(x,y);return pixels;}
    }
    private static CompoundTag save(AbstractAbility.Controller controller){var tag=new CompoundTag();controller.saveData(tag);return tag;}
    private static void age(UiSpring spring,long amount)throws Exception{var f=field(spring,"updated");f.setLong(spring,f.getLong(spring)-amount);}
    private static void reduced(boolean value){net.parkabird.changedsynergy.ChangedSynergyClientConfig.CLIENT.reducedUiMotion.set(value);}
    private static java.lang.reflect.Field field(Object o,String name)throws Exception{for(Class<?> c=o instanceof Class<?> t?t:o.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);checks++;}
    private static final class Probe extends AbstractAbility<SimpleAbilityInstance> {
        boolean charging;
        Probe(){super(SimpleAbilityInstance::new);}
        @Override public boolean canUse(IAbstractChangedEntity entity){return true;}
        @Override public UseType getUseType(IAbstractChangedEntity entity){return charging?UseType.CHARGE_TIME:UseType.HOLD;}
        @Override public int getCoolDown(IAbstractChangedEntity entity){return 20;}
        @Override public int getChargeTime(IAbstractChangedEntity entity){return 20;}
    }
}
