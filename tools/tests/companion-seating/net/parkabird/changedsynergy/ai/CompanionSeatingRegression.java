package net.parkabird.changedsynergy.ai;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.compat.CompanionSeats;
import net.parkabird.changedsynergy.network.CompanionSeatInputPacket;
import net.parkabird.changedsynergy.network.CompanionSeatStatePacket;

@Mod.EventBusSubscriber(modid="changed_synergy")
public final class CompanionSeatingRegression {
    private static int checks;
    private static final int Y = 160;
    private static ServerLevel level;
    private static ServerPlayer owner;
    private static final List<Entity> spawned = new ArrayList<>();

    @SubscribeEvent public static void run(ServerStartedEvent event) {
        try {
            level = event.getServer().overworld();
            ChangedSynergyConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
            level.getGameRules().getRule(net.parkabird.changedsynergy.init.ChangedSynergyGameRules.FRIENDSHIP_SYSTEM).set(true,event.getServer());
            owner = player("SeatOwner");
            for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)for(int y=Y-1;y<=Y+4;y++)
                level.setBlock(new BlockPos(x,y,z),(y==Y-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
            network(); permission(); vanilla(); nativeSeat(); racing(); generic(); superb(); interrupted(); exitSafety(); navigation();
            System.out.println("COMMUNITY_REGRESSION_PASS: "+checks+" companion seating, real passenger admission, permissions, queue, safe exit and numbered-seat contract checks");
        } catch(Throwable failure) { System.err.println("COMPANION_SEATING_REGRESSION_FAIL"); failure.printStackTrace(); }
        finally { cleanup(); event.getServer().halt(false); }
    }

    @SuppressWarnings("unchecked") private static ServerPlayer player(String name) throws Exception {
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),name));
        player.connection=new ServerGamePacketListenerImpl(level.getServer(),new Connection(PacketFlow.SERVERBOUND),player);
        var players=(Map<UUID,ServerPlayer>)ObfuscationReflectionHelper.findField(net.minecraft.server.players.PlayerList.class,"f_11197_").get(level.getServer().getPlayerList());
        players.put(player.getUUID(),player);
        level.addNewPlayer(player); player.moveTo(.5,Y,.5); player.setOnGround(true); spawned.add(player); return player;
    }
    private static <T extends Entity>T add(T entity) { entity.moveTo(3.5,Y,3.5); level.addFreshEntity(entity); spawned.add(entity); return entity; }
    private static ChangedEntity friend(double x) {
        var mob=add(ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level)); mob.moveTo(x,Y,2.5); mob.setOnGround(true);
        CreaturePersonality.establishRelationship(mob,owner); CreaturePersonality.setFamiliarity(mob,owner,20);
        CreaturePersonality.setSocialFollowing(mob,owner,true); return mob;
    }
    private static void cleanup() {
        for(var entity:List.copyOf(spawned)) { if(entity instanceof ChangedEntity mob)CompanionSeatingService.finish(mob,false,null); entity.stopRiding(); entity.discard(); }
        spawned.clear();
    }
    private static void reset() {
        for(var entity:List.copyOf(spawned))if(entity!=owner){if(entity instanceof ChangedEntity mob)CompanionSeatingService.finish(mob,false,null);entity.stopRiding();entity.discard();spawned.remove(entity);}
        owner.stopRiding();owner.moveTo(.5,Y,.5);ChangedSynergyConfig.COMMON.companionSeating.set(true);ChangedSynergyConfig.COMMON.companionRallyBoarding.set(true);
    }
    private static void aim(Vec3 target) {
        Vec3 diff=target.subtract(owner.getEyePosition());
        owner.setYRot((float)Math.toDegrees(Math.atan2(-diff.x,diff.z)));
        owner.setYHeadRot(owner.getYRot());
        owner.setXRot((float)-Math.toDegrees(Math.atan2(diff.y,Math.hypot(diff.x,diff.z))));
    }
    @SuppressWarnings("unchecked") private static int token() throws Exception {
        var field=CompanionSeatingService.class.getDeclaredField("SELECTIONS");field.setAccessible(true);
        Object selection=((Map<UUID,?>)field.get(null)).get(owner.getUUID());
        check(selection!=null,"server created a real seat-selection session");
        var method=selection.getClass().getDeclaredMethod("token");method.setAccessible(true);return (int)method.invoke(selection);
    }
    private static void select(ChangedEntity mob,Entity vehicle) throws Exception {
        CompanionSeatingService.command(owner,mob,"social_seat");int token=token();aim(vehicle.getBoundingBox().getCenter());
        CompanionSeatingService.select(owner,token,vehicle.getId(),null,false);
        if(!CompanionSeatingService.pending(mob)) {
            var body=net.parkabird.changedsynergy.compat.TrueTransfurCompat.interactionBody(owner);
            System.out.println("SEAT_SELECTION_DIAGNOSTIC: target="+vehicle.getClass()+" supported="+CompanionSeats.supported(vehicle,false)
                    +" resolved="+(level.getEntity(vehicle.getId())==vehicle)+" command="+CompanionSeatingService.action(mob,owner)
                    +" body="+body.getClass()+" eye="+body.getEyePosition()+" view="+body.getViewVector(1)
                    +" box="+vehicle.getBoundingBox()+" ray="+vehicle.getBoundingBox().inflate(.25).clip(body.getEyePosition(),body.getEyePosition().add(body.getViewVector(1).scale(8))));
        }
    }
    private static void network() {
        var input=new CompanionSeatInputPacket(17,-1,new BlockPos(2,3,4),false);var buf=new FriendlyByteBuf(Unpooled.buffer());
        CompanionSeatInputPacket.encode(input,buf);check(CompanionSeatInputPacket.decode(buf).equals(input),"selection packet roundtrip");buf.release();
        var state=new CompanionSeatStatePacket(17,30,Component.literal("SeatFriend"));buf=new FriendlyByteBuf(Unpooled.buffer());
        CompanionSeatStatePacket.encode(state,buf);check(CompanionSeatStatePacket.decode(buf).equals(state),"selection-state packet roundtrip");buf.release();
    }
    private static void permission() throws Exception {
        var mob=friend(2);check(CompanionSeatingService.canCommand(mob,owner),"familiar friend accepts commands");
        CreaturePersonality.setFamiliarity(mob,owner,0);check(!CompanionSeatingService.canCommand(mob,owner),"mere acquaintance has no seat command permission");
        LatexSocialMemory.addBond(mob,owner);check(CompanionSeatingService.canCommand(mob,owner),"bond permission is independent of familiarity");
        var stranger=player("OtherOwner");check(!CompanionSeatingService.canCommand(mob,stranger),"another player cannot command this bond");
        mob.setNoAi(true);check(!CompanionSeatingService.canCommand(mob,owner),"no-AI creatures are excluded");reset();
    }
    private static void vanilla() throws Exception {
        for(int kind=0;kind<3;kind++) {
            Entity vehicle=add(kind==0?new Boat(EntityType.BOAT,level):kind==1?new ChestBoat(EntityType.CHEST_BOAT,level):new Minecart(EntityType.MINECART,level));
            var mob=friend(2.5);int affection=CreaturePersonality.familiarity(mob,owner);select(mob,vehicle);
            check(CompanionSeatingService.pending(mob),"vanilla seat order created");CompanionSeatingService.walk(mob);
            check(mob.getVehicle()==vehicle&&!CompanionSeatingService.pending(mob),"normal vanilla boarding succeeds");
            check(CreaturePersonality.familiarity(mob,owner)==affection&&CreaturePersonality.isSocialFollowing(mob,owner),"seat order preserves relationship and follow preference");
            check(CompanionSeatingService.action(mob,owner).equals("social_disembark"),"wheel changes to disembark");
            check(!new SocialFollowGoal(mob).canUse(),"following does not immediately eject passenger");
            var saved=mob.getPersistentData().copy();mob.getPersistentData().merge(saved);
            check(saved.contains("ChangedSynergyCommandSeat"),"successful seat ownership is serializable");
            if(kind==0) {
                var tree=new net.minecraft.nbt.CompoundTag();check(vehicle.save(tree),"vehicle/passenger tree saves normally");
                Entity restored=EntityType.loadEntityRecursive(tree,level,entity->entity);
                check(restored!=null&&restored.getPassengers().size()==1,"vehicle and its passenger deserialize together");
                var rider=(ChangedEntity)restored.getFirstPassenger();
                check(rider.getPersistentData().getCompound("ChangedSynergyCommandSeat").getUUID("Vehicle").equals(restored.getUUID())
                        &&!CompanionSeatingService.pending(rider)&&CompanionSeatingService.canCommand(rider,owner),"reloaded rider retains valid seat ownership without resurrecting a boarding order");
                rider.stopRiding();rider.discard();restored.discard();
            }
            ChangedSynergyConfig.COMMON.companionSeating.set(false);CompanionSeatingService.disembark(mob,owner);
            check(!mob.isPassenger(),"turning off new boarding does not prevent safe exit");reset();
        }
        var chest=add(new ChestBoat(EntityType.CHEST_BOAT,level));owner.startRiding(chest);
        check(!CompanionSeatingService.rallyAvailable(owner),"chest boat has no fake second seat");reset();
        check(!CompanionSeats.supported(add(EntityType.CHEST_MINECART.create(level)),false),"storage minecart is not a passenger seat");reset();
    }
    private static void nativeSeat()throws Exception {
        var pos=new BlockPos(3,Y,3);
        level.setBlock(pos,net.ltxprogrammer.changed.init.ChangedBlocks.PILLOWS.get(net.minecraft.world.item.DyeColor.WHITE).get().defaultBlockState(),3);
        var holder=(net.ltxprogrammer.changed.block.entity.PillowBlockEntity)level.getBlockEntity(pos);
        if(holder.getEntityHolder()==null)net.ltxprogrammer.changed.entity.SeatEntity.createFor(level,level.getBlockState(pos),pos,false);
        check(CompanionSeats.blockAvailable(level,pos),"native pillow uses its real occupancy state");
        var mob=friend(2.5);CompanionSeatingService.command(owner,mob,"social_seat");int nonce=token();aim(Vec3.atBottomCenterOf(pos).add(0,.1,0));
        CompanionSeatingService.select(owner,nonce,-1,pos,false);check(CompanionSeatingService.pending(mob),"real block ray selects the pillow");
        CompanionSeatingService.walk(mob);check(mob.getVehicle() instanceof net.ltxprogrammer.changed.entity.SeatEntity,"native pillow mounts through its own block entity");
        check(!CompanionSeats.blockAvailable(level,pos),"native occupied seat cannot be duplicated");
        var seat=mob.getVehicle();seat.moveTo(3.5,Y+50,3.5);mob.moveTo(seat.position());
        CompanionSeatingService.newCommand(mob,owner);CreaturePersonality.setSocialFollowing(mob,owner,true);
        check(mob.getVehicle()==seat,"friend follow cleanup cannot eject a commanded seat without a safe floor");
        LatexSocialMemory.addBond(mob,owner);LatexSocialMemory.setFollowingOwner(mob,true);
        check(mob.getVehicle()==seat,"bond follow cleanup cannot bypass safe disembarking");
        seat.moveTo(3.5,Y,3.5);mob.moveTo(seat.position());
        CompanionSeatingService.disembark(mob,owner);check(!mob.isPassenger(),"native block seat permits safe exit");reset();level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
    }
    private static void racing() throws Exception {
        var boat=add(new Boat(EntityType.BOAT,level));var first=friend(2.5);var second=friend(2.7);var third=friend(2.9);
        select(first,boat);select(second,boat);select(third,boat);
        check(CompanionSeatingService.pending(first)&&CompanionSeatingService.pending(second)&&!CompanionSeatingService.pending(third),"two boat slots cannot be over-reserved");
        var visitor=player("RacingVisitor");owner.startRiding(boat);visitor.startRiding(boat);CompanionSeatingService.walk(first);CompanionSeatingService.walk(second);
        check(boat.getPassengers().containsAll(List.of(owner,visitor))&&!first.isPassenger()&&!second.isPassenger(),"late player boarding never ejects an existing passenger");reset();
    }
    private static final class DualHorse extends Horse {
        final boolean npcAllowed;
        DualHorse(boolean allow){super(EntityType.HORSE,level);npcAllowed=allow;setTamed(true);}
        @Override protected boolean canAddPassenger(Entity entity){return getPassengers().size()<2&&(npcAllowed||entity instanceof ServerPlayer);}
    }
    private static void generic() {
        for(boolean allow:new boolean[]{true,false}) {
            var horse=add(new DualHorse(allow));check(!CompanionSeats.supported(horse,false)&&CompanionSeats.supported(horse,true),"unknown horse uses rider-authorized fallback, not arbitrary targeting");
            owner.startRiding(horse);var first=friend(4.5);var second=friend(5.2);
            check(CompanionSeatingService.rallyAvailable(owner),"unknown capacity still exposes rally action");
            CompanionSeatingService.rally(owner);CompanionSeatingService.walk(second);
            check(!second.isPassenger(),"generic queue waits for the earlier admission");
            CompanionSeatingService.walk(first);CompanionSeatingService.walk(second);
            check(first.isPassenger()==allow&&!second.isPassenger()&&horse.hasPassenger(owner),"fallback respects both real capacity and NPC restrictions");reset();
        }
    }
    private static void superb() {
        var vehicle=add(new com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity(level));owner.startRiding(vehicle);vehicle.changeSeat(owner,3);
        check(CompanionSeats.capacity(vehicle)==4&&CompanionSeats.firstFree(vehicle,Set.of())==1,"numbered-seat bridge preserves an empty driver slot");
        var first=friend(4.5);var second=friend(5.2);var third=friend(5.8);CompanionSeatingService.rally(owner);
        CompanionSeatingService.walk(first);CompanionSeatingService.walk(second);CompanionSeatingService.walk(third);
        check(vehicle.getOrderedPassengers().get(0)==null&&vehicle.getOrderedPassengers().get(1)==first&&vehicle.getOrderedPassengers().get(2)==second
                &&vehicle.getOrderedPassengers().get(3)==owner&&!third.isPassenger(),"non-contiguous numbered seats allocated without stealing driver or player positions");
        vehicle.locked=true;check(!CompanionSeats.usable(vehicle),"locked vehicle refuses boarding");vehicle.locked=false;vehicle.wreck=true;
        check(!CompanionSeats.usable(vehicle),"wreck refuses boarding");reset();
    }
    private static void interrupted() throws Exception {
        var boat=add(new Boat(EntityType.BOAT,level));var mob=friend(2.5);select(mob,boat);var enemy=add(EntityType.ZOMBIE.create(level));mob.setTarget(enemy);
        check(mob.getTarget()==enemy,"combat interrupt uses an actual hostile target, not a protected friend");CompanionSeatingService.walk(mob);
        check(!CompanionSeatingService.pending(mob)&&!mob.isPassenger(),"combat interrupts pending boarding");mob.setTarget(null);
        select(mob,boat);ChangedSynergyConfig.COMMON.companionSeating.set(false);CompanionSeatingService.walk(mob);
        check(!CompanionSeatingService.pending(mob),"disabling boarding cancels pending orders");ChangedSynergyConfig.COMMON.companionSeating.set(true);
        select(mob,boat);CreaturePersonality.setSocialFollowing(mob,owner,false);CompanionSeatingService.walk(mob);
        check(!CompanionSeatingService.pending(mob),"new follow/wait command cancels boarding");CreaturePersonality.setSocialFollowing(mob,owner,true);
        select(mob,boat);boat.discard();CompanionSeatingService.walk(mob);check(!CompanionSeatingService.pending(mob),"removed vehicle releases the reservation");reset();
        boat=add(new Boat(EntityType.BOAT,level));mob=friend(2.5);CompanionSeatingService.command(owner,mob,"social_seat");int nonce=token();
        aim(boat.getBoundingBox().getCenter());CompanionSeatingService.select(owner,nonce+1,boat.getId(),null,false);
        check(!CompanionSeatingService.pending(mob),"stale or forged session cannot issue an order");
        owner.setYRot(180);owner.setXRot(0);CompanionSeatingService.select(owner,nonce,boat.getId(),null,false);
        check(!CompanionSeatingService.pending(mob),"server rejects a target outside the actual view ray");
        CompanionSeatingService.select(owner,nonce,-1,null,true);reset();
    }
    private static void exitSafety() throws Exception {
        var boat=add(new Boat(EntityType.BOAT,level));var mob=friend(2.5);select(mob,boat);CompanionSeatingService.walk(mob);
        boat.moveTo(3.5,Y+50,3.5);mob.moveTo(boat.position());CompanionSeatingService.disembark(mob,owner);
        check(mob.isPassenger(),"no forced ejection from the air");
        boat.moveTo(3.5,Y,3.5);mob.moveTo(boat.position());boat.setDeltaMovement(1,0,0);CompanionSeatingService.disembark(mob,owner);
        check(mob.isPassenger(),"moving vehicle requires a safe stop");boat.setDeltaMovement(Vec3.ZERO);CompanionSeatingService.disembark(mob,owner);
        check(!mob.isPassenger(),"safe exit resumes after vehicle stops");reset();
    }
    private static final class CountingNavigation extends net.minecraft.world.entity.ai.navigation.GroundPathNavigation {
        int calls;
        CountingNavigation(ChangedEntity mob){super(mob,CompanionSeatingRegression.level);}
        @Override public net.minecraft.world.level.pathfinder.Path createPath(BlockPos pos,int accuracy){calls++;return super.createPath(pos,accuracy);}
    }
    private static void navigation()throws Exception {
        long original=level.getGameTime();
        try {
            var boat=add(new Boat(EntityType.BOAT,level));var mob=friend(-7.5);select(mob,boat);
            for(int tick=0;tick<400&&CompanionSeatingService.pending(mob);tick++) {
                level.getServer().getWorldData().overworldData().setGameTime(level.getGameTime()+1);mob.tick();
            }
            check(mob.getVehicle()==boat,"real goal walks a distant companion to the boat and boards");reset();
            boat=add(new Boat(EntityType.BOAT,level));mob=friend(-6.5);mob.moveTo(-6.5,Y,-6.5);
            var nav=new CountingNavigation(mob);ObfuscationReflectionHelper.setPrivateValue(net.minecraft.world.entity.Mob.class,mob,nav,"f_21344_");
            for(var pos:BlockPos.betweenClosed(-8,Y,-8,-6,Y+3,-6))if(pos.getX()!=-7||pos.getZ()!=-7)level.setBlock(pos,Blocks.OBSIDIAN.defaultBlockState(),3);
            select(mob,boat);
            for(int tick=0;tick<400&&CompanionSeatingService.pending(mob);tick++) {
                level.getServer().getWorldData().overworldData().setGameTime(level.getGameTime()+1);mob.tick();
            }
            check(!mob.isPassenger()&&!CompanionSeatingService.pending(mob),"real enclosed companion safely releases an unreachable boarding order");
            check(nav.calls<=7,"unreachable boarding has bounded pathfinding: "+nav.calls);
            reset();
        } finally {level.getServer().getWorldData().overworldData().setGameTime(original);}
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
}
