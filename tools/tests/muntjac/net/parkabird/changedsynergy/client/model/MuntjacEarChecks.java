package net.parkabird.changedsynergy.client.model;

import java.nio.file.Files;
import java.nio.file.Path;
import net.ltxprogrammer.changed.entity.SpringType;
import net.minecraft.client.model.geom.ModelPart;

public final class MuntjacEarChecks {
    private static int checks;

    public static void main(String[] args) throws Exception {
        ModelPart root = LatexMuntjacModel.createBodyLayer().bakeRoot();
        ModelPart head = root.getChild("Head");
        ModelPart left = head.getChild("LeftEar"), right = head.getChild("RightEar");
        ModelPart bind = left.getChild("LeftEarPivot"), antler = head.getChild("LeftAntler");
        float bindRoll = bind.zRot, antlerRoll = antler.zRot, leftY = left.y;
        check(bindRoll > .2F, "upright splayed bind pose exported");
        check(Math.abs(root.getChild("RightLeg").x + 2.5F) < .00001F, "BB to Java X conversion");
        check(Math.abs(root.getChild("RightLeg").y - 10.5F) < .00001F, "BB to Java Y conversion");
        MuntjacEarsAnimator.poseEars(left, right, .45F, 0, -.4F);
        float pitch = left.xRot, yaw = left.yRot;
        check(Math.abs(pitch) > .02F && Math.abs(pitch) < .105F, "small visible jump tilt");
        check(yaw < 0 && yaw == right.yRot, "both ears lag in the same head-space direction");
        for (int frame = 0; frame < 5000; frame++) {
            MuntjacEarsAnimator.poseEars(left, right, .45F, 0, -.4F);
            check(left.xRot == pitch && left.yRot == yaw, "no pose accumulation");
        }
        check(bind.zRot == bindRoll && antler.zRot == antlerRoll && left.y == leftY,
                "bind angle, antlers and ear root position unchanged");
        MuntjacEarsAnimator.poseEars(left, right, 100, -100, 100);
        check(Math.abs(left.xRot) <= .10472F && Math.abs(left.yRot) <= .08727F
                && Math.abs(left.zRot) <= .03491F, "rotation limits under extreme motion");
        MuntjacEarsAnimator.poseEars(left, right, 0, 0, 0);
        check(left.xRot == 0 && left.yRot == 0 && left.zRot == 0, "exact return to rest");

        var turn = new MuntjacEarsAnimator.TurnResponse();
        turn.sample(0, 0, 1);
        float first = turn.sample(1, 45, 1);
        check(first < -.1F, "head-only turn generates inertia");
        for (int frame = 0; frame < 5000; frame++)
            check(turn.sample(1, 45, 1) == first, "physics runs once per tick and pauses with the game");
        float rebound = 0, end = 0;
        for (int tick = 2; tick < 181; tick++) {
            end = turn.sample(tick, 45, 1);
            rebound = Math.max(rebound, end);
        }
        check(rebound > .02F && Math.abs(end) < .00001F, "opposite rebound decays to rest");
        var quiet = new MuntjacEarsAnimator.TurnResponse();
        check(quiet.sample(1, 45, 1) == 0, "different entities do not share turn state");
        var wrapped = new MuntjacEarsAnimator.TurnResponse();
        wrapped.sample(0, 179, 1);
        check(Math.abs(wrapped.sample(1, -179, 1)) < .03F, "yaw wrap is a two-degree turn");
        check(turn.sample(500, -170, 1) == 0, "long visibility gap clears stale inertia");
        check(turn.sample(0, 0, 1) == 0, "entity tick reset clears stale inertia");
        var fast = new MuntjacEarsAnimator.TurnResponse();
        var slow = new MuntjacEarsAnimator.TurnResponse();
        fast.sample(0, 0, 1);
        slow.sample(0, 0, 1);
        for (int tick = 1; tick < 80; tick++) {
            float target = tick < 12 ? tick * 7 : 84;
            slow.sample(tick, target, 1);
            for (int frame = 0; frame < 8; frame++) fast.sample(tick, target, frame / 8.0F);
            check(fast.sample(tick, target, 1) == slow.sample(tick, target, 1), "frame-rate-independent tick state");
        }
        var jump = new SpringType.Simulator(SpringType.LIGHT_NORMAL);
        float jumpRebound = 0;
        for (int tick = 0; tick < 80; tick++) {
            jump.tick(tick == 0 ? .42F : 0);
            jumpRebound = Math.min(jumpRebound, jump.getSpring(1));
        }
        check(jumpRebound < -.02F && Math.abs(jump.getSpring(1)) < .001F,
                "native vertical spring rebounds and settles");
        String report = "{\"pass\":true,\"checks\":" + checks
                + ",\"headTurnRebound\":" + rebound + ",\"jumpRebound\":" + jumpRebound
                + ",\"gameIntegrationTested\":false}\n";
        Files.writeString(Path.of(args[0]), report);
        System.out.println("MUNTJAC_EAR_CHECKS_PASS " + checks);
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
