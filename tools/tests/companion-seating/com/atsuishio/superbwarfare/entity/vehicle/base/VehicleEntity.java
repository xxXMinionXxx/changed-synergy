package com.atsuishio.superbwarfare.entity.vehicle.base;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;

/** Contract double only: does not claim to test Superb Warfare physics or rendering. */
public final class VehicleEntity extends Horse {
    private final Map<UUID,Integer> seats = new HashMap<>();
    public boolean locked, wreck;
    public VehicleEntity(ServerLevel level) { super(EntityType.HORSE, level); setTamed(true); }
    public boolean getLocked() { return locked; }
    public boolean isWreck() { return wreck; }
    public List<Entity> getOrderedPassengers() {
        List<Entity> ordered = new ArrayList<>(Collections.nCopies(4, null));
        for (Entity passenger : getPassengers()) ordered.set(seats.getOrDefault(passenger.getUUID(), 0), passenger);
        return ordered;
    }
    @Override protected boolean canAddPassenger(Entity passenger) { return getPassengers().size() < 4; }
    @Override protected void addPassenger(Entity passenger) {
        var before = getOrderedPassengers();
        super.addPassenger(passenger);
        seats.put(passenger.getUUID(), before.indexOf(null));
    }
    public boolean changeSeat(Entity entity, int index) {
        if (index < 0 || index >= 4 || !getPassengers().contains(entity) || getOrderedPassengers().get(index) != null) return false;
        seats.put(entity.getUUID(), index); return true;
    }
}
