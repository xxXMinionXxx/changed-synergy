package net.parkabird.changedsynergy.mixin;

import java.util.List;
import java.util.function.Predicate;
import net.ltxprogrammer.changed.server.ServerTransfurVariantInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Scare predicates and sight checks may change entity sections while querying them. */
@Mixin(value = ServerTransfurVariantInstance.class, remap = false)
public abstract class PlayerScareSnapshotMixin {
    @Group(name = "scareSnapshot", min = 1, max = 1)
    @Redirect(method = "tickScare", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"), require = 0)
    private <T extends Entity> List<T> changedSynergy$scareSnapshot(Level level, Class<T> type,
            AABB bounds, Predicate<? super T> predicate) {
        List<T> nearby = level.getEntitiesOfClass(type, bounds, entity -> true);
        nearby.removeIf(entity -> entity.isRemoved() || !predicate.test(entity));
        return nearby;
    }

    @Group(name = "scareSnapshot", min = 1, max = 1)
    @Redirect(method = "tickScare", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;m_6443_(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"), require = 0)
    private <T extends Entity> List<T> changedSynergy$scareSnapshotProduction(Level level, Class<T> type,
            AABB bounds, Predicate<? super T> predicate) {
        return changedSynergy$scareSnapshot(level, type, bounds, predicate);
    }
}
