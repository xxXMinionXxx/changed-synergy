package net.parkabird.changedsynergy.regression;

import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.client.Minecraft;

final class TaurToggleClientRegression {
    static void run(Minecraft mc) {
        boolean oldToggle=mc.options.toggleCrouch().get(),oldKey=mc.options.keyShift.isDown();
        boolean oldInput=mc.player.input.shiftKeyDown,oldShift=mc.player.isShiftKeyDown();
        var taur=ChangedEntities.WHITE_LATEX_CENTAUR.get().create(mc.level);
        if(mc.player.isPassenger())throw new AssertionError("Toggle test requires an unmounted local player");
        try {
            mc.options.toggleCrouch().set(false);mc.options.keyShift.setDown(false);
            mc.options.toggleCrouch().set(true);mc.options.keyShift.setDown(true);
            mc.player.input.shiftKeyDown=true;mc.player.setShiftKeyDown(true);
            if(!mc.player.startRiding(taur,true))throw new AssertionError("Toggle crouch prevents mounting");
            mc.player.input.tick(false,.3F);
            if(mc.options.keyShift.isDown()||mc.player.input.shiftKeyDown||mc.player.isShiftKeyDown()||mc.player.getVehicle()!=taur)
                throw new AssertionError("Mounting must clear the old toggle latch without changing the chosen input mode");
            mc.options.keyShift.setDown(true);mc.player.input.tick(false,.3F);
            if(!mc.player.input.shiftKeyDown||!mc.options.toggleCrouch().get())throw new AssertionError("A fresh sneak press must still allow normal dismounting");
            mc.player.removeVehicle();
            mc.options.toggleCrouch().set(false);mc.options.keyShift.setDown(true);
            mc.player.startRiding(taur,true);
            if(!mc.options.keyShift.isDown())throw new AssertionError("Hold crouch must retain its ordinary behavior");
            System.out.println("TAUR_TOGGLE_CLIENT_REGRESSION_PASS: actual mounting event clears toggled crouch, accepts a fresh dismount press and preserves hold mode");
        } finally {
            mc.player.removeVehicle();taur.discard();mc.options.toggleCrouch().set(false);mc.options.keyShift.setDown(oldKey);
            mc.options.toggleCrouch().set(oldToggle);mc.player.input.shiftKeyDown=oldInput;mc.player.setShiftKeyDown(oldShift);
        }
    }
}
