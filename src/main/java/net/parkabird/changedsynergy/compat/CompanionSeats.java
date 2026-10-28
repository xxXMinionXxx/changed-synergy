package net.parkabird.changedsynergy.compat;

import java.lang.reflect.Method;
import java.util.List;
import javax.annotation.Nullable;
import net.ltxprogrammer.changed.block.OfficeChair;
import net.ltxprogrammer.changed.block.Pillow;
import net.ltxprogrammer.changed.block.entity.ChairBlockEntity;
import net.ltxprogrammer.changed.block.entity.PillowBlockEntity;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Vanilla passenger fallback plus one optional, numbered-seat bridge. */
public final class CompanionSeats {
    public static final int UNKNOWN = -1;
    public static final TagKey<EntityType<?>> ALLOWED = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("changed_synergy", "companion_seats"));
    private record Superb(Method passengers, Method change, Method locked, Method wreck) {}
    private static final ClassValue<java.util.Optional<Superb>> SUPERB = new ClassValue<>() {
        @Override protected java.util.Optional<Superb> computeValue(Class<?> type) {
            Class<?> base = type;
            while (base != null && !base.getName().equals("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity"))
                base = base.getSuperclass();
            if (base == null) return java.util.Optional.empty();
            try {
                return java.util.Optional.of(new Superb(type.getMethod("getOrderedPassengers"),
                        type.getMethod("changeSeat", Entity.class, int.class),
                        type.getMethod("getLocked"), type.getMethod("isWreck")));
            } catch (ReflectiveOperationException failure) { return java.util.Optional.empty(); }
        }
    };

    private CompanionSeats() {}
    private static boolean superb(Entity entity) { return SUPERB.get(entity.getClass()).isPresent(); }

    public static boolean supported(Entity target, boolean playerAlreadyAboard) {
        if (target == null || !target.isAlive() || target instanceof Player || target instanceof ChangedEntity) return false;
        if (target instanceof AbstractHorse horse && !horse.isTamed()) return false;
        return target instanceof Boat || target instanceof Minecart || superb(target)
                || target.getType().is(ALLOWED) || playerAlreadyAboard;
    }

    public static boolean usable(Entity target) {
        var bridge = SUPERB.get(target.getClass());
        if (bridge.isEmpty()) return !hasSuperbBase(target);
        try {
            return !Boolean.TRUE.equals(bridge.get().locked().invoke(target))
                    && !Boolean.TRUE.equals(bridge.get().wreck().invoke(target));
        } catch (ReflectiveOperationException failure) { return false; }
    }

    private static boolean hasSuperbBase(Entity target) {
        for (Class<?> base = target.getClass(); base != null; base = base.getSuperclass())
            if (base.getName().equals("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity")) return true;
        return false;
    }

    @Nullable private static List<?> numbered(Entity target) {
        try { return (List<?>) SUPERB.get(target.getClass()).orElseThrow().passengers().invoke(target); }
        catch (ReflectiveOperationException | java.util.NoSuchElementException failure) { return null; }
    }

    public static int capacity(Entity target) {
        if (superb(target)) { var seats = numbered(target); return seats == null ? 0 : seats.size(); }
        if (!(target instanceof SeatEntity) && !net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).getNamespace().equals("minecraft")) return UNKNOWN;
        if (target instanceof ChestBoat || target instanceof Minecart || target instanceof SeatEntity) return 1;
        if (target instanceof Boat) return 2;
        return UNKNOWN;
    }

    public static int firstFree(Entity target, java.util.Set<Integer> reserved) {
        if (!usable(target)) return -2;
        if (superb(target)) {
            var seats = numbered(target);
            if (seats == null) return -2;
            // The driver stays available for a human on multi-seat vehicles.
            for (int i = seats.size() > 1 ? 1 : 0; i < seats.size(); i++)
                if (seats.get(i) == null && !reserved.contains(i)) return i;
            return -2;
        }
        int count = capacity(target);
        if (count < 0) return UNKNOWN;
        for (int i = target.getPassengers().size(); i < count; i++) if (!reserved.contains(i)) return i;
        return -2;
    }

    public static int freeSeats(Entity target) {
        if (!usable(target)) return 0;
        if (superb(target)) {
            var seats = numbered(target);
            if (seats == null) return 0;
            int free = 0;
            for (int i = seats.size() > 1 ? 1 : 0; i < seats.size(); i++) if (seats.get(i) == null) free++;
            return free;
        }
        int capacity = capacity(target);
        return capacity < 0 ? UNKNOWN : Math.max(0, capacity - target.getPassengers().size());
    }

    public static boolean board(ChangedEntity mob, Entity target, int seat) {
        if (!usable(target)) return false;
        if ((target instanceof Boat || target instanceof Minecart || target instanceof SeatEntity) && mob.getBbWidth() > 1.4F) return false;
        var slots = superb(target) ? numbered(target) : null;
        if (superb(target) && (slots == null || seat < 0 || seat >= slots.size() || slots.get(seat) != null)) return false;
        try {
            // Normal startRiding dispatches the vehicle's own canAddPassenger and Forge mount events.
            if (!mob.startRiding(target, false)) return false;
        } catch (RuntimeException failure) {
            // Some player-only vehicles reject a living NPC inside addPassenger instead of canAddPassenger.
            try { if (mob.getVehicle() == target) mob.stopRiding(); }
            catch (RuntimeException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            net.parkabird.changedsynergy.ChangedSynergyMod.LOGGER.debug("Vehicle rejected companion boarding: {}", target.getType(), failure);
            return false;
        }
        if (superb(target)) {
            try {
                slots = numbered(target);
                if (slots == null || slots.indexOf(mob) != seat
                        && !Boolean.TRUE.equals(SUPERB.get(target.getClass()).orElseThrow().change().invoke(target, mob, seat))) {
                    mob.stopRiding(); return false;
                }
            } catch (ReflectiveOperationException failure) { mob.stopRiding(); return false; }
        }
        return mob.getVehicle() == target;
    }

    @Nullable public static BlockPos blockSeat(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof OfficeChair && state.hasProperty(OfficeChair.HALF)
                && state.getValue(OfficeChair.HALF) == DoubleBlockHalf.UPPER) pos = pos.below();
        return level.getBlockEntity(pos) instanceof ChairBlockEntity || level.getBlockEntity(pos) instanceof PillowBlockEntity ? pos.immutable() : null;
    }

    public static boolean blockAvailable(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        if (level.getBlockEntity(pos) instanceof ChairBlockEntity chair) return chair.getSeatedEntity() == null;
        return level.getBlockEntity(pos) instanceof PillowBlockEntity pillow && pillow.getSeatedEntity() == null
                && !level.getBlockState(pos).getValue(Pillow.OCCUPIED);
    }

    public static boolean boardBlock(ChangedEntity mob, BlockPos pos) {
        if (!blockAvailable(mob.level(), pos) || mob.getBbWidth() > 1.4F) return false;
        if (mob.level().getBlockEntity(pos) instanceof ChairBlockEntity chair) return chair.sitEntity(mob) && mob.isPassenger();
        return mob.level().getBlockEntity(pos) instanceof PillowBlockEntity pillow && pillow.sitEntity(mob) && mob.isPassenger();
    }
}
