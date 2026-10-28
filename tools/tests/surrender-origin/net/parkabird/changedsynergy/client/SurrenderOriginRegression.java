package net.parkabird.changedsynergy.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedGameRules;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ability.SwordfishRushAbility.RushDamageSource;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.compat.ChangedAddonCompat;
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;
import net.parkabird.changedsynergy.network.*;
import net.parkabird.changedsynergy.origin.NativeOriginService;
import org.joml.Matrix4f;

/** A fresh private world, actual selection packets, takeover input and server death/respawn. */
@Mod.EventBusSubscriber(modid="changed_synergy",value=Dist.CLIENT)
public final class SurrenderOriginRegression {
    private static int stage,wait,checks,scenario;
    private static long started=System.nanoTime(),surrenderAt,fullCoverAt;
    private static CompletableFuture<Void> task;
    private static ChangedEntity carrier;
    private static UUID session;
    private static Object oldPlayer;
    private static boolean sawFade,coveredCaptured;
    private static TakeoverScreen surrenderWheel;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||stage==99)return;
        var mc=Minecraft.getInstance();
        try {
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
            if(System.nanoTime()-started>210_000_000_000L)throw new AssertionError("fixture timeout stage "+stage);
            if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
                check(mc.gameDirectory.getName().startsWith("author-feedback-client-"),"private world only");
                mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);mc.options.simulationDistance().set(2);
                var rules=new GameRules();rules.getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(true,null);rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("surrender",new LevelSettings("surrender",GameType.SURVIVAL,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(864132,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());stage=1;
            }
            if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;
            if(stage==1&&mc.screen instanceof NativeOriginScreen screen&&mc.player.tickCount>20){
                screen.keyPressed(256,0,0);stage=2;wait=0;
            }
            if(stage==2&&mc.screen==null&&++wait>5){
                check(mc.getConnection()!=null,"Escape keeps the connection alive");
                task=server(mc,()->{var owner=owner(mc);check(NativeOriginService.identity(owner).state.equals("human")&&!NativeOriginService.pending(owner),"mandatory Escape commits human identity");check(NativeOriginService.reopen(owner),"optional reselection opens");});stage=3;
            }
            if(stage==3&&task.isDone()&&mc.screen instanceof NativeOriginScreen screen){task.join();screen.keyPressed(256,0,0);stage=4;wait=0;}
            if(stage==4&&mc.screen==null&&++wait>5){
                task=server(mc,()->{check(NativeOriginService.identity(owner(mc)).state.equals("human"),"optional Escape preserves committed identity");armour(owner(mc));});stage=5;
            }
            if(stage==5&&task.isDone()){
                task.join();visual(mc);task=server(mc,()->begin(owner(mc),0));stage=6;wait=0;
            }
            if(stage==6&&task.isDone()&&TakeoverClientState.current()!=null){
                task.join();var state=TakeoverClientState.current();if(!state.sessionId().equals(session))return;
                check(state.surrenderAllowed(),"server authorizes surrender in the active wheel");
                var wheel=new TakeoverScreen();check(java.util.stream.IntStream.range(0,wheel.getCount()).anyMatch(i->wheel.tooltipsFor(i).get(0).getString().equals(TakeoverClientState.text("surrender").getString())),"give-up action is visible and localized");
                surrenderWheel=wheel;mc.setScreen(wheel);clickSurrender(wheel);
                check(mc.screen instanceof ConfirmScreen,"first selection opens a real confirmation dialog");
                check(!mc.screen.isPauseScreen()&&mc.screen.shouldCloseOnEsc(),"confirmation allows cancellation and never pauses the takeover");
                check(mc.screen.children().stream().filter(c->c instanceof Button b&&b.active).count()==0,"confirmation buttons reject an accidental immediate second click");
                stage=11;wait=0;
            }
            if(stage==11&&++wait>12){
                check(TakeoverClientState.current().surrenderAllowed(),"opening confirmation never starts death");
                mc.screen.keyPressed(256,0,0);check(mc.screen==surrenderWheel,"Escape cancels and restores the wheel");
                stage=12;wait=0;
            }
            if(stage==12&&++wait>4){
                task=server(mc,()->{var e=uncheckedEntry(owner(mc));check(longField(e,"surrenderConfirmUntil")<0,"cancel clears the server confirmation");});stage=14;
            }
            if(stage==14&&task.isDone()){
                task.join();clickSurrender(surrenderWheel);check(mc.screen instanceof ConfirmScreen,"reopening requires a fresh confirmation");stage=13;wait=0;
            }
            if(stage==13&&++wait>12){
                capture(mc,"surrender-confirm-"+scenario+".png");
                var button=mc.screen.children().stream().filter(c->c instanceof Button b&&b.active&&b.getMessage().getString().equals(TakeoverClientState.text("surrender_confirm").getString())).map(c->(Button)c).findFirst().orElseThrow();
                oldPlayer=mc.player;sawFade=false;coveredCaptured=false;fullCoverAt=-1;button.onPress();stage=7;wait=0;
            }
            if(stage==7){
                var state=TakeoverClientState.current();
                if(state!=null&&state.sessionId().equals(session)&&state.phase()==TakeoverStatePacket.SLEEPING){
                    check(state.outcome()==2&&!state.surrenderAllowed(),"surrender enters a nonrepeatable fatal dissolve");
                    if(state.fade()>0&&state.fade()<1)sawFade=true;
                    if(state.fade()>=1&&fullCoverAt<0){
                        fullCoverAt=mc.level.getGameTime();check(mc.player.isAlive(),"fully opaque colour arrives while the player is still alive");
                        if(scenario<2)task=server(mc,()->{String key=carrier.getPersistentData().getString("ChangedSynergyLastDialogue");check(owner(mc).isAlive()&&key.contains(scenario==0?"surrender.farewell.":"surrender.punitive_farewell."),"farewell is visible during the full-colour hold, before death");});
                    }
                    if(fullCoverAt>=0&&!coveredCaptured&&mc.level.getGameTime()-fullCoverAt>=2){captureCover(mc,state);coveredCaptured=true;}
                }
                if(++wait>300)throw new AssertionError("takeover did not resolve");
                if(scenario<2&&!mc.player.isAlive()){
                    task.join();check(sawFade&&coveredCaptured,"received progressive opacity and rendered opaque cover before death");
                    check(fullCoverAt>=0&&mc.level.getGameTime()-fullCoverAt>=50,"full colour stays visible for about three seconds before death");
                    check(TakeoverClientState.fatalCoverTint()>=0,"full colour cover persists through the native death-screen delay");
                    task=server(mc,()->verifyDeath(owner(mc)));stage=8;wait=0;
                }else if(scenario==2&&!TakeoverClientState.active()&&wait>8){
                    task=server(mc,()->{var p=owner(mc);check(p.isAlive()&&!TakeoverService.active(p)&&!p.noPhysics,"totem resolves without death or leftover control locks");check(carrier.isRemoved()&&ProcessTransfur.getPlayerTransfurVariantSafe(p).orElseThrow().getFormId().equals(carrier.getSelfVariant().getFormId()),"totem claims the original carrier body");});stage=10;wait=0;
                }
            }
            if(stage==8&&task.isDone()){
                task.join();mc.player.respawn();stage=9;wait=0;
            }
            if(stage==9&&mc.player!=oldPlayer&&mc.player.isAlive()&&++wait>5){
                check(TakeoverClientState.fatalCoverTint()<0,"respawn clears fatal colour cover");
                if(scenario==1&&!ChangedAddonCompat.isLoaded()){finish(mc);return;}
                scenario++;task=server(mc,()->begin(owner(mc),scenario));stage=6;wait=0;
            }
            if(stage==10&&task.isDone()&&++wait>5){
                task.join();check(TakeoverClientState.fatalCoverTint()<0,"totem survivor never retains fatal screen cover");finish(mc);
            }
        }catch(Throwable failure){stage=99;System.err.println("SURRENDER_ORIGIN_REGRESSION_FAIL stage="+stage+" scenario="+scenario);failure.printStackTrace();mc.stop();}
    }
    private static CompletableFuture<Void> server(Minecraft mc,Runnable run){return CompletableFuture.runAsync(run,mc.getSingleplayerServer());}
    private static ServerPlayer owner(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void clickSurrender(TakeoverScreen wheel){int index=java.util.stream.IntStream.range(0,wheel.getCount()).filter(i->wheel.tooltipsFor(i).get(0).getString().equals(TakeoverClientState.text("surrender").getString())).findFirst().orElseThrow();check(!wheel.handleClicked(index,new net.ltxprogrammer.changed.util.SingleRunnable(()->{})),"first click does not confirm a fatal action");}
    private static void capture(Minecraft mc,String name)throws Exception{try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(mc.gameDirectory.toPath().resolve(name));}}
    private static Object uncheckedEntry(ServerPlayer p){try{return active().get(p.getUUID());}catch(Exception e){throw new RuntimeException(e);}}
    private static long longField(Object o,String name){try{return field(o,name).getLong(o);}catch(Exception e){throw new RuntimeException(e);}}
    private static void captureCover(Minecraft mc,TakeoverStatePacket state)throws Exception{try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){int rgb=state.carrierColor(),expected=(rgb&255)<<16|(rgb&0xff00)|(rgb>>>16&255),actual=image.getPixelRGBA(image.getWidth()-8,image.getHeight()-8)&0xffffff;image.writeToFile(mc.gameDirectory.toPath().resolve("surrender-covered-"+scenario+".png"));check(actual==expected,"the actual HUD renders the carrier colour fully opaque outside the readable chat panel: expected="+Integer.toHexString(expected)+" actual="+Integer.toHexString(actual)+" screen="+mc.screen);}}
    @SuppressWarnings("unchecked") private static Map<UUID,Object> active()throws Exception{return (Map<UUID,Object>)field(TakeoverService.class,"ACTIVE").get(null);}
    private static void begin(ServerPlayer p,int number){
        try {
            p.setGameMode(GameType.SURVIVAL);p.setHealth(p.getMaxHealth());p.getFoodData().setFoodLevel(20);
            var config=ChangedSynergyConfig.COMMON;config.takeoverEnabled.set(true);config.takeoverPunitive.set(true);config.takeoverHostileFinale.set(true);config.creatureTransfurMethod.set(ChangedSynergyConfig.CreatureTransfurMethod.TAKEOVER);
            p.server.overworld().getGameRules().getRule(ChangedGameRules.RULE_KEEP_BRAIN).set(number==0,p.server);
            carrier=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(p.serverLevel());carrier.moveTo(p.getX()+.6,p.getY(),p.getZ());p.serverLevel().addFreshEntity(carrier);
            FactionReputation.set(carrier,p,number==0?0:-100);
            var reason=number==0?InvoluntaryTransfurNegotiation.Reason.HOST_SEEKING:InvoluntaryTransfurNegotiation.Reason.FACTION_RETALIATION;
            check(TakeoverService.authorizeOrdinary(carrier,p,reason)&&TakeoverService.beginOrdinary(carrier,p),"real authorized carrier starts takeover "+number);
            var entry=active().get(p.getUUID());session=(UUID)field(entry,"id").get(entry);
            var model=(TakeoverSession)field(entry,"session").get(entry);
            TakeoverService.handleAction(p,UUID.randomUUID(),TakeoverActionPacket.SURRENDER,0,0);check(model.getPhase()==TakeoverSession.Phase.CONTROLLED,"forged session cannot surrender");
            TakeoverService.handleAction(p,session,TakeoverActionPacket.SURRENDER,0,0);check(model.getPhase()==TakeoverSession.Phase.CONTROLLED,"a final packet without the first confirmation is rejected");
            TakeoverService.handleAction(p,session,TakeoverActionPacket.CONFIRM_SURRENDER,0,0);field(entry,"surrenderConfirmUntil").setLong(entry,p.serverLevel().getGameTime()-1);
            TakeoverService.handleAction(p,session,TakeoverActionPacket.SURRENDER,0,0);check(model.getPhase()==TakeoverSession.Phase.CONTROLLED,"an expired confirmation cannot surrender");
            field(entry,"confinement").setBoolean(entry,true);TakeoverService.handleAction(p,session,TakeoverActionPacket.SURRENDER,0,0);check(model.getPhase()==TakeoverSession.Phase.CONTROLLED,"bonded confinement rejects surrender");field(entry,"confinement").setBoolean(entry,false);
            if(number==2){
                p.getInventory().setItem(9,new ItemStack(ForgeRegistries.ITEMS.getValue(ChangedAddonCompat.TRANSFUR_TOTEM)));
                field(model,"nextBorrowAt").setLong(model,p.serverLevel().getGameTime());carrier.setOnGround(true);
                TakeoverService.handleAction(p,session,TakeoverActionPacket.REQUEST_CONTROL,0,0);
                check(model.hasPlayerControl(),"totem surrender also accepts an authenticated borrowed-control phase");
            }
            surrenderAt=p.serverLevel().getGameTime();
        }catch(Exception failure){throw new RuntimeException(failure);}
    }
    private static void verifyDeath(ServerPlayer p){
        check(!p.isAlive()&&p.serverLevel().getGameTime()-surrenderAt>=TakeoverSession.FATAL_FADE_TICKS+TakeoverSession.FATAL_HOLD_TICKS,"surrender waits for the full ten-second presentation before real death");
        String farewell=carrier.getPersistentData().getString("ChangedSynergyLastDialogue");
        check(farewell.contains(scenario==0?"surrender.farewell.":"surrender.punitive_farewell."),"farewell is already sent before death");
        check(!TakeoverService.active(p)&&!p.noPhysics&&carrier.isAlive(),"fatal release preserves original NPC and clears session/physics");
        carrier.discard();
    }
    private static void armour(ServerPlayer p){
        var cow=EntityType.COW.create(p.serverLevel());var attack=p.damageSources().playerAttack(p);
        check(new RushDamageSource(attack).getEntity()==p&&new RushDamageSource(attack).typeHolder()==attack.typeHolder(),"rush keeps native damage type and attacker");
        for(int armour:new int[]{0,20}){
            if(cow.getAttribute(Attributes.ARMOR)==null)throw new AssertionError("target armour attribute missing");
            cow.getAttribute(Attributes.ARMOR).setBaseValue(armour);cow.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8);
            cow.setHealth(cow.getMaxHealth());cow.invulnerableTime=0;float before=cow.getHealth();cow.hurt(new RushDamageSource(attack),8);float dealt=before-cow.getHealth();
            float expected=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(8,armour*.5F,8);
            try {
                var method=net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("m_21161_",net.minecraft.world.damagesource.DamageSource.class,float.class);method.setAccessible(true);
                System.out.println("RUSH_ARMOUR_DIAGNOSTIC armour="+cow.getArmorValue()+" toughness="+cow.getAttributeValue(Attributes.ARMOR_TOUGHNESS)+" before="+before+" hp="+cow.getHealth()+" ordinary="+method.invoke(cow,attack,8F)+" rush="+method.invoke(cow,new RushDamageSource(attack),8F));
            }catch(Exception failure){throw new RuntimeException(failure);}
            check(Math.abs(dealt-expected)<.001,"rush applies half effective armour with unchanged toughness; armour="+armour+" dealt="+dealt+" expected="+expected+" alive="+cow.isAlive());
            cow.setHealth(cow.getMaxHealth());cow.invulnerableTime=0;before=cow.getHealth();cow.hurt(attack,8);float ordinary=before-cow.getHealth();check(armour==0?Math.abs(ordinary-dealt)<.001:dealt>ordinary&&dealt<8,"partial armour bypass does not become a full bypass");
            cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,100,0));cow.setHealth(cow.getMaxHealth());cow.invulnerableTime=0;before=cow.getHealth();cow.hurt(new RushDamageSource(attack),8);check(before-cow.getHealth()<dealt,"resistance still reduces rush damage");cow.removeAllEffects();
        }
        cow.discard();
    }
    private static void visual(Minecraft mc)throws Exception{
        UUID id=UUID.randomUUID();var packet=new TakeoverStatePacket(id,0,3,-1,mc.player.getId(),40,40,0,false,false,true,2,0,0,0,0,.5F,"Carrier",false,0xB98764);
        var buf=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{TakeoverStatePacket.encode(packet,buf);check(TakeoverStatePacket.decode(buf).equals(packet)&&buf.readableBytes()==0,"new takeover state fields round-trip without trailing bytes");}finally{buf.release();}
        var target=new TextureTarget(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true,Minecraft.ON_OSX);var mv=RenderSystem.getModelViewStack();mv.pushPose();
        try{
            int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();mv.setIdentity();mv.translate(0,0,-11000);RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,w,h,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
            var draw=TakeoverOverlay.class.getDeclaredMethod("dissolve",GuiGraphics.class,int.class,int.class,float.class,int.class);draw.setAccessible(true);
            for(float opacity:new float[]{.5F,1}){
                target.setClearColor(.1F,.2F,.3F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);var g=new GuiGraphics(mc,mc.renderBuffers().bufferSource());draw.invoke(null,g,w,h,opacity,packet.carrierColor());g.flush();
                try(var img=net.minecraft.client.Screenshot.takeScreenshot(target)){
                    int center=img.getPixelRGBA(img.getWidth()/2,img.getHeight()/2);check(center==img.getPixelRGBA(2,2)&&center==img.getPixelRGBA(img.getWidth()-3,img.getHeight()-3),"fatal tint is uniform across the full screen");
                    if(opacity==1)check((center&0xFFFFFF)==0x6487B9,"full dissolve matches the carrier's actual theme RGB");
                    img.writeToFile(mc.gameDirectory.toPath().resolve(opacity==1?"fatal-colour-full.png":"fatal-colour-half.png"));
                }
            }
        }finally{mv.popPose();RenderSystem.applyModelViewMatrix();target.destroyBuffers();mc.getMainRenderTarget().bindWrite(true);}
    }
    private static java.lang.reflect.Field field(Object object,String name)throws Exception{for(Class<?> c=object instanceof Class<?> type?type:object.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void finish(Minecraft mc){stage=99;System.out.println("SURRENDER_ORIGIN_REGRESSION_PASS: "+checks+" checks; first-login Escape, optional cancel, native packets, partial armour, uniform tint, real surrender/death/respawn and optional totem reversal");mc.stop();}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
}
