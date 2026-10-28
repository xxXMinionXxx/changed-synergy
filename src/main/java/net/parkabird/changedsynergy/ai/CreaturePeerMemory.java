package net.parkabird.changedsynergy.ai;

import java.util.UUID;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

/** Small preferences, carried by the existing morph-continuous life data. */
final class CreaturePeerMemory {
    private CreaturePeerMemory() {}

    static CompoundTag data(ChangedEntity creature) {
        var persistent = creature.getPersistentData();
        if (!persistent.contains("ChangedSynergyLife", Tag.TAG_COMPOUND)) {
            persistent.put("ChangedSynergyLife", new CompoundTag());
        }
        return persistent.getCompound("ChangedSynergyLife");
    }

    static boolean knows(ChangedEntity creature, UUID other) {
        return find(creature, other) != null;
    }

    static boolean nextChases(ChangedEntity creature, UUID other) {
        CompoundTag previous = find(creature, other);
        return previous == null || !previous.contains("Chased")
                ? creature.getRandom().nextBoolean() : !previous.getBoolean("Chased");
    }

    static void remember(ChangedEntity creature, UUID other, Boolean chased) {
        CompoundTag previous = find(creature, other);
        CompoundTag life = data(creature);
        ListTag peers = life.getList("Playmates", Tag.TAG_COMPOUND);
        UUID resolved = resolve(creature, other);
        for (int i = peers.size() - 1; i >= 0; i--) {
            CompoundTag entry = peers.getCompound(i);
            if (!entry.hasUUID("Id") || resolve(creature, entry.getUUID("Id")).equals(resolved)) peers.remove(i);
        }
        while (peers.size() >= 8) peers.remove(0);
        CompoundTag entry = new CompoundTag();
        entry.putUUID("Id", resolved);
        if (chased != null) entry.putBoolean("Chased", chased);
        else if (previous != null && previous.contains("Chased")) entry.putBoolean("Chased", previous.getBoolean("Chased"));
        peers.add(entry);
        life.put("Playmates", peers);
    }

    private static CompoundTag find(ChangedEntity creature, UUID other) {
        UUID resolved = resolve(creature, other);
        ListTag peers = data(creature).getList("Playmates", Tag.TAG_COMPOUND);
        for (int i = peers.size() - 1; i >= 0; i--) {
            CompoundTag entry = peers.getCompound(i);
            if (entry.hasUUID("Id") && resolve(creature, entry.getUUID("Id")).equals(resolved)) return entry;
        }
        return null;
    }

    private static UUID resolve(ChangedEntity creature, UUID id) {
        return creature.level() instanceof ServerLevel level
                ? CreatureMorphAliasData.get(level.getServer()).resolveAlias(id) : id;
    }
}
