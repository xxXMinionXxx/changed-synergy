package net.parkabird.changedsynergy.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.ai.CompanionSeatingService;
import net.parkabird.changedsynergy.ai.CreaturePersonality;
import net.parkabird.changedsynergy.event.LatexSocialEvents;
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;
import net.parkabird.changedsynergy.world.inventory.PlayerRelationshipMenu;

@Mod.EventBusSubscriber(modid="changed_synergy",value=Dist.CLIENT)
public final class CompanionSeatClientRegression {
    private static int stage, frames, checks;
    private static boolean finished;
    private static CompletableFuture<Void> task;
    private static net.ltxprogrammer.changed.entity.ChangedEntity creature;
    private static Boat boat;
    private static final long START=System.nanoTime();
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("changed_synergy.seatingClientRegression")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try {
            if(System.nanoTime()-START>240_000_000_000L)throw new AssertionError("seat client fixture timed out");
            if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null) {
                check(mc.gameDirectory.toPath().getFileName().toString().startsWith("author-feedback-client-"),"private fixture only");
                mc.options.pauseOnLostFocus=false;org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
                var rules=new GameRules();rules.getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false,null);rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("seating-regression",
                        new LevelSettings("seating-regression",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(73451L,false,false),registry->registry.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());stage=1;
            }
            if(mc.player==null||mc.level==null||!mc.hasSingleplayerServer()||mc.getOverlay()!=null)return;
            mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getTutorial().stop();mc.getToasts().clear();mc.gui.getChat().clearMessages(true);
            if(stage==1&&mc.player.tickCount>30) {
                task=CompletableFuture.runAsync(()->{
                    var server=mc.getSingleplayerServer();var owner=server.getPlayerList().getPlayer(mc.player.getUUID());var level=owner.serverLevel();
                    creature=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);creature.moveTo(owner.getX()+1,owner.getY(),owner.getZ()+1);level.addFreshEntity(creature);
                    CreaturePersonality.establishRelationship(creature,owner);CreaturePersonality.setFamiliarity(creature,owner,20);
                    LatexSocialEvents.openSocialMenu(owner,creature);
                },mc.getSingleplayerServer());stage=2;frames=0;
            }
            if(stage==2&&task.isDone()&&mc.screen instanceof SocialInteractionScreen screen&&++frames>35) {
                task.join();var field=SocialInteractionScreen.class.getDeclaredField("actions");field.setAccessible(true);
                check(field.get(screen).toString().contains("social_seat"),"real server menu exposes seat action");
                CompanionSeatIcons.ensureLoaded();
                for(boolean plus:new boolean[]{false,true}) {
                    check(mc.getTextureManager().getTexture(CompanionSeatIcons.id(plus)) instanceof DynamicTexture,"runtime icon registered without missing texture");
                    var pixels=((DynamicTexture)mc.getTextureManager().getTexture(CompanionSeatIcons.id(plus))).getPixels();
                    int minX=24,minY=24,maxX=-1,maxY=-1,white=0;
                    for(int y=0;y<24;y++)for(int x=0;x<24;x++) {
                        int value=pixels.getPixelRGBA(x,y);if((value>>>24)==0)continue;
                        check((value&255)==((value>>8)&255)&&((value>>8)&255)==((value>>16)&255),"saddle and plus are grayscale");
                        minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                        if(value==0xFFE0E0E0)white++;
                    }
                    check(Math.abs(minX+maxX-23)<=1&&Math.abs(minY+maxY-23)<=1,"combined visible icon centered in its canvas");
                    if(plus)check(white>=9,"small high-contrast plus remains visible");
                    pixels.writeToFile(directory(mc).resolve(plus?"saddle-plus.png":"saddle-gray.png"));
                }
                capture(mc,"seat-wheel.png");
                task=CompletableFuture.runAsync(()->{
                    var owner=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    CompanionSeatingService.command(owner,creature,"social_seat");
                },mc.getSingleplayerServer());stage=3;frames=0;
            }
            if(stage==3&&task.isDone()&&CompanionSeatClient.active()&&mc.screen==null&&++frames>25) {
                task.join();check(!CompanionSeatClient.hint().getString().contains("hud.changed"),"selection hint translated");capture(mc,"seat-selection-hud.png");
                task=CompletableFuture.runAsync(()->{
                    var owner=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    boat=new Boat(EntityType.BOAT,owner.serverLevel());boat.moveTo(owner.getX(),owner.getY(),owner.getZ());owner.serverLevel().addFreshEntity(boat);
                    owner.startRiding(boat);PlayerRelationshipMenu.open(owner);
                },mc.getSingleplayerServer());stage=4;frames=0;
            }
            if(stage==4&&task.isDone()&&mc.screen instanceof PlayerRelationshipScreen screen&&++frames>35) {
                task.join();check(!CompanionSeatClient.active(),"opening a new wheel cancels target selection");
                check(screen.getMenu().canRallyBoarding(),"real mounted player menu exposes rally");capture(mc,"rally-wheel.png");
                finished=true;System.out.println("SEATING_CLIENT_REGRESSION_PASS: "+checks+" grayscale, plus, canvas alignment, real menu, selection HUD and rally checks; "+directory(mc));mc.stop();
            }
        } catch(Throwable failure) { finished=true;System.err.println("SEATING_CLIENT_REGRESSION_FAIL");failure.printStackTrace();mc.stop(); }
    }
    private static Path directory(Minecraft mc)throws Exception {
        var path=mc.gameDirectory.toPath().getParent().resolve("companion-seating-visuals");Files.createDirectories(path);return path;
    }
    private static void capture(Minecraft mc,String name)throws Exception {
        try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(directory(mc).resolve(name));}
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
}
