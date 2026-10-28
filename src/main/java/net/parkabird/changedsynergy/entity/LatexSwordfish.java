package net.parkabird.changedsynergy.entity;

import java.util.EnumSet;
import net.ltxprogrammer.changed.entity.TransfurCause;
import net.ltxprogrammer.changed.entity.beast.LatexEel;
import net.ltxprogrammer.changed.util.Color3;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.parkabird.changedsynergy.ai.BondedSuitService;
import net.parkabird.changedsynergy.compat.TrueTransfurCompat;
import net.parkabird.changedsynergy.init.ChangedSynergyAbilities;

/** An open-ocean aquatic with eel-like attributes and faster swimming. */
public final class LatexSwordfish extends LatexEel {
    public static final double SWIM_SPEED = 1.85D;
    public LatexSwordfish(EntityType<? extends LatexSwordfish> type, Level level) {
        super(type, level);
        registerAbility(ignored -> false, ChangedSynergyAbilities.SWORDFISH_RUSH.get()
                .makeInstance(net.ltxprogrammer.changed.ability.IAbstractChangedEntity.forEntity(this)));
    }

    public static boolean checkSwordfishSpawnRules(EntityType<LatexSwordfish> type,
            net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType reason,
            net.minecraft.core.BlockPos position, net.minecraft.util.RandomSource random) {
        // Keep the aquatic 1-in-40 natural rarity without the darkness check that excluded open ocean.
        return level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                && position.getY() < level.getSeaLevel() - 5
                && level.isWaterAt(position) && level.isWaterAt(position.below())
                && level.canSeeSkyFromBelowWater(position)
                && (reason != net.minecraft.world.entity.MobSpawnType.NATURAL
                    && reason != net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION || random.nextInt(40) == 0);
    }

    @Override
    public Color3 getTransfurColor(TransfurCause cause) {
        return Color3.fromInt(0x294960);
    }

    @Override protected void setAttributes(net.minecraft.world.entity.ai.attributes.AttributeMap attributes) {
        super.setAttributes(attributes);
        attributes.getInstance(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get()).setBaseValue(SWIM_SPEED);
    }

    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        var swim = getAttribute(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get());
        if (Math.abs(swim.getBaseValue() - 1.48D) < 0.00001D) swim.setBaseValue(SWIM_SPEED);
    }

    @Override public float getSwimAmount(float partialTicks) {
        float swimming = super.getSwimAmount(partialTicks);
        var rush = getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());
        return level().isClientSide && rush != null
                ? net.minecraft.util.Mth.lerp(rush.visualAmount(partialTicks), swimming, 1) : swimming;
    }

    @Override public void tick() {
        var rush = getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());
        float cooldown = rush == null ? 1 : rush.getController().coolDownPercent();
        super.tick();
        if (getUnderlyingPlayer() == null) {
            if (rush != null) {
                // The general ability goal can be paused; keep one cooldown tick per NPC tick.
                if (!TrueTransfurCompat.isInstinctBody(this) && rush.getController().isCoolingDown()
                        && rush.getController().coolDownPercent() == cooldown) rush.getController().tickCoolDown();
                rush.tickIdle();
            }
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
            @Override public boolean canUse() {
                var target = getTarget();
                var dash = getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());
                return target != null && target.isAlive() && isInWater()
                        && distanceToSqr(target) <= 144
                        && hasLineOfSight(target) && !TrueTransfurCompat.isInstinctBody(LatexSwordfish.this)
                        && BondedSuitService.getSuitController(LatexSwordfish.this) == null
                        && dash != null && !dash.getController().isCoolingDown() && dash.canUse();
            }
            @Override public void start() {
                getNavigation().stop();
                if (getTarget() == null) return;
                lookAt(getTarget(), 360, 360);
                var dash = getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());
                dash.getController().activateAbility();
                dash.getController().applyCoolDown();
                dash.getController().deactivateAbility();
            }
            @Override public boolean canContinueToUse() {
                var dash = getAbilityInstance(ChangedSynergyAbilities.SWORDFISH_RUSH.get());
                return dash != null && dash.isDashing();
            }
            @Override public void tick() { getNavigation().stop(); }
        });
    }
}
