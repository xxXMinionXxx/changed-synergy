package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.parkabird.changedsynergy.client.PartialSkinLayersCompat;

final class PartialSkinLayersClientRegression {
    private static int checks;
    static void run(Minecraft mc) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("skinlayers3d")) {
            PartialSkinLayersCompat.apply(null,null);
            System.out.println("PARTIAL_SKIN_LAYERS_ABSENT_PASS: optional compatibility does not load its absent API");return;
        }
        var image=new NativeImage(64,64,true);
        for(int x=0;x<64;x++)for(int y=0;y<64;y++)image.setPixelRGBA(x,y,0xFF77BBEE);
        var texture=mc.getTextureManager().register("synergy_skin_layers_test",new DynamicTexture(image));
        var injector=Class.forName("dev.tr7zw.skinlayers.accessor.ModelPartInjector");
        var get=injector.getMethod("getInjectedMesh");
        String[] names={"Hat","Jacket","LeftSleeve","RightSleeve","LeftPants","RightPants"};
        try {
            for(boolean slim:new boolean[]{false,true}) {
                var player=new TestPlayer(mc.level,texture,slim);player.setPos(mc.player.position());
                String[][] models={
                    {"net.ltxprogrammer.changed.client.renderer.model.LatexHumanModel",slim?"LAYER_LOCATION_SLIM":"LAYER_LOCATION"},
                    {"net.ltxprogrammer.changed.client.renderer.model.DarkLatexWolfPartialModel",slim?"LAYER_LOCATION_HUMAN_SLIM":"LAYER_LOCATION_HUMAN"},
                    {"net.foxyas.changedaddon.client.model.SnowLeopardPartialModel",slim?"LAYER_LOCATION_HUMAN_SLIM":"LAYER_LOCATION_HUMAN"}};
                for(int kind=0;kind<models.length;kind++) {
                    if(kind==2&&!net.minecraftforge.fml.ModList.get().isLoaded("changed_addon"))continue;
                    Class<?> type=Class.forName(models[kind][0]);
                    var root=mc.getEntityModels().bakeLayer((ModelLayerLocation)type.getField(models[kind][1]).get(null));
                    Object model=kind==0?type.getConstructor(ModelPart.class).newInstance(root):type.getMethod("human",ModelPart.class).invoke(null,root);
                    type.getMethod("setModelProperties",net.minecraft.client.player.AbstractClientPlayer.class).invoke(model,player);
                    ModelPart[] parts=new ModelPart[6];Object[] meshes=new Object[6];
                    for(int i=0;i<6;i++) {
                        var field=type.getDeclaredField(names[i]);field.setAccessible(true);ModelPart part=(ModelPart)field.get(model);
                        if(kind>0&&i>=4)part=part.getChild(i==4?"LeftThighLayer_r1":"RightThighLayer_r1");
                        parts[i]=part;meshes[i]=get.invoke(part);
                        check(meshes[i]!=null,"Actual 3D mesh installed on "+type.getSimpleName()+"/"+names[i]+"/slim="+slim);
                        var vertices=new Bounds();part.render(new com.mojang.blaze3d.vertex.PoseStack(),vertices,15728880,0);
                        check(vertices.count>0,"Injected outer skin actually emits 3D vertices");
                        if(kind>0&&i>=2)check(vertices.maxY<.62,"Partial mesh ends above the transformed hands/legs; maxY="+vertices.maxY);
                    }
                    if(kind>0) {
                        Class<?> data=Class.forName("dev.tr7zw.skinlayers.api.PlayerData");
                        check(meshes[2]!=data.getMethod("getLeftArmMesh").invoke(player)
                                &&meshes[4]!=data.getMethod("getLeftLegMesh").invoke(player),"Partial sleeves and thighs use cropped meshes rather than full human limbs");
                    }
                    player.shown=false;type.getMethod("setModelProperties",net.minecraft.client.player.AbstractClientPlayer.class).invoke(model,player);
                    check(!parts[0].visible&&!parts[1].visible,"Disabled skin customization parts remain hidden");player.shown=true;
                    player.setPos(mc.player.getX()+100,mc.player.getY(),mc.player.getZ());
                    type.getMethod("setModelProperties",net.minecraft.client.player.AbstractClientPlayer.class).invoke(model,player);
                    for(var part:parts)check(get.invoke(part)==null,"Distance LOD restores the original flat outer layer");
                    player.setPos(mc.player.position());
                    type.getMethod("setModelProperties",net.minecraft.client.player.AbstractClientPlayer.class).invoke(model,player);
                    type.getMethod("defaultModelProperties").invoke(model);
                    for(var part:parts)check(get.invoke(part)==null,"NPC/default render clears the previous player's 3D mesh");
                }
            }
            System.out.println("PARTIAL_SKIN_LAYERS_CLIENT_REGRESSION_PASS: "+checks+" actual installed-API, classic/slim, cropped partial-limb and NPC reset checks");
        } finally {mc.getTextureManager().release(texture);}
    }
    private static final class TestPlayer extends RemotePlayer {
        private final ResourceLocation texture;private final boolean slim;
        boolean shown=true;
        TestPlayer(ClientLevel level,ResourceLocation texture,boolean slim) {
            super(level,new GameProfile(UUID.randomUUID(),"SkinLayersTest"));this.texture=texture;this.slim=slim;
        }
        @Override public ResourceLocation getSkinTextureLocation(){return texture;}
        @Override public String getModelName(){return slim?"slim":"default";}
        @Override public boolean isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart part){return shown;}
    }
    private static final class Bounds implements com.mojang.blaze3d.vertex.VertexConsumer {
        int count;double maxY=-Double.MAX_VALUE;
        @Override public Bounds vertex(double x,double y,double z){count++;maxY=Math.max(maxY,y);return this;}
        @Override public Bounds color(int r,int g,int b,int a){return this;}
        @Override public Bounds uv(float u,float v){return this;}
        @Override public Bounds overlayCoords(int u,int v){return this;}
        @Override public Bounds uv2(int u,int v){return this;}
        @Override public Bounds normal(float x,float y,float z){return this;}
        @Override public void endVertex(){}
        @Override public void defaultColor(int r,int g,int b,int a){}
        @Override public void unsetDefaultColor(){}
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
}
