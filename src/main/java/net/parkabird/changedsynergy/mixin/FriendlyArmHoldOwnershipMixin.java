package net.parkabird.changedsynergy.mixin;

import net.ltxprogrammer.changed.ability.GrabEntityAbilityInstance;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.beast.AbstractDarkLatexEntity;
import net.parkabird.changedsynergy.ai.LatexSocialMemory;
import net.parkabird.changedsynergy.network.FriendlySocialHugState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Native pets normally infer a suit from any hold of their owner. */
@Mixin(value = AbstractDarkLatexEntity.class, remap = false)
public abstract class FriendlyArmHoldOwnershipMixin {
    @Unique
    private static boolean changedSynergy$isArmHold(GrabEntityAbilityInstance hold) {
        if (hold.grabbedEntity == null || !(hold.entity.getEntity() instanceof ChangedEntity carrier)) return false;
        return carrier.level().isClientSide
                ? FriendlySocialHugState.isActive(carrier.getId(), hold.grabbedEntity.getId())
                : LatexSocialMemory.isFriendlyArmHoldActive(carrier, hold.grabbedEntity);
    }

    @Redirect(method = {"tick", "m_8119_"}, at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/ltxprogrammer/changed/ability/GrabEntityAbilityInstance;suited:Z", remap = false))
    private void changedSynergy$keepArmHold(GrabEntityAbilityInstance hold, boolean suited) {
        hold.suited = suited && !changedSynergy$isArmHold(hold);
    }

    @Redirect(method = {"tick", "m_8119_"}, at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/ltxprogrammer/changed/ability/GrabEntityAbilityInstance;grabbedHasControl:Z", remap = false))
    private void changedSynergy$keepCarrierControl(GrabEntityAbilityInstance hold, boolean control) {
        hold.grabbedHasControl = control && !changedSynergy$isArmHold(hold);
    }
}
