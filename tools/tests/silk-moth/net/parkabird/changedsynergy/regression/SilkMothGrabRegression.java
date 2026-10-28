package net.parkabird.changedsynergy.regression;

import java.nio.file.Files;
import java.nio.file.Path;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel.GrabState;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.entity.HumanoidArm;
import net.parkabird.changedsynergy.client.model.LatexSilkMothModel;
import net.parkabird.changedsynergy.entity.LatexSilkMoth;
import net.parkabird.changedsynergy.init.ChangedSynergyEntities;

final class SilkMothGrabRegression {
    static void run(Minecraft mc, LatexSilkMothModel<LatexSilkMoth> model,
            LatexSilkMoth body, Path directory) throws Exception {
        var grab = body.getAbilityInstance(ChangedAbilities.GRAB_ENTITY_ABILITY.get());
        check(grab != null, "player form has the original grab ability");
        var controller = grab.getController();
        var idleBody = ChangedSynergyEntities.LATEX_SILK_MOTH.get().create(mc.level);
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.entity.EquipmentSlot.OFFHAND})
            idleBody.setItemSlot(slot, body.getItemBySlot(slot).copy());
        var idleModel = new LatexSilkMothModel<LatexSilkMoth>(LatexSilkMothModel.createBodyLayer().bakeRoot());
        controller.exchangeKeyState(false);
        controller.deactivateAbility();
        PartPose[] idle = frame(model, body, 600);
        controller.exchangeKeyState(true);
        controller.activateAbility();
        frame(model, body, 600);
        PartPose[] rising = frame(model, body, 600.5F);
        check(controller.getHoldTicks() == 0 && model.getAnimator(body).grabState == GrabState.EMPTY,
                "test exercises key-down before the first hold tick");
        for (int i = 0; i < 4; i++) check(rising[i].xRot < idle[i].xRot - .02F,
                "each arm starts rising on controller activation");
        PartPose[] raised = frame(model, body, 604);
        for (int i = 0; i < 4; i++) {
            check(raised[i].xRot < rising[i].xRot - .1F, "raising is a visible transition");
            check(Math.abs(raised[i].xRot - model.getTorso().xRot + 1.4137167F) < .0001F,
                    "ready pose matches Changed's reach pose");
        }
        PartPose[] other = frame(model, idleBody, 604);
        same(other, frame(idleModel, idleBody, 604), "another entity does not inherit the grab pose");
        same(frame(model, body, 604), raised, "returning to the grabbing entity preserves its pose");
        controller.exchangeKeyState(false);
        controller.deactivateAbility();
        PartPose[] lowering = frame(model, body, 605);
        for (int i = 0; i < 4; i++) check(lowering[i].xRot > raised[i].xRot + .03F
                && lowering[i].xRot < idle[i].xRot - .1F, "cancel lowers each arm gradually");
        same(frame(model, body, 608), frame(idleModel, idleBody, 608), "cancel restores normal movement");

        grab.grabbedEntity = idleBody;
        grab.suited = false;
        frame(model, body, 610);
        PartPose[] held = frame(model, body, 614);
        check(model.getAnimator(body).grabState == GrabState.HOLD, "real grab ability enters holding state");
        for (int i = 0; i < 4; i++) {
            float target = i % 2 == 0 ? -1.6493361F : -(float) (Math.PI * 2 / 5);
            check(Math.abs(held[i].xRot - model.getTorso().xRot - target) < .0001F,
                    "four-arm holding pose matches Changed");
        }
        for (int frame = 0; frame < 100; frame++)
            same(frame(model, body, 614), held, "repeated rendering does not accumulate arm offsets");
        grab.grabbedEntity = null;
        PartPose[] releasing = frame(model, body, 615);
        for (int i = 0; i < 4; i++) check(releasing[i].xRot > held[i].xRot + .02F,
                "releasing a held target lowers the arms");
        same(frame(model, body, 618), frame(idleModel, idleBody, 618), "release restores all four arms");
        Files.writeString(directory.resolve("silk-moth-grab-result.json"),
                "{\"pass\":true,\"fourArmRaiseAndLower\":true,\"activationBeforeHoldTick\":true,"
                + "\"originalReachAndHoldPoses\":true,\"releaseRestoresIdle\":true,"
                + "\"entityIsolation\":true,\"repeatRenderStable\":true}\n");
    }

    private static PartPose[] frame(LatexSilkMothModel<LatexSilkMoth> model, LatexSilkMoth body, float age) {
        body.tickCount = (int) age;
        model.prepareMobModel(body, 0, 0, age - (int) age);
        model.setupAnim(body, 0, 0, age, 0, 0);
        return new PartPose[]{model.getArm(HumanoidArm.LEFT).storePose(), model.getArm(HumanoidArm.RIGHT).storePose(),
                model.getOtherArm(HumanoidArm.LEFT).storePose(), model.getOtherArm(HumanoidArm.RIGHT).storePose()};
    }

    private static void same(PartPose[] actual, PartPose[] expected, String message) {
        for (int i = 0; i < actual.length; i++) {
            var a = actual[i]; var b = expected[i];
            check(Math.abs(a.x - b.x) < .0001F && Math.abs(a.y - b.y) < .0001F
                    && Math.abs(a.z - b.z) < .0001F && Math.abs(a.xRot - b.xRot) < .0001F
                    && Math.abs(a.yRot - b.yRot) < .0001F && Math.abs(a.zRot - b.zRot) < .0001F, message);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
