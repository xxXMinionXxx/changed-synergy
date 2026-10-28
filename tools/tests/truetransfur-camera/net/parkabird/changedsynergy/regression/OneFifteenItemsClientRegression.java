package net.parkabird.changedsynergy.regression;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.file.Path;
import java.util.List;
import net.ltxprogrammer.changed.init.ChangedItems;
import net.ltxprogrammer.changed.init.ChangedTabs;
import net.ltxprogrammer.changed.item.Syringe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.parkabird.changedsynergy.init.ChangedSynergyTransfurVariants;

/** Native item models, tint handlers and populated creative contents in the real client. */
final class OneFifteenItemsClientRegression {
    static void run(Minecraft mc, Path directory)throws Exception {
        var form=ChangedSynergyTransfurVariants.LATEX_SILK_MOTH.get();
        var spin=net.parkabird.changedsynergy.init.ChangedSynergyAbilities.SPIN_SILK.get();
        var model=mc.getModelManager().getModel(new net.minecraft.client.resources.model.ModelResourceLocation(
                net.parkabird.changedsynergy.init.ChangedSynergyAbilities.SPIN_SILK.getId(),"ability"));
        if(model==mc.getModelManager().getMissingModel())throw new AssertionError("Missing silk ability model");
        var quads=model.getQuads(null,null,net.minecraft.util.RandomSource.create());
        if(quads.stream().noneMatch(q->q.getSprite().contents().name().toString().equals("minecraft:item/string"))
                ||quads.stream().noneMatch(q->q.getSprite().contents().name().toString().equals("changed:abilities/create_cobweb")&&q.getTintIndex()==0))
            throw new AssertionError("Silk icon must contain both the native string and plus badge");
        var abstraction=net.ltxprogrammer.changed.ability.IAbstractChangedEntity.forPlayer(mc.player);
        var preview=net.ltxprogrammer.changed.process.ProcessTransfur.setPlayerTransfurVariant(mc.player,form);
        var selected=preview.getAbilityInstance(spin);
        if(spin.getAbilityName(abstraction).getString().contains("ability.changed_synergy")
                ||spin.getAbilityDescription(abstraction).stream().anyMatch(c->c.getString().contains("ability.changed_synergy")))
            throw new AssertionError("Silk ability must have a translated name and description");
        var items=List.of(ChangedItems.LATEX_SYRINGE.get(),ChangedItems.LATEX_FLASK.get(),ChangedItems.LATEX_TIPPED_ARROW.get());
        CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(),true,mc.level.registryAccess());
        var tab=ChangedTabs.TAB_CHANGED_ITEMS.get();
        var ordered=new java.util.ArrayList<>(tab.getDisplayItems());
        int previous=-1;
        for(var item:items) {
            int moth=-1,last=-1;
            for(int i=0;i<ordered.size();i++)if(ordered.get(i).is(item)) {
                last=i;if(Syringe.getVariant(ordered.get(i))==form)moth=i;
            }
            if(moth<0||moth!=last||moth<=previous||moth==0||!ordered.get(moth-1).is(item))
                throw new AssertionError("Moth transfur item is outside its native creative group: "+item);
            previous=moth;
        }
        int width=mc.getWindow().getGuiScaledWidth(),height=mc.getWindow().getGuiScaledHeight();
        RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,width,height,0,1000,3000),
                com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
        var target=new TextureTarget(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true,Minecraft.ON_OSX);
        try {
            target.setClearColor(.10F,.10F,.10F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            graphics.drawCenteredString(mc.font,"Latex Silk Moth",width/2,22,0xEADCC6);
            for(int i=0;i<items.size();i++) {
                var stack=Syringe.setPureVariant(new ItemStack(items.get(i)),form.getFormId());
                if(Syringe.getVariant(stack)!=form)throw new AssertionError("Moth variant lost in item");
                if(tab.getDisplayItems().stream().noneMatch(s->s.is(stack.getItem())&&Syringe.getVariant(s)==form))
                    throw new AssertionError("Moth item missing from native creative tab: "+stack);
                if((mc.getItemColors().getColor(stack,0)&0xFFFFFF)!=0xEADCC6
                        || (mc.getItemColors().getColor(stack,1)&0xFFFFFF)!=0xB19B7A)
                    throw new AssertionError("Moth item is not tinted with its own cream colour");
                var name=stack.getHoverName().getString();
                if(name.contains("item.changed")||name.contains("entity.changed"))throw new AssertionError("Untranslated item name: "+name);
                int x=width/2-80+i*72;
                graphics.pose().pushPose();graphics.pose().translate(x,60,0);graphics.pose().scale(3,3,1);
                graphics.renderItem(stack,0,0);graphics.pose().popPose();
                graphics.drawCenteredString(mc.font,Component.literal(name),x+24,122,0xFFFFFF);
            }
            graphics.flush();
            try(var image=Screenshot.takeScreenshot(target)){image.writeToFile(directory.resolve("silk-moth-native-items.png"));}
            abilityAlignment(mc,directory,preview,selected);
            System.out.println("ONE_FIFTEEN_ITEMS_CLIENT_REGRESSION_PASS: three native tinted icons, creative entries and translated names");
        } finally {target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
    }
    private static void abilityAlignment(Minecraft mc,Path directory,
            net.ltxprogrammer.changed.entity.variant.TransfurVariantInstance<?> variant,
            net.ltxprogrammer.changed.ability.AbstractAbilityInstance selected)throws Exception {
        for(int scale:new int[]{2,3}) {
            mc.options.guiScale().set(scale);mc.resizeDisplay();
            int width=mc.getWindow().getGuiScaledWidth(),height=mc.getWindow().getGuiScaledHeight();
            RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,width,height,0,1000,3000),
                    com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            var target=new TextureTarget(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true,Minecraft.ON_OSX);
            try {
                target.setClearColor(.10F,.10F,.10F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
                var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());int x=width/2-24,y=height/2-24;
                net.ltxprogrammer.changed.client.ChangedClient.abilityRenderer.getOrThrow()
                        .renderAndDecorateAbility(graphics,mc.player,selected,x,y,48,1,false,0);graphics.flush();
                try(var image=Screenshot.takeScreenshot(target)) {
                    int minX=image.getWidth(),minY=image.getHeight(),maxX=-1,maxY=-1;
                    for(int py=0;py<image.getHeight();py++)for(int px=0;px<image.getWidth();px++) {
                        int rgba=image.getPixelRGBA(px,py),red=rgba&255,g=(rgba>>>8)&255,blue=(rgba>>>16)&255;
                        if(Math.abs(red-26)+Math.abs(g-26)+Math.abs(blue-26)>60) {
                            minX=Math.min(minX,px);minY=Math.min(minY,py);maxX=Math.max(maxX,px);maxY=Math.max(maxY,py);
                        }
                    }
                    double centerX=(minX+maxX+1D)/2*width/image.getWidth();
                    double centerY=(minY+maxY+1D)/2*height/image.getHeight();
                    int badge=sample(image,(x+37.5)*image.getWidth()/width,(y+37.5)*image.getHeight()/height);
                    for(int cy=0;cy<5;cy++)for(int cx=0;cx<5;cx++)if(cx==2||cy==2) {
                        int pixel=sample(image,(x+(10+cx+.5)*3)*image.getWidth()/width,(y+(10+cy+.5)*3)*image.getHeight()/height);
                        if(difference(pixel,badge)>12)throw new AssertionError("Native 5x5 cross has a missing or miscoloured pixel");
                    }
                    if(difference(badge,image.getPixelRGBA(0,0))<60||Math.abs(centerX-(x+24))>2.5||Math.abs(centerY-(y+24))>2.5)
                        throw new AssertionError("Silk badge colour/alignment: center="+centerX+","+centerY+" expected="+(x+24)+","+(y+24));
                }
                graphics.drawCenteredString(mc.font,"Spin Silk: centred icon + native 5x5 badge",width/2,24,0xFFFFFF);
                var scheme=net.ltxprogrammer.changed.client.gui.AbstractRadialScreen.getColors(variant).setForegroundToBright();
                net.ltxprogrammer.changed.client.gui.AbilityOverlay.renderBackground(25,55,graphics,scheme,mc.player,variant,selected);
                net.ltxprogrammer.changed.client.gui.AbilityOverlay.renderForeground(30,50,graphics,scheme,mc.player,variant,selected);
                graphics.flush();
                try(var image=Screenshot.takeScreenshot(target)){image.writeToFile(directory.resolve("silk-ability-alignment-scale-"+scale+".png"));}
            } finally {target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
        }
        System.out.println("SILK_ABILITY_ICON_ALIGNMENT_PASS: native wheel/HUD model, original 5x5 grey cross and centred visible bounds at GUI scales 2/3");
    }
    private static int sample(com.mojang.blaze3d.platform.NativeImage image,double x,double y){return image.getPixelRGBA((int)x,(int)y);}
    private static int difference(int a,int b){return Math.abs((a&255)-(b&255))+Math.abs(((a>>>8)&255)-((b>>>8)&255))+Math.abs(((a>>>16)&255)-((b>>>16)&255));}
}
