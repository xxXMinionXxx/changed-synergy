package net.parkabird.changedsynergy.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedKeyMappings;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.client.gui.AbilityRadialScreen;
import net.ltxprogrammer.changed.world.inventory.AbilityRadialMenu;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;
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
public final class UiSpringClientRegression {
    private static int stage, frames, checks;
    private static int heartbeat;
    private static boolean finished;
    private static CompletableFuture<Void> task;
    private static net.ltxprogrammer.changed.entity.ChangedEntity creature;
    private static Boat boat;
    private static AbilityRadialScreen tested;
    private static net.ltxprogrammer.changed.ability.AbstractAbility<?> expected;
    private static UiSpring transferSpring;
    private static boolean arrivalChecked;
    private static int closingMovementChecks;
    private static int brightnessSlot;
    private static long brightnessBegan;
    private static boolean brightnessRampObserved;
    private static final long START=System.nanoTime();
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("changed_synergy.uiSpringClientRegression")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try {
            if(++heartbeat%100==0) {
                System.out.println("UI_FIXTURE_STAGE "+stage+" screen="+(mc.screen==null?"null":mc.screen.getClass().getSimpleName())+" frames="+frames+" same="+(mc.screen==tested));
                if(stage==7)for(Class<?> c=tested.getClass();c!=null;c=c.getSuperclass())for(var f:c.getDeclaredFields())if(f.getName().contains("closing")||f.getName().contains("completion")){f.setAccessible(true);System.out.println(f.getName()+"="+f.get(tested));}
            }
            if(System.nanoTime()-START>240_000_000_000L)throw new AssertionError("UI client fixture timed out at "+stage);
            if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null) {
                check(mc.gameDirectory.toPath().getFileName().toString().startsWith("author-feedback-client-"),"private fixture only");
                mc.options.pauseOnLostFocus=false;org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
                var rules=new GameRules();rules.getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false,null);rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("seating-regression",
                        new LevelSettings("seating-regression",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(73451L,false,false),registry->registry.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());stage=1;
            }
            if(mc.player==null||mc.level==null||!mc.hasSingleplayerServer()||mc.getOverlay()!=null)return;
            // This is our hidden private window; simulate focus only for injected input checks.
            mc.setWindowActive(true);
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
                press(false);agePress();
                task=CompletableFuture.runAsync(()->{
                    var owner=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    boat=new Boat(EntityType.BOAT,owner.serverLevel());boat.moveTo(owner.getX(),owner.getY(),owner.getZ());owner.serverLevel().addFreshEntity(boat);
                    owner.startRiding(boat);PlayerRelationshipMenu.open(owner);
                },mc.getSingleplayerServer());stage=4;frames=0;
            }
            if(stage==4&&task.isDone()&&mc.screen instanceof PlayerRelationshipScreen screen&&++frames>35) {
                task.join();check(!CompanionSeatClient.active(),"opening a new wheel cancels target selection");
                check(AbilityWheelInput.ownsPress()&&AbilityWheelInput.allowsMovement(mc.options.keyUp),"holding through a human player's initial relationship opening permits movement");release();
                check(screen.getMenu().canRallyBoarding(),"real mounted player menu exposes rally");capture(mc,"rally-wheel.png");
                task=CompletableFuture.runAsync(()->{
                    var owner=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                    owner.closeContainer();owner.stopRiding();
                    creature.moveTo(owner.position());creature.startRiding(boat);
                    CreaturePersonality.setSocialFollowing(creature,owner,true);
                    creature.getPersistentData().remove("ChangedSynergyCommandSeat");
                    LatexSocialEvents.openSocialMenu(owner,creature);
                },mc.getSingleplayerServer());stage=41;frames=0;
            }
            if(stage==41&&task.isDone()&&mc.screen instanceof SocialInteractionScreen screen&&++frames>16) {
                task.join();var actions=(java.util.List<?>)field(screen,"actions").get(screen);
                check(actions.get(0).toString().contains("social_disembark"),"unmarked seated friend's exit is on the first wheel page");
                capture(mc,"disembark-wheel.png");
                task=CompletableFuture.runAsync(()->{
                    var owner=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());PlayerRelationshipMenu.open(owner);
                },mc.getSingleplayerServer());stage=42;frames=0;
            }
            if(stage==42&&task.isDone()&&mc.screen instanceof PlayerRelationshipScreen screen&&++frames>12) {
                task.join();screen.handleClicked(2,new net.ltxprogrammer.changed.util.SingleRunnable(()->{}));stage=43;frames=0;
            }
            if(stage==43&&!creature.isPassenger()&&++frames>8) {
                var owner=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                check(CreaturePersonality.isSocialFollowing(creature,owner),"Travel together disembarks an already-following seated friend and keeps follow enabled");
                check(!creature.getPersistentData().contains("ChangedSynergyCommandSeat"),"safe exit removes temporary seat state");
                task=CompletableFuture.runAsync(()->{
                    owner.closeContainer();creature.moveTo(owner.getX()+8,owner.getY(),owner.getZ()+8);
                    ProcessTransfur.setPlayerTransfurVariant(owner,ChangedTransfurVariants.LATEX_HYPNO_CAT.get(),null,1,false);
                },mc.getSingleplayerServer());stage=5;frames=0;
            }
            if(stage==5&&task.isDone()&&ProcessTransfur.getPlayerTransfurVariant(mc.player)!=null&&mc.screen==null&&++frames>15) {
                task.join();press(true);stage=6;frames=0;
            }
            if(stage==6&&mc.screen instanceof AbilityRadialScreen screen&&++frames>12) {
                tested=screen;check(AbilityWheelInput.holding(screen),"physical long press activates directional mode");
                int index=-1;for(int i=0;i<Math.min(8,screen.getCount());i++) {
                    var instance=screen.variant.getAbilityInstance(screen.abilities.get(i));
                    if(!instance.getUseType().name().equals("MENU")){index=i;break;}
                }
                check(index>=0,"non-menu ability available");expected=screen.abilities.get(index);
                point(mc,screen,index,150);check(screen.getSectionAt(pointerX(mc),pointerY(mc)).orElse(-1)==index,"far cursor direction selects beyond native boxes");
                var preview = new AbilityRadialScreen(screen.menu,mc.player.getInventory(),Component.empty()) {
                    @Override public java.util.List<Component> tooltipsFor(int section){return null;}
                };
                mc.setScreen(preview);tested=preview;point(mc,preview,index,85);
                stage=61;frames=0;
            }
            if(stage==61&&mc.screen instanceof AbilityRadialScreen screen&&++frames>8) {
                capture(mc,"ability-hold.png");
                var tilt=field(screen,"changedSynergy$tiltX");
                check(Math.abs(((UiSpring)tilt.get(screen)).value(System.nanoTime()))>1,"held wheel tilts toward cursor");
                var lifts=(UiSpring[])field(screen,"changedSynergy$preselection").get(screen);
                int selected=AbilityWheelInput.selection(screen,pointerX(mc),pointerY(mc)).orElseThrow()-RadialWheelAnimations.viewOffset(screen);
                for(int i=0;i<lifts.length;i++)check(i==selected?lifts[i].value(System.nanoTime())>5:lifts[i].value(System.nanoTime())<.01,"only the directionally preselected frame protrudes six GUI pixels");
                checkSelectedIcon(mc,screen,selected);
                check(screen.getSectionAt(pointerX(mc),pointerY(mc)).orElseThrow()==selected+RadialWheelAnimations.viewOffset(screen),"outward frame motion cannot change the selected direction");
                nativeKey(mc,GLFW.GLFW_KEY_W,GLFW.GLFW_PRESS);nativeKey(mc,GLFW.GLFW_KEY_LEFT_CONTROL,GLFW.GLFW_PRESS);
                check(mc.options.keyUp.isDown()&&mc.options.keySprint.isDown(),"movement is down before release");release();
                check(!AbilityWheelInput.holding(screen),"release consumes gesture before native ability callback");
                check(AbilityWheelInput.showHoldHint(screen),"release does not flash the click-mode hint during closing");
                check(screen.variant.selectedAbility==expected,"release selects native ability");stage=7;frames=0;
            }
            if(stage==7&&mc.screen==tested) {
                check(AbilityWheelInput.allowsMovement(mc.options.keyUp),"closing retains movement context after consuming the wheel gesture");
                check(mc.options.keyUp.isDown()&&mc.options.keySprint.isDown(),"movement and sprint stay down throughout closing");
                mc.player.input.tick(false,.3F);check(mc.player.input.forwardImpulse>0,"vanilla movement input never pauses while the held wheel closes");
                check(AbilityWheelInput.showHoldHint(tested),"closing keeps the same footer description");closingMovementChecks++;
                if(closingMovementChecks==2)capture(mc,"ability-hold-closing.png");
            }
            if(stage==7&&mc.screen==null&&++frames>8) {
                check(closingMovementChecks>0,"observed the real closing animation");
                check(!AbilityWheelInput.allowsMovement(mc.options.keyUp),"closing movement context clears when the screen leaves");
                nativeKey(mc,GLFW.GLFW_KEY_W,GLFW.GLFW_RELEASE);nativeKey(mc,GLFW.GLFW_KEY_LEFT_CONTROL,GLFW.GLFW_RELEASE);
                capture(mc,"ability-hud.png");
                var owner=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                check(ProcessTransfur.getPlayerTransfurVariant(owner).selectedAbility==expected,"server receives native ability selection");
                press(true);release();stage=8;frames=0;
            }
            if(stage==8&&mc.screen instanceof AbilityRadialScreen screen&&++frames>12) {
                check(!AbilityWheelInput.holding(screen),"short tap leaves sticky click-driven wheel");
                check(!AbilityWheelInput.showHoldHint(screen),"sticky tap mode uses its normal hint");
                brightnessSlot=(screen.abilities.indexOf(expected)+1)%Math.min(8,screen.getCount());
                point(mc,screen,brightnessSlot,90);stage=81;frames=0;brightnessBegan=System.nanoTime();brightnessRampObserved=false;
            }
            if(stage==81&&mc.screen instanceof AbilityRadialScreen screen) {
                frames++;
                float highlight=(float)((UiSpring[])field(screen,"changedSynergy$highlights").get(screen))[brightnessSlot].value(System.nanoTime());
                if(highlight>0&&highlight<.99)brightnessRampObserved=true;
                if(highlight<.999||!brightnessRampObserved){if(System.nanoTime()-brightnessBegan>2_000_000_000L)throw new AssertionError("wheel highlight did not fade in");return;}
                check(brightnessRampObserved,"ordinary wheel highlight fades in instead of switching textures instantly");
                int selected=screen.getSectionAt(pointerX(mc),pointerY(mc)).orElseThrow()-RadialWheelAnimations.viewOffset(screen);
                check(((UiSpring[])field(screen,"changedSynergy$preselection").get(screen))[selected].value(System.nanoTime())>5,"ordinary click wheels share the selected outward lift");
                check(Math.abs(((UiSpring)field(screen,"changedSynergy$tiltX").get(screen)).value(System.nanoTime()))>1,"ordinary click wheels share the cursor tilt");
                check(frameScale(screen,selected)>1.04,"selected frame grows slightly above its resting size");
                check(highlight>.99,"hovered option reaches normal selected brightness");
                capture(mc,"ability-click-hover.png");
                checkFrameBlend(mc,screen);
                AbilityHudGlowChecks.run(mc);
                point(mc,screen,0,0);stage=82;frames=0;brightnessBegan=System.nanoTime();brightnessRampObserved=false;
            }
            if(stage==82&&mc.screen instanceof AbilityRadialScreen screen) {
                frames++;
                float highlight=(float)((UiSpring[])field(screen,"changedSynergy$highlights").get(screen))[brightnessSlot].value(System.nanoTime());
                if(highlight>0&&highlight<.99)brightnessRampObserved=true;
                if(highlight>.001||!brightnessRampObserved){if(System.nanoTime()-brightnessBegan>2_000_000_000L)throw new AssertionError("wheel highlight did not fade out; value="+highlight+" slot="+brightnessSlot+" active="+screen.abilities.indexOf(screen.variant.selectedAbility));return;}
                check(brightnessRampObserved,"highlight fades out when the cursor leaves");
                check(highlight<.001,"unhovered option returns to its original dim artwork");
                press(false);agePress();point(mc,screen,0,0);release();stage=9;frames=0;
            }
            if(stage==9&&mc.screen==null&&++frames>8) {
                check(ProcessTransfur.getPlayerTransfurVariant(mc.player).selectedAbility==expected,"center release cancels without selection");
                var screen=new Paged(new AbilityRadialMenu(120,mc.player.getInventory()),mc);mc.setScreen(screen);
                press(false);
                check(AbilityWheelInput.showHoldHint(screen)&&!AbilityWheelInput.holding(screen),"first-frame hold hint does not wait for the activation threshold");
                agePress();check(AbilityWheelInput.showHoldHint(screen),"hold hint remains stable after the threshold");point(mc,screen,1,100);screen.scrollDown();
                check(RadialWheelAnimations.viewOffset(screen)==1,"page advances one item");
                check(screen.getSectionAt(pointerX(mc),pointerY(mc)).isEmpty(),"page change disarms old cursor selection");
                point(mc,screen,2,115);check(screen.getSectionAt(pointerX(mc),pointerY(mc)).orElse(-1)==3,"real cursor movement rearms next page");
                screen.scrollDown();screen.scrollUp();
                check(RadialWheelAnimations.viewOffset(screen)==1,"rapid paging retargets without input lock");
                var texture=net.ltxprogrammer.changed.client.gui.VariantRadialScreen.class.getDeclaredMethod("getTextureForSection",net.ltxprogrammer.changed.entity.variant.TransfurVariant.class,int.class,boolean.class,boolean.class);
                texture.setAccessible(true);var previous=screen.variant.selectedAbility;screen.variant.selectedAbility=screen.abilities.get(1);
                check(texture.invoke(screen,null,0,false,false).toString().contains("_selected/0"),"paged native selected frame follows action offset");screen.variant.selectedAbility=previous;
                nativeKey(mc,GLFW.GLFW_KEY_W,GLFW.GLFW_PRESS);
                AbilityWheelInput.key(new InputEvent.Key(GLFW.GLFW_KEY_ESCAPE,0,GLFW.GLFW_PRESS,0));
                check(!AbilityWheelInput.holding(screen)&&AbilityWheelInput.allowsMovement(mc.options.keyUp),"Escape consumes selection without interrupting movement during closing");
                nativeKey(mc,GLFW.GLFW_KEY_W,GLFW.GLFW_RELEASE);
                check(!mc.options.keyUp.isDown(),"movement key releases still take effect during closing");
                mc.setScreen(null);release();
                press(true);stage=10;frames=0;
            }
            if(stage==10&&mc.screen instanceof AbilityRadialScreen screen&&++frames>8) {
                point(mc,screen,0,110);agePress();
                var e=new ScreenEvent.MouseScrolled.Pre(screen,screen.width/2.0,screen.height/2.0,1);
                capture(mc,"slide-right-start.png");
                PlayerRelationshipClientEvents.onMouseScrolled(e);check(e.isCanceled(),"edge scroll requests relationship handoff");
                transferSpring=((RadialWheelAnimationAccess)(Object)screen).changedSynergy$getHorizontalSpring();
                checkFullFrames(screen);arrivalChecked=false;
                release();stage=11;frames=0;
            }
            if(stage==11&&mc.screen instanceof PlayerRelationshipScreen screen) {
                if(!arrivalChecked) {
                    check(((RadialWheelAnimationAccess)(Object)screen).changedSynergy$getHorizontalSpring()==transferSpring,"incoming relationship wheel inherits the same spring and momentum");
                    checkFullFrames(screen);check(RadialWheelAnimations.overallAlpha(screen)>.99F,"incoming wheel does not fade out and reopen");
                    arrivalChecked=true;
                }
                if(frames<8)capture(mc,"slide-right-"+frames+".png");
            }
            if(stage==11&&mc.screen instanceof PlayerRelationshipScreen screen&&++frames>18) {
                check(!AbilityWheelInput.holding(tested),"relationship handoff ends hold gesture");capture(mc,"relationship-theme-cat.png");
                check(screen.getSectionAt(screen.width/2,screen.height/2).isEmpty(),"relationship wheel retains native click hitboxes");
                // Snapshot layout during a departure: only draw transforms may change.
                var xf=PlayerRelationshipScreen.class.getDeclaredField("contactHeaderWidth");xf.setAccessible(true);
                int widthBefore=xf.getInt(screen);check(widthBefore>0,"relationship panel measured");
                var hit=PlayerRelationshipScreen.class.getDeclaredMethod("contactButtonAt",double.class,double.class);hit.setAccessible(true);
                double cx=field(screen,"contactHeaderX").getInt(screen),cy=field(screen,"contactHeaderY").getInt(screen);
                float scale=field(screen,"contactHeaderScale").getFloat(screen);
                for(int i=0;i<3;i++)check((int)hit.invoke(screen,cx+widthBefore+(-48+i*18)*scale,cy+10*scale)==i,"scaled contact button "+i+" shares the same visual center line");
                RadialWheelAnimations.requestWheelSwitch(screen,()->{});stage=12;frames=0;expectedWidth=widthBefore;
            }
            if(stage==12&&mc.screen instanceof PlayerRelationshipScreen screen&&++frames>=2
                    &&System.nanoTime()-field(screen,"changedSynergy$closingAtNanos").getLong(screen)>=60_000_000L) {
                var xf=PlayerRelationshipScreen.class.getDeclaredField("contactHeaderWidth");xf.setAccessible(true);
                check(xf.getInt(screen)==expectedWidth,"animated wheel departure cannot reflow panel");
                float panelAlpha=RadialWheelAnimations.panelAlpha(screen);
                System.out.println("PANEL_EXIT_OBSERVATION alpha="+panelAlpha+" overall="+RadialWheelAnimations.overallAlpha(screen)+" reduced="+UiMotion.reduced()
                        +" elapsed="+(System.nanoTime()-field(screen,"changedSynergy$closingAtNanos").getLong(screen))/1_000_000+" continuous="+field(screen,"changedSynergy$continuousSwitch").getBoolean(screen));
                check(panelAlpha>0&&panelAlpha<.95F,"relationship panels fade out before handing back to abilities");
                check(RadialWheelAnimations.overallAlpha(screen)>.99F,"panel exit keeps the wheel visible at full size");
                capture(mc,"relationship-mid-switch.png");checkFullFrames(screen);
                field(screen,"changedSynergy$closingAtNanos").setLong(screen,System.nanoTime()-3_100_000_000L);
                stage=121;frames=0;
            }
            if(stage==121&&mc.screen instanceof PlayerRelationshipScreen screen&&++frames>8) {
                check(!(boolean)field(screen,"changedSynergy$closing").get(screen),"missing server response restores the original wheel");
                checkFullFrames(screen);transferSpring=((RadialWheelAnimationAccess)(Object)screen).changedSynergy$getHorizontalSpring();
                var e=new ScreenEvent.MouseScrolled.Pre(screen,screen.width/2.0,screen.height/2.0,1);
                PlayerRelationshipClientEvents.onMouseScrolled(e);check(e.isCanceled(),"relationship wheel requests real return to abilities");
                stage=122;frames=0;arrivalChecked=false;
            }
            if(stage==122&&mc.screen instanceof AbilityRadialScreen screen) {
                if(!arrivalChecked) {
                    check(((RadialWheelAnimationAccess)(Object)screen).changedSynergy$getHorizontalSpring()==transferSpring,"returning ability wheel inherits the same spring");
                    checkFullFrames(screen);arrivalChecked=true;
                }
                if(frames<8)capture(mc,"slide-center-"+frames+".png");
                if(++frames>18) {
                    check(Math.abs(RadialWheelAnimations.horizontalOffset(screen))<.1F,"returning wheel settles at screen center");
                    mc.player.closeContainer();stage=13;frames=0;
                }
            }
            if(stage==13&&mc.screen==null&&++frames>8) {
                // Rebound mouse button follows the same physical press/release path.
                var mapping=ChangedKeyMappings.SELECT_ABILITY;var original=mapping.getKey();
                mapping.setKey(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(4));
                var screen=new Paged(new AbilityRadialMenu(121,mc.player.getInventory()),mc);mc.setScreen(screen);
                AbilityWheelInput.screenMouse(new ScreenEvent.MouseButtonPressed.Pre(screen,0,0,4));agePress();
                check(AbilityWheelInput.holding(screen),"rebound mouse button can hold ability wheel");
                point(mc,screen,0,0);AbilityWheelInput.screenMouseUp(new ScreenEvent.MouseButtonReleased.Pre(screen,0,0,4));
                check(!AbilityWheelInput.holding(screen),"rebound mouse release consumes gesture");mapping.setKey(original);
                mc.setScreen(null);stage=14;frames=0;
            }
            if(stage==14&&++frames>10) {
                press(false);agePress();release();
                var late=new ScreenEvent.Opening(null,new AbilityRadialScreen(new AbilityRadialMenu(123,mc.player.getInventory()),mc.player.getInventory(),Component.empty()));
                AbilityWheelInput.opening(late);check(late.isCanceled(),"long release rejects late server opening");
                press(false);agePress();
                long before=field(AbilityWheelInput.class,"pressedAt").getLong(null);
                AbilityWheelInput.key(new InputEvent.Key(GLFW.GLFW_KEY_R,0,GLFW.GLFW_REPEAT,0));
                check(field(AbilityWheelInput.class,"pressedAt").getLong(null)==before,"OS repeat cannot restart gesture");
                mc.setWindowActive(false);AbilityWheelInput.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
                mc.setWindowActive(true);release();
                check(!(boolean)field(AbilityWheelInput.class,"down").get(null),"focus loss cancels gesture");
                press(false);release();
                net.parkabird.changedsynergy.ChangedSynergyClientConfig.CLIENT.reducedUiMotion.set(true);
                var screen=new Paged(new AbilityRadialMenu(122,mc.player.getInventory()),mc);mc.setScreen(screen);stage=15;frames=0;
            }
            if(stage==15&&mc.screen instanceof AbilityRadialScreen screen&&++frames>12) {
                capture(mc,"ability-reduced-motion.png");check(UiMotion.reduced(),"client reduced motion setting active");
                for(var highlight:(UiSpring[])field(screen,"changedSynergy$highlights").get(screen))check(highlight.velocity(System.nanoTime())==0,"reduced motion snaps highlight brightness without a fade");
                RadialWheelAnimations.requestWheelSwitch(screen,()->{});
                var target=new PlayerRelationshipScreen(new PlayerRelationshipMenu(124,mc.player.getInventory(),java.util.List.of()),mc.player.getInventory(),Component.empty());
                mc.setScreen(target);
                check(Math.abs(RadialWheelAnimations.horizontalOffset(target)-RadialWheelAnimations.layoutOffset(target.width))<.01,"reduced motion snaps directly to the final anchor");
                checkFullFrames(target);
                for(int distance=0,previous=255;distance<=8;distance++) {
                    int alpha=RelationshipPanel.shadowAlpha(distance,1);
                check(alpha>=0&&alpha<=previous,"theme shadow fades smoothly outward");previous=alpha;
                    check(RelationshipPanel.shadowAlpha(distance,0)==0,"invisible panels leave no shadow");
                }
                for(var hud:(UiSpring[])field(AbilityHudMotion.class,"VISIBLE").get(null)) {
                    hud.snap(0,0);hud.target(1,0);
                    check(hud.value(100_000_000L)<.5,"ability HUD entrance stays readable instead of flashing into place");
                    check(hud.value(600_000_000L)>.98,"slower ability HUD still settles promptly");
                }
                RadialWheelAnimations.requestWheelSwitch(target,()->{});target.keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);
                var cancelled=new ScreenEvent.Opening(null,new AbilityRadialScreen(new AbilityRadialMenu(125,mc.player.getInventory()),mc.player.getInventory(),Component.empty()));
                AbilityWheelInput.opening(cancelled);check(cancelled.isCanceled(),"Escape suppresses a late menu response after the immediate handoff request");
                press(false);
                var fresh=new ScreenEvent.Opening(null,new AbilityRadialScreen(new AbilityRadialMenu(126,mc.player.getInventory()),mc.player.getInventory(),Component.empty()));
                AbilityWheelInput.opening(fresh);check(!fresh.isCanceled(),"a fresh physical press permits opening a wheel again");release();
                net.parkabird.changedsynergy.ChangedSynergyClientConfig.CLIENT.reducedUiMotion.set(false);
                task=CompletableFuture.runAsync(()->{
                    var owner=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());PlayerRelationshipMenu.open(owner);
                },mc.getSingleplayerServer());stage=16;frames=0;
            }
            if(stage==16&&task.isDone()&&mc.screen instanceof PlayerRelationshipScreen screen&&++frames>16) {
                task.join();nativeKey(mc,GLFW.GLFW_KEY_R,GLFW.GLFW_PRESS);agePress();
                check(AbilityWheelInput.ownsPress(),"pressing the wheel key in an existing relationship menu owns the physical hold");
                nativeKey(mc,GLFW.GLFW_KEY_W,GLFW.GLFW_PRESS);nativeKey(mc,GLFW.GLFW_KEY_LEFT_CONTROL,GLFW.GLFW_PRESS);
                System.out.println("HOLD_MOVEMENT_DIAGNOSTIC up="+mc.options.keyUp.isDown()+" sprint="+mc.options.keySprint.isDown()
                        +" allow="+AbilityWheelInput.allowsMovement(mc.options.keyUp)+" down="+AbilityWheelInput.ownsPress()
                        +" focus="+mc.isWindowActive()+" keys="+java.util.Arrays.toString((boolean[])field(AbilityWheelInput.class,"movementDown").get(null)));
                check(mc.options.keyUp.isDown()&&mc.options.keySprint.isDown(),"movement and sprint pass through a held relationship wheel");
                mc.options.toggleCrouch().set(true);
                nativeKey(mc,GLFW.GLFW_KEY_LEFT_SHIFT,GLFW.GLFW_PRESS);
                nativeKey(mc,GLFW.GLFW_KEY_LEFT_SHIFT,GLFW.GLFW_REPEAT);
                check(mc.options.keyShift.isDown(),"repeat cannot repeatedly toggle crouch in hold mode");
                nativeKey(mc,GLFW.GLFW_KEY_LEFT_SHIFT,GLFW.GLFW_RELEASE);
                check(mc.options.keyShift.isDown(),"toggle crouch survives release");
                nativeKey(mc,GLFW.GLFW_KEY_LEFT_SHIFT,GLFW.GLFW_PRESS);nativeKey(mc,GLFW.GLFW_KEY_LEFT_SHIFT,GLFW.GLFW_RELEASE);
                check(!mc.options.keyShift.isDown(),"next crouch press toggles off normally");mc.options.toggleCrouch().set(false);
                heldScreen=screen;movementStart=mc.player.position();
                for(int i=0;i<5;i++)polledWheelDown();stage=17;frames=0;
            }
            if(stage==17&&++frames>20) {
                check(mc.screen==heldScreen,"polled held key cannot reopen the relationship menu");
                check(mc.player.position().distanceToSqr(movementStart)>.2,"the player actually moves while holding the wheel");
                check(mc.player.isSprinting(),"vanilla sprint remains active while the wheel is held");
                transferSpring=((RadialWheelAnimationAccess)(Object)mc.screen).changedSynergy$getHorizontalSpring();
                PlayerRelationshipClientEvents.onMouseScrolled(new ScreenEvent.MouseScrolled.Pre(mc.screen,0,0,1));stage=18;frames=0;
            }
            if(stage==18&&mc.screen instanceof AbilityRadialScreen screen&&++frames>18) {
                check(((RadialWheelAnimationAccess)(Object)screen).changedSynergy$getHorizontalSpring()==transferSpring,"holding through relationship-to-ability handoff preserves the same spring");
                check(AbilityWheelInput.ownsPress()&&mc.options.keyUp.isDown()&&mc.options.keySprint.isDown(),"movement survives the server container handoff");
                check(!AbilityWheelInput.holding(screen),"returning from relationships does not arm release-to-select without a fresh press");
                heldScreen=screen;
                for(int i=0;i<5;i++){nativeKey(mc,GLFW.GLFW_KEY_R,GLFW.GLFW_REPEAT);polledWheelDown();}
                stage=19;frames=0;
            }
            if(stage==19&&++frames>20) {
                check(mc.screen==heldScreen,"repeat and mouse recapture cannot make paired wheels oscillate");
                check(RadialWheelAnimations.horizontalOffset(mc.screen)==0,"wheel finishes at an exact centered pixel");
                PlayerRelationshipClientEvents.onMouseScrolled(new ScreenEvent.MouseScrolled.Pre(mc.screen,0,0,1));stage=20;frames=0;
            }
            if(stage==20&&mc.screen instanceof PlayerRelationshipScreen screen&&++frames>18) {
                check(AbilityWheelInput.ownsPress(),"the same physical hold can scroll back to relationships again");
                nativeKey(mc,GLFW.GLFW_KEY_R,GLFW.GLFW_RELEASE);
                nativeKey(mc,GLFW.GLFW_KEY_W,GLFW.GLFW_RELEASE);nativeKey(mc,GLFW.GLFW_KEY_LEFT_CONTROL,GLFW.GLFW_RELEASE);
                check(mc.screen==screen&&!AbilityWheelInput.allowsMovement(mc.options.keyUp),"release leaves relationship wheel clickable and ends movement passthrough");
                mc.player.input.tick(false,.3F);check(mc.player.input.forwardImpulse==0,"sticky relationship menus do not retain movement");
                check(Math.abs(RelationshipPanel.breathing(0)-RelationshipPanel.breathing(2_000_000_000L))>.25,"shadow breathing stays visible within the existing radius");
                net.parkabird.changedsynergy.ChangedSynergyClientConfig.CLIENT.reducedUiMotion.set(true);
                check(RelationshipPanel.breathing(2_000_000_000L)==1,"reduced motion disables shadow breathing");
                finished=true;System.out.println("UI_SPRING_CLIENT_REGRESSION_PASS: "+checks+" checks including held round-trip switches, polled reopen suppression, real movement/sprint, toggle crouch, seat exit, native selection/server sync and fixed panel layout; "+directory(mc));mc.stop();
            }
        } catch(Throwable failure) { finished=true;System.err.println("UI_SPRING_CLIENT_REGRESSION_FAIL");failure.printStackTrace();mc.stop(); }
    }
    private static int expectedWidth;
    private static net.minecraft.client.gui.screens.Screen heldScreen;
    private static net.minecraft.world.phys.Vec3 movementStart;
    private static void nativeKey(Minecraft mc,int key,int action) { mc.keyboardHandler.keyPress(mc.getWindow().getWindow(),key,0,action,0); }
    private static void polledWheelDown()throws Exception {
        for(var method:net.minecraft.client.KeyMapping.class.getDeclaredMethods())if(method.getName().contains("noPolledReopen")) {
            method.setAccessible(true);method.invoke(null,ChangedKeyMappings.SELECT_ABILITY,true);return;
        }
        throw new AssertionError("production key polling redirect not applied");
    }
    private static void checkFullFrames(Object screen)throws Exception {
        for(var spring:(UiSpring[])field(screen,"changedSynergy$frames").get(screen))check(spring.value(System.nanoTime())>.99,"wheel frame keeps full size during handoff");
        check(((UiSpring)field(screen,"changedSynergy$center").get(screen)).value(System.nanoTime())>.99,"center preview keeps full size during handoff");
    }
    private static java.lang.reflect.Field field(Object obj,String name)throws Exception {for(Class<?> c=obj instanceof Class<?> type?type:obj.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static float frameScale(Object screen,int slot)throws Exception {
        for(Class<?> c=screen.getClass();c!=null;c=c.getSuperclass())for(var method:c.getDeclaredMethods())if(method.getName().contains("frameMotion")){
            method.setAccessible(true);var motion=method.invoke(screen,slot);var scale=motion.getClass().getDeclaredMethod("scale");scale.setAccessible(true);return (float)scale.invoke(motion);
        }
        throw new AssertionError("frame motion method missing");
    }
    private static void checkSelectedIcon(Minecraft mc,AbilityRadialScreen screen,int slot)throws Exception {
        double angle=(slot+.5)*Math.PI/4;
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            double sx=image.getWidth()/(double)screen.width,sy=image.getHeight()/(double)screen.height;
            int cx=(int)((screen.width*.5+Math.sin(angle)*96)*sx),cy=(int)((screen.height*.5-Math.cos(angle)*96)*sy),colored=0;
            for(int x=Math.max(0,cx-(int)(28*sx));x<Math.min(image.getWidth(),cx+28*sx);x++)for(int y=Math.max(0,cy-(int)(28*sy));y<Math.min(image.getHeight(),cy+28*sy);y++){
                int rgba=image.getPixelRGBA(x,y);if((rgba&255)>150&&(rgba>>>8&255)>170&&(rgba>>>16&255)<140)colored++;
            }
            check(colored>100,"scaled native 3D ability icon remains visible inside the selected frame; pixels="+colored);
        }
    }
    private static void checkFrameBlend(Minecraft mc,AbilityRadialScreen screen)throws Exception {
        var probe=new AbilityRadialScreen(screen.menu,mc.player.getInventory(),Component.empty());
        var spring=((UiSpring[])field(probe,"changedSynergy$highlights").get(probe))[0];
        var target=new com.mojang.blaze3d.pipeline.TextureTarget(64,64,true,Minecraft.ON_OSX);
        var view=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();view.pushPose();
        var projection=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix());
        try {
            view.setIdentity();view.translate(0,0,-11000);com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0,64,64,0,1000,21000),com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            for(float alpha:new float[]{1,.4F}) {
                int[] pixels=new int[3];int sample=0;
                for(float progress:new float[]{0,.5F,1}) {
                    spring.snap(progress,System.nanoTime());target.setClearColor(.1F,.15F,.2F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
                    var graphics=new net.minecraft.client.gui.GuiGraphics(mc,mc.renderBuffers().bufferSource());graphics.setColor(.8F,.6F,.4F,alpha);
                    RadialWheelAnimations.drawFrame(probe,graphics,net.ltxprogrammer.changed.Changed.modResource("textures/gui/radial/goo_selected/0.png"),0,0,0,0,64,64,64,64);graphics.flush();
                    try(var image=Screenshot.takeScreenshot(target)){pixels[sample++]=image.getPixelRGBA(32,32);}
                }
                for(int shift:new int[]{0,8,16})check(Math.abs(2*(pixels[1]>>>shift&255)-(pixels[0]>>>shift&255)-(pixels[2]>>>shift&255))<=6,"frame midpoint blends painted brightness while preserving overall opacity "+alpha);
                check((pixels[2]&255)>(pixels[0]&255)+10,"selected frame reaches its original brighter artwork");
                for(String style:new String[]{"goo","organic"})for(int slot=0;slot<8;slot++){
                    int[][] rendered=new int[2][4096];
                    for(int mode=0;mode<2;mode++){
                        ((UiSpring[])field(probe,"changedSynergy$highlights").get(probe))[slot].snap(0,System.nanoTime());
                        target.setClearColor(.1F,.15F,.2F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
                        var graphics=new net.minecraft.client.gui.GuiGraphics(mc,mc.renderBuffers().bufferSource());graphics.setColor(.8F,.6F,.4F,alpha);
                        if(mode==0)RadialWheelAnimations.drawFrame(probe,graphics,net.ltxprogrammer.changed.Changed.modResource("textures/gui/radial/"+style+"_selected/"+slot+".png"),0,0,0,0,64,64,64,64);
                        else graphics.blit(net.ltxprogrammer.changed.Changed.modResource("textures/gui/radial/"+style+"/"+slot+".png"),0,0,0,0,64,64,64,64);
                        graphics.flush();try(var image=Screenshot.takeScreenshot(target)){for(int y=0;y<64;y++)for(int x=0;x<64;x++)rendered[mode][y*64+x]=image.getPixelRGBA(x,y);}
                    }
                    check(java.util.Arrays.equals(rendered[0],rendered[1]),"unselected "+style+" frame "+slot+" preserves every original pixel at opacity "+alpha);
                }
            }
        }finally{
            view.popPose();com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(projection,com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);
        }
    }
    private static void press(boolean open) { AbilityWheelInput.key(new InputEvent.Key(GLFW.GLFW_KEY_R,0,GLFW.GLFW_PRESS,0));if(open)ChangedKeyMappings.SELECT_ABILITY.setDown(true); }
    private static void release() { AbilityWheelInput.key(new InputEvent.Key(GLFW.GLFW_KEY_R,0,GLFW.GLFW_RELEASE,0));ChangedKeyMappings.SELECT_ABILITY.setDown(false); }
    private static void agePress()throws Exception {var f=AbilityWheelInput.class.getDeclaredField("pressedAt");f.setAccessible(true);f.setLong(null,System.nanoTime()-200_000_000L);}
    private static void point(Minecraft mc,AbilityRadialScreen screen,int slot,int radius)throws Exception {
        double angle=(slot+.5)*Math.PI/4;
        double x=screen.width/2.0+Math.sin(angle)*radius,y=screen.height/2.0-Math.cos(angle)*radius;
        var m=mc.mouseHandler.getClass().getDeclaredField("f_91507_");m.setAccessible(true);m.setDouble(mc.mouseHandler,x*mc.getWindow().getScreenWidth()/screen.width);
        m=mc.mouseHandler.getClass().getDeclaredField("f_91508_");m.setAccessible(true);m.setDouble(mc.mouseHandler,y*mc.getWindow().getScreenHeight()/screen.height);
    }
    private static int pointerX(Minecraft mc){return (int)(mc.mouseHandler.xpos()*mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth());}
    private static int pointerY(Minecraft mc){return (int)(mc.mouseHandler.ypos()*mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight());}
    private static final class Paged extends AbilityRadialScreen {
        Paged(AbilityRadialMenu menu,Minecraft mc){super(menu,mc.player.getInventory(),Component.literal("UI input fixture"));}
        @Override public int getCount(){return 12;}
        @Override public java.util.List<Component> tooltipsFor(int section){return java.util.List.of(Component.literal("Fixture "+section));}
        @Override public void renderSectionForeground(net.minecraft.client.gui.GuiGraphics g,int section,double x,double y,float partial,int mx,int my,float r,float gr,float b,float a){super.renderSectionForeground(g,section%abilities.size(),x,y,partial,mx,my,r,gr,b,a);}
    }
    private static Path directory(Minecraft mc)throws Exception {
        var path=mc.gameDirectory.toPath().getParent().resolve("ui-spring-visuals");Files.createDirectories(path);return path;
    }
    private static void capture(Minecraft mc,String name)throws Exception {
        try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(directory(mc).resolve(name));}
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
}
