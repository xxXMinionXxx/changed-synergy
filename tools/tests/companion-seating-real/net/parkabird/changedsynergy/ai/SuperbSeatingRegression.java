package net.parkabird.changedsynergy.ai;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.compat.CompanionSeats;

/** Actual downloadable Superb Warfare 0.8.9.1, not the separate contract double. */
@Mod.EventBusSubscriber(modid="changed_synergy")
public final class SuperbSeatingRegression {
    private static int checks;
    private static net.minecraft.server.MinecraftServer ready;
    @SubscribeEvent public static void start(ServerStartedEvent event) { ready=event.getServer(); }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if(ready==null||event.phase!=net.minecraftforge.event.TickEvent.Phase.END||ready.getTickCount()<40)return;
        var server=ready;ready=null;run(server);
    }
    @SuppressWarnings("unchecked") private static void run(net.minecraft.server.MinecraftServer server) {
        var level=server.overworld();List<Entity> cleanup=new ArrayList<>();
        try {
            ChangedSynergyConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
            var players=(Map<UUID,ServerPlayer>)ObfuscationReflectionHelper.findField(net.minecraft.server.players.PlayerList.class,"f_11197_").get(server.getPlayerList());
            var owner=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"VehicleOwner"));
            owner.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),owner);
            owner.moveTo(4,160,4);players.put(owner.getUUID(),owner);level.addNewPlayer(owner);cleanup.add(owner);
            for(int x=-12;x<=16;x++)for(int z=-12;z<=16;z++)for(int y=159;y<164;y++)level.setBlock(new BlockPos(x,y,z),(y==159?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
            for(String field:List.of("M_1A_2","SPEEDBOAT")) {
                var registration=Class.forName("com.atsuishio.superbwarfare.init.ModEntities").getField(field).get(null);
                var type=(net.minecraft.world.entity.EntityType<?>)registration.getClass().getMethod("get").invoke(registration);
                var id=ForgeRegistries.ENTITY_TYPES.getKey(type);
                check(id!=null&&id.getNamespace().equals("superbwarfare"),"real registered Superb vehicle: "+id);
                Entity vehicle=type.create(level);vehicle.moveTo(4,160,4);vehicle.setOnGround(true);level.addFreshEntity(vehicle);cleanup.add(vehicle);
                check(CompanionSeats.supported(vehicle,true)&&CompanionSeats.usable(vehicle),"real optional bridge resolves: "+id);
                System.out.println("REAL_SUPERB_SEATS: "+id+" seats="+vehicle.getClass().getMethod("getOrderedPassengers").invoke(vehicle)+" capacity="+CompanionSeats.capacity(vehicle));
                check(CompanionSeats.capacity(vehicle)>1,"real vehicle has multiple seats");
                check(owner.startRiding(vehicle,false),"owner boards normally");
                var method=vehicle.getClass().getMethod("getOrderedPassengers");
                var before=new ArrayList<>((List<?>)method.invoke(vehicle));int playerIndex=before.indexOf(owner);
                List<ChangedEntity> companions=new ArrayList<>();
                int expected=Math.min(CompanionSeats.freeSeats(vehicle),2);
                for(int i=0;i<2;i++) {
                    var mob=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
                    mob.moveTo(vehicle.getBoundingBox().maxX+.4+i*.25,160,4);mob.setOnGround(true);level.addFreshEntity(mob);cleanup.add(mob);
                    CreaturePersonality.establishRelationship(mob,owner);CreaturePersonality.setFamiliarity(mob,owner,20);CreaturePersonality.setSocialFollowing(mob,owner,true);companions.add(mob);
                }
                check(CompanionSeatingService.rallyAvailable(owner),"mounted real vehicle exposes rally");
                CompanionSeatingService.rally(owner);for(var mob:companions)CompanionSeatingService.walk(mob);
                var after=(List<?>)method.invoke(vehicle);
                check(after.get(playerIndex)==owner,"real driver's seat retained");
                check(companions.stream().filter(mob->mob.getVehicle()==vehicle).count()==expected,"real seats accept exactly the expected NPC passengers: "+id);
                for(var mob:companions) { CompanionSeatingService.finish(mob,false,null);mob.stopRiding();mob.discard(); }
                vehicle.getClass().getMethod("setLocked",boolean.class).invoke(vehicle,true);
                check(!CompanionSeats.usable(vehicle),"real locked vehicle refuses new boarding");
                owner.stopRiding();vehicle.discard();
            }
            System.out.println("COMMUNITY_REGRESSION_PASS: "+checks+" real Superb Warfare 0.8.9.1 M1A2 and speedboat boarding checks");
        } catch(Throwable failure) { System.err.println("SUPERB_SEATING_REGRESSION_FAIL");failure.printStackTrace(); }
        finally { for(var entity:cleanup)entity.discard();server.halt(false); }
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
}
