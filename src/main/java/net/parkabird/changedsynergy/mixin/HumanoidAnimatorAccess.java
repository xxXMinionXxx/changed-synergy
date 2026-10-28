package net.parkabird.changedsynergy.mixin;

import java.util.List;
import java.util.EnumMap;
import net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = HumanoidAnimator.class, remap = false)
public interface HumanoidAnimatorAccess {
    @Accessor("animators")
    EnumMap<HumanoidAnimator.AnimateStage, List<HumanoidAnimator.Animator<?, ?>>> changedSynergy$animators();
}
