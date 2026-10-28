package net.parkabird.changedsynergy.ability;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.ltxprogrammer.changed.ability.AbstractAbility;
import net.ltxprogrammer.changed.ability.AbstractAbilityInstance;
import net.ltxprogrammer.changed.ability.GrabEntityAbility;
import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.parkabird.changedsynergy.entity.LatexSwordfish;
import net.parkabird.changedsynergy.compat.TrueTransfurCompat;

/** Riptide II launch speed with one server-authoritative contact hit per target. */
public final class SwordfishRushAbility extends AbstractAbility<SwordfishRushAbility.Instance> {
    public SwordfishRushAbility() { super(Instance::new); }
    @Override public UseType getUseType(IAbstractChangedEntity entity) { return UseType.CHARGE_TIME; }
    @Override public int getChargeTime(IAbstractChangedEntity entity) { return 10; }
    @Override public int getCoolDown(IAbstractChangedEntity entity) {
        return entity.getEntity() instanceof LatexSwordfish fish && fish.getUnderlyingPlayer() == null
                && !TrueTransfurCompat.isInstinctBody(fish) ? 80 : 60;
    }
    @Override public List<Component> getAbilityDescription(IAbstractChangedEntity entity) {
        return List.of(Component.translatable("ability.changed_synergy.swordfish_rush.desc"));
    }

    public static final class Instance extends AbstractAbilityInstance {
        private int remaining, slowTicks;
        private Vec3 direction = Vec3.ZERO;
        private Vec3 previous;
        private final Set<Integer> hit = new HashSet<>();
        private float visual, previousVisual, spin, previousSpin;

        public Instance(AbstractAbility<?> ability, IAbstractChangedEntity entity) { super(ability, entity); }
        public boolean isDashing() { return remaining > 0; }
        public Vec3 direction() { return direction; }
        public float visualAmount(float partial) { return Mth.lerp(partial, previousVisual, visual); }
        public float spin(float partial) { return Mth.lerp(partial, previousSpin, spin); }
        private Player foodOwner() {
            return entity.getEntity() instanceof Player player ? player : TrueTransfurCompat.bodyOwner(entity.getChangedEntity());
        }
        @Override public boolean canUse() {
            var living = entity.getEntity();
            return entity.getChangedEntity() instanceof LatexSwordfish && living.isAlive() && !living.isSpectator()
                    && !living.isPassenger() && !living.isSleeping() && !isDashing()
                    && living.isInWater() && (foodOwner() == null || foodOwner().getAbilities().instabuild || foodOwner().getFoodData().getFoodLevel() > 0)
                    && GrabEntityAbility.getGrabberSafe(living).isEmpty()
                    && (entity.getChangedEntity().getAbilityInstance(net.ltxprogrammer.changed.init.ChangedAbilities.GRAB_ENTITY_ABILITY.get()) == null
                        || entity.getChangedEntity().getAbilityInstance(net.ltxprogrammer.changed.init.ChangedAbilities.GRAB_ENTITY_ABILITY.get()).grabbedEntity == null);
        }
        @Override public boolean canKeepUsing() { return false; }
        @Override public void tick() {}
        @Override public void stopUsing() {}

        @Override public void startUsing() {
            if (!canUse()) return;
            LivingEntity living = entity.getEntity();
            direction = living.getLookAngle().normalize();
            if (living instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != null)
                direction = mob.getTarget().getBoundingBox().getCenter().subtract(living.getBoundingBox().getCenter()).normalize();
            previous = living.position();
            remaining = 20;
            slowTicks = 0;
            hit.clear();
            living.setDeltaMovement(living.getDeltaMovement().add(direction.scale(2.25)));
            living.hurtMarked = true;
            if (!living.level().isClientSide) {
                living.level().playSound(null, living.blockPosition(), net.parkabird.changedsynergy.init.ChangedSynergySoundEvents.SWORDFISH_RUSH.get(),
                        living.getSoundSource(), 1, 1);
                Player owner = foodOwner();
                if (owner != null && !owner.getAbilities().instabuild)
                    owner.getFoodData().setFoodLevel(owner.getFoodData().getFoodLevel() - 1);
                sync();
            }
        }

        @Override public void tickIdle() {
            if (entity.getLevel().isClientSide) {
                previousVisual = visual;
                previousSpin = spin;
                visual = Mth.clamp(visual + (isDashing() ? 1F / 3 : -1F / 2), 0, 1);
                if (isDashing()) spin -= 75 * visual;
                else if (visual > 0) spin -= Mth.wrapDegrees(spin) * 0.5F;
                else spin -= Mth.wrapDegrees(spin);
            }
            if (!isDashing()) return;
            LivingEntity living = entity.getEntity();
            if (!living.isAlive() || living.isRemoved() || living.isPassenger() || living.isSleeping()
                    || !(entity.getChangedEntity() instanceof LatexSwordfish)
                    || GrabEntityAbility.getGrabberSafe(living).isPresent()) { finish(); return; }
            boolean motionEnded = false;
            if (!living.level().isClientSide) {
                // Sweep the distance travelled this tick so a fast charge cannot skip a small target.
                Vec3 start = previous == null ? living.position() : previous;
                Vec3 end = living.position();
                // ServerPlayer velocity can remain stale after a launch; use actual movement packets instead.
                slowTicks = remaining <= 18 && end.subtract(start).dot(direction) < 0.35D ? slowTicks + 1 : 0;
                motionEnded = !living.isInWater() || slowTicks >= 2;
                Vec3 bill = direction.scale(1.0D);
                var area = living.getBoundingBox().expandTowards(start.subtract(end)).expandTowards(bill).inflate(0.25);
                for (LivingEntity target : living.level().getEntitiesOfClass(LivingEntity.class, area,
                        other -> other != living && other.isAlive() && !other.isSpectator()
                                && living.canAttack(other) && !living.isAlliedTo(other) && !other.isAlliedTo(living)
                                && (!(living instanceof net.minecraft.world.entity.Mob mob) || mob.getTarget() == other))) {
                    if (living instanceof net.minecraft.server.level.ServerPlayer player
                            && target instanceof net.ltxprogrammer.changed.entity.ChangedEntity npc
                            && (net.parkabird.changedsynergy.ai.LatexSocialMemory.isPetOwner(npc, player)
                                || net.parkabird.changedsynergy.ai.CreaturePersonality.hasTrustedRelationship(npc, player)
                                    && !net.parkabird.changedsynergy.ai.LatexSocialMemory.isProvoked(npc, player))) continue;
                    Vec3 offset = new Vec3(0, living.getBbHeight() * 0.5, 0);
                    var sweptTarget = target.getBoundingBox().inflate(living.getBbWidth() * 0.5 + 0.25, living.getBbHeight() * 0.5, living.getBbWidth() * 0.5 + 0.25);
                    if (!sweptTarget.contains(start.add(offset)) && sweptTarget.clip(start.add(offset), end.add(offset).add(bill)).isEmpty()) continue;
                    Vec3 targetPoint = target.getBoundingBox().getCenter();
                    if (living.level().clip(new ClipContext(living.getBoundingBox().getCenter(), targetPoint,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, living)).getType() != HitResult.Type.MISS) continue;
                    if (hit.add(target.getId()) && target.hurt(new RushDamageSource(living instanceof Player player
                            ? living.damageSources().playerAttack(player) : living.damageSources().mobAttack(living)), 8)
                            && foodOwner() instanceof net.minecraft.server.level.ServerPlayer player)
                        net.parkabird.changedsynergy.advancement.SynergyAdvancements.grant(player,
                                net.parkabird.changedsynergy.advancement.SynergyAdvancements.SWORDFISH_RUSH);
                }
                ((ServerLevel)living.level()).sendParticles(ParticleTypes.BUBBLE, living.getX(), living.getY()+0.5,
                        living.getZ(), 3, 0.25, 0.25, 0.25, 0.02);
                previous = end;
            }
            if (--remaining == 0 || motionEnded || living.horizontalCollision || living.verticalCollision && !living.onGround()) finish();
        }

        @Override public void onRemove() {
            finish();
            visual = previousVisual = spin = previousSpin = 0;
        }
        private void finish() {
            boolean active = isDashing();
            remaining = slowTicks = 0; previous = null; hit.clear();
            if (active && !entity.getLevel().isClientSide) sync();
        }
        private void sync() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("RushTicks", remaining);
            tag.putDouble("X", direction.x); tag.putDouble("Y", direction.y); tag.putDouble("Z", direction.z);
            sendPayload(tag);
        }
        @Override public void acceptPayload(CompoundTag tag) {
            // Clients may only request normal ability activation, never damage, duration or direction.
            if (!entity.getLevel().isClientSide) return;
            remaining = Math.max(0, Math.min(20, tag.getInt("RushTicks")));
            direction = new Vec3(tag.getDouble("X"),tag.getDouble("Y"),tag.getDouble("Z")).normalize();
        }
    }

    /** Retains the native attack's attribution, scaling and tags; only its armour calculation differs. */
    public static final class RushDamageSource extends DamageSource {
        public RushDamageSource(DamageSource attack) {
            super(attack.typeHolder(), attack.getDirectEntity(), attack.getEntity());
        }
        @Override public Component getLocalizedDeathMessage(LivingEntity victim) {
            return getEntity() == null ? super.getLocalizedDeathMessage(victim)
                    : Component.translatable("death.attack.changed_synergy.swordfish_rush",
                            getEntity().getDisplayName(), victim.getDisplayName());
        }
    }
}
