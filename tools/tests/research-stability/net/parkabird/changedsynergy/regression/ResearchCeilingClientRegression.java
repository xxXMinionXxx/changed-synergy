package net.parkabird.changedsynergy.regression;

import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;
import net.parkabird.changedsynergy.world.ResearchLabStructure;

@Mod.EventBusSubscriber(modid="changed_synergy",value=Dist.CLIENT)
public final class ResearchCeilingClientRegression {
    private static int stage,frames,index;
    private static CompletableFuture<Void> task;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("changed_synergy.researchCeilingClient")||stage==9)return;
        var mc=Minecraft.getInstance();
        try {
            if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null) {
                if(!mc.gameDirectory.getName().startsWith("author-feedback-client-"))throw new AssertionError("private fixture only");
                mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(5);mc.options.fov().set(95);
                org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
                var rules=new GameRules();rules.getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false,null);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("ceiling-preview",new LevelSettings("ceiling-preview",GameType.SPECTATOR,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(220261025L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());stage=1;
            }
            if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;
            mc.setWindowActive(true);mc.options.hideGui=true;mc.getTutorial().stop();
            if(stage==1&&mc.player.tickCount>20) {
                task=CompletableFuture.runAsync(()->{
                    var server=mc.getSingleplayerServer();var level=server.overworld();var anchor=new BlockPos(0,100,0);
                    var piece=new ResearchLabStructure.Piece(server.getStructureManager(),new ResourceLocation("changed_synergy","red_lab_tower"),anchor,Rotation.NONE,false);
                    var box=piece.getBoundingBox();
                    for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++) {
                        level.getChunk(x,z);var chunk=new net.minecraft.world.level.ChunkPos(x,z);
                        piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(4),new net.minecraft.world.level.levelgen.structure.BoundingBox(chunk.getMinBlockX(),level.getMinBuildHeight(),chunk.getMinBlockZ(),chunk.getMaxBlockX(),level.getMaxBuildHeight()-1,chunk.getMaxBlockZ()),chunk,anchor);
                    }
                    for(var entity:level.getEntities(null,net.minecraft.world.phys.AABB.of(box)))if(entity instanceof net.minecraft.world.entity.Mob mob)mob.setNoAi(true);
                    level.setDayTime(6000);
                },mc.getSingleplayerServer());stage=2;
            }
            if(stage==2&&task.isDone()) {task.join();view(mc);stage=3;frames=0;}
            if(stage==3&&++frames>=60) {
                var directory=mc.gameDirectory.toPath().resolve("red-ceiling-previews");Files.createDirectories(directory);
                try(var img=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {img.writeToFile(directory.resolve(String.format("ceiling-%02d.png",index)));}
                System.out.println("CEILING_CAPTURE "+index);
                if(++index==8) {stage=9;System.out.println("RESEARCH_CEILING_CLIENT_PASS: 8 native interior views");mc.stop();}
                else {view(mc);frames=0;}
            }
        } catch(Throwable failure) {stage=9;System.err.println("RESEARCH_CEILING_CLIENT_FAIL");failure.printStackTrace();mc.stop();}
    }
    private static void view(Minecraft mc) {
        float yaw=index%2==0?0:-90;double y=104,x=index%2==0?18:24,z=index%2==0?20:17;
        var server=mc.getSingleplayerServer();server.execute(()->server.getPlayerList().getPlayer(mc.player.getUUID()).connection.teleport(x,y,z,yaw,-40));
        var camera=new net.minecraft.world.entity.decoration.ArmorStand(mc.level,x,y,z);
        camera.moveTo(x,y,z,yaw,-40);camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;camera.setOldPosAndRot();mc.setCameraEntity(camera);
        mc.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,6000,0,false,false));
    }
}
