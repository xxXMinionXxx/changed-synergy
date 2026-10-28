package net.parkabird.changedsynergy.regression;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import net.ltxprogrammer.changed.entity.PlayerDataExtension;
import net.ltxprogrammer.changed.entity.variant.TransfurVariantInstance;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.client.renderer.model.LatexMothModel;
import net.ltxprogrammer.changed.client.renderer.model.LatexBeeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.ChangedSynergyMod;
import net.parkabird.changedsynergy.client.LatexSilkMothRenderer;
import net.parkabird.changedsynergy.client.model.LatexSilkMothModel;
import net.parkabird.changedsynergy.entity.LatexSilkMoth;
import net.parkabird.changedsynergy.init.ChangedSynergyEntities;
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;
import net.parkabird.changedsynergy.init.ChangedSynergyTransfurVariants;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Mod.EventBusSubscriber(modid = ChangedSynergyMod.MOD_ID, value = Dist.CLIENT)
public final class SilkMothClientRegression {
    private static final long START = System.nanoTime();
    private static boolean loading, finished;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("silkmoth.client.regression") || finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
            if (System.nanoTime() - START > 240_000_000_000L) throw new AssertionError("Client fixture timeout");
            Path directory = mc.gameDirectory.toPath().toAbsolutePath().normalize();
            check(directory.endsWith(Path.of(".codex-tmp", "silk-moth-client")), "private client fixture");
            if (!loading && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                loading = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(2);
                mc.options.simulationDistance().set(2);
                mc.options.guiScale().set(2);
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                if (Files.isRegularFile(directory.resolve("saves/silk-moth/level.dat"))) {
                    mc.createWorldOpenFlows().loadLevel(mc.screen, "silk-moth");
                } else {
                    var rules = new GameRules();
                    rules.getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false, null);
                    rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                    var settings = new LevelSettings("silk-moth", GameType.CREATIVE, false,
                            Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("silk-moth", settings, new WorldOptions(73451L, false, false),
                            registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                    .getOrThrow(WorldPresets.FLAT).createWorldDimensions());
                }
            }
            if (mc.level == null || mc.player == null || mc.player.tickCount < 40 || mc.getOverlay() != null) return;
            run(mc, directory);
            finished = true;
            System.out.println("SILK_MOTH_CLIENT_REGRESSION_PASS");
            Files.writeString(directory.resolve("silk-moth-client-result.txt"), "PASS\n");
            mc.stop();
        } catch (Throwable failure) {
            System.err.println("SILK_MOTH_CLIENT_REGRESSION_FAIL");
            failure.printStackTrace();
            finished = true;
            mc.stop();
        }
    }

    private static void run(Minecraft mc, Path directory) throws Exception {
        mc.resizeDisplay();
        LatexSilkMoth moth = ChangedSynergyEntities.LATEX_SILK_MOTH.get().create(mc.level);
        var renderer = mc.getEntityRenderDispatcher().getRenderer(moth);
        check(renderer instanceof LatexSilkMothRenderer, "registered renderer");
        var model = ((LatexSilkMothRenderer) renderer).getModel();
        check(Set.of(model.getArm(HumanoidArm.LEFT), model.getArm(HumanoidArm.RIGHT),
                model.getOtherArm(HumanoidArm.LEFT), model.getOtherArm(HumanoidArm.RIGHT)).size() == 4,
                "four independent rendered arms");
        check(mc.getResourceManager().getResource(LatexSilkMothRenderer.TEXTURE).isPresent(), "body texture present");
        float[] start = pose(model, moth, 0), peak = pose(model, moth, 30), end = pose(model, moth, 120);
        check(!Arrays.equals(start, peak), "actual runtime wings and antennae move");
        for (int i = 0; i < start.length; i++) check(Math.abs(start[i] - end[i]) < 0.001F, "continuous runtime idle loop");
        motionRegression(mc, directory, model, moth);
        var playerForm = TransfurVariantInstance.variantFor(ChangedSynergyTransfurVariants.LATEX_SILK_MOTH.get(), mc.player);
        ((PlayerDataExtension) mc.player).setTransfurVariant(playerForm);
        playerForm.transfurProgressionO = playerForm.transfurProgression = 1;
        var surrogate = playerForm.getChangedEntity();
        check(surrogate instanceof LatexSilkMoth silkBody
                && mc.getEntityRenderDispatcher().getRenderer(silkBody) instanceof LatexSilkMothRenderer,
                "player form uses the new renderer");
        SilkMothGrabRegression.run(mc, model, (LatexSilkMoth) surrogate, directory);
        extraJumpRegression(mc, directory, model, (LatexSilkMoth) surrogate, playerForm);
        var extra = playerForm.getAbilityInstance(ChangedAbilities.SWITCH_HANDS.get());
        check(extra != null, "extra-hand inventory initialized");
        extra.extraLeftHand = new ItemStack(Items.APPLE);
        extra.extraRightHand = new ItemStack(Items.TORCH);
        moth.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        moth.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        mc.player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        mc.player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        surrogate.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        surrogate.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        gallery(mc, directory, moth, (LatexSilkMoth) surrogate, "silk-moth-game-gallery.png");
    }

    private static float[] pose(LatexSilkMothModel<LatexSilkMoth> model, LatexSilkMoth moth, int age) {
        moth.tickCount = age;
        model.prepareMobModel(moth, 0, 0, 0);
        model.setupAnim(moth, 0, 0, age, 0, 0);
        // Changed's default wing fold has its own period; compare only the added six-second offset.
        float defaultFold = net.minecraft.util.Mth.lerp(model.getAnimator(moth).ageLerp, 0.174532F, 0.3490659F);
        return new float[]{model.getTorso().getChild("LeftWing").yRot + defaultFold,
                model.getTorso().getChild("RightWing").yRot - defaultFold,
                model.getTorso().getChild("LeftWing2").yRot,
                model.getHead().getChild("RightAntennaMotion").getChild("RightAntenna")
                        .getChild("RightAntennaTipMotion").xRot,
                model.getTorso().getChild("Tail").yRot};
    }

    private static void motionRegression(Minecraft mc, Path directory,
            LatexSilkMothModel<LatexSilkMoth> model, LatexSilkMoth moth) throws Exception {
        var original = new LatexMothModel(LatexMothModel.createBodyLayer().bakeRoot());
        var beeModel = new LatexBeeModel(LatexBeeModel.createBodyLayer().bakeRoot());
        var bee = ChangedEntities.LATEX_BEE.get().create(mc.level);
        ModelPart left = model.getHead().getChild("LeftAntennaMotion");
        ModelPart right = model.getHead().getChild("RightAntennaMotion");
        ModelPart tip = right.getChild("RightAntenna").getChild("RightAntennaTipMotion");
        ModelPart bind = right.getChild("RightAntenna");
        float bindYaw = bind.yRot;
        float peakRoot = 0, peakTip = 0, peakJump = 0;
        for (int tick = 0; tick < 300; tick++) {
            float turn = tick >= 20 && tick < 60 ? 12 : tick >= 60 && tick < 100 ? -12 : 0;
            boolean walking = tick >= 100 && tick < 130;
            boolean airborne = tick >= 130 && tick < 150;
            double vertical = airborne ? (tick < 140 ? .6 : -.6) : 0;
            for (var body : new net.ltxprogrammer.changed.entity.ChangedEntity[]{moth, bee}) {
                body.tickCount = tick;
                body.yBodyRotO = body.yBodyRot;
                body.yBodyRot += turn;
                body.setYRot(body.yBodyRot);
                body.setOnGround(!airborne);
                body.setShiftKeyDown(tick >= 200 && tick < 230);
                body.setDeltaMovement(new Vec3(0, vertical, walking ? .2 : 0));
                // This is the same entity-owned spring/turn update used for NPCs and player forms.
                body.variantTick(mc.level);
            }
            float swing = tick * .6F, amount = walking ? .6F : 0;
            for (float partial : new float[]{0, .5F, 1}) {
                model.prepareMobModel(moth, swing, amount, partial);
                original.prepareMobModel(moth, swing, amount, partial);
                beeModel.prepareMobModel(bee, swing, amount, partial);
                // Exercise the swimming blend as well as actual crouch/turn/jump entity state.
                if (tick >= 240 && tick < 260) {
                    model.getAnimator(moth).swimAmount = .7F;
                    original.getAnimator(moth).swimAmount = .7F;
                    beeModel.getAnimator(bee).swimAmount = .7F;
                }
                model.setupAnim(moth, swing, amount, tick + partial, 0, 0);
                original.setupAnim(moth, swing, amount, tick + partial, 0, 0);
                beeModel.setupAnim(bee, swing, amount, tick + partial, 0, 0);
                sameTail(model.getTorso().getChild("Tail"), original.getTorso().getChild("Tail"));
                sameTail(model.getTorso().getChild("Tail"), beeModel.getTorso().getChild("Tail"));
                ModelPart beeLeft = beeModel.getHead().getChild("LeftEar");
                ModelPart beeRight = beeModel.getHead().getChild("RightEar");
                check(Math.abs(left.yRot - beeLeft.yRot) < .00001F
                        && Math.abs(right.yRot - beeRight.yRot) < .00001F,
                        "antenna roots retain the original bee turning response");
                peakRoot = Math.max(peakRoot, Math.abs(right.yRot));
                peakTip = Math.max(peakTip, Math.abs(tip.yRot) + Math.abs(tip.zRot));
                peakJump = Math.max(peakJump, Math.abs(right.xRot - beeRight.xRot));
                check(bind.yRot == bindYaw, "backward antenna bind orientation preserved");
            }
        }
        check(peakRoot > .10F, "visible antenna response to body turns");
        check(peakTip > .04F && peakJump > .02F, "articulated tips and jumping springs respond");
        float[] fixed = tailRotations(model.getTorso().getChild("Tail"));
        for (int frame = 0; frame < 3000; frame++) {
            model.prepareMobModel(moth, 0, 0, 1);
            model.setupAnim(moth, 0, 0, 300, 0, 0);
            if (frame == 0) fixed = tailRotations(model.getTorso().getChild("Tail"));
            check(Arrays.equals(fixed, tailRotations(model.getTorso().getChild("Tail"))),
                    "tail remains stable over repeated renders without manually resetting bones");
        }
        moth.setShiftKeyDown(false);
        moth.setOnGround(true);
        moth.setDeltaMovement(Vec3.ZERO);
        Files.writeString(directory.resolve("silk-moth-motion-result.json"),
                "{\"pass\":true,\"movementSamples\":900,\"repeatRenders\":3000,"
                + "\"tailMatchesMothAndBee\":true,\"antennaRootMatchesBee\":true,"
                + "\"peakAntennaYawRadians\":" + peakRoot + ",\"peakTipBendRadians\":" + peakTip
                + ",\"peakJumpPitchRadians\":" + peakJump + "}\n");
    }

    private static void sameTail(ModelPart actual, ModelPart expected) {
        float[] a = tailRotations(actual), b = tailRotations(expected);
        for (int i = 0; i < a.length; i++)
            check(Math.abs(a[i] - b[i]) < .00001F, "all tail joints match the original moth/bee at axis " + i);
    }

    private static void extraJumpRegression(Minecraft mc, Path directory,
            LatexSilkMothModel<LatexSilkMoth> model, LatexSilkMoth body,
            TransfurVariantInstance<?> form) throws Exception {
        long time = mc.level.getGameTime();
        try {
            mc.player.setOnGround(true);
            form.tick();
            check(form.getJumpCharges() == 6, "original six extra-jump charges retained");
            mc.player.jumpFromGround();
            check(Float.isInfinite(body.getWingFlapTime(0)), "ground jump does not trigger extra-jump flap");
            mc.player.setOnGround(false);
            float[] neutral = wingPose(model, body);
            for (int jump = 0; jump < 6; jump++) {
                long start = time + jump * 12;
                mc.level.setGameTime(start);
                net.ltxprogrammer.changed.init.ChangedKeyMappings.KeyEventListener.onKeyInput(
                        new net.minecraftforge.client.event.InputEvent.Key(
                                mc.options.keyJump.getKey().getValue(), 0, org.lwjgl.glfw.GLFW.GLFW_PRESS, 0));
                check(form.getJumpCharges() == 5 - jump, "actual Changed key handler consumes one charge");
                check(body.getWingFlapTime(0) == 0, "actual extra jump starts wing animation");
                mc.level.setGameTime(start + 4);
                float[] peak = wingPose(model, body);
                check(neutral[0] - peak[0] > 1.2F && neutral[2] - peak[2] > .9F,
                        "upper and lower wings visibly flap on every extra jump");
                for (int i = 4; i < peak.length; i++)
                    check(Math.abs(peak[i] - neutral[i]) < .00001F, "wing attachment stays at its fixed short-stem hinge");
                if (jump == 5) gallery(mc, directory, body, body, "silk-moth-jump-gallery.png");
                mc.level.setGameTime(start + 10);
                float[] end = wingPose(model, body);
                for (int i = 0; i < end.length; i++)
                    check(Math.abs(end[i] - neutral[i]) < .00001F, "flap finishes at the original wing pose");
            }
            long lastStart = time + 60;
            net.ltxprogrammer.changed.init.ChangedKeyMappings.KeyEventListener.onKeyInput(
                    new net.minecraftforge.client.event.InputEvent.Key(
                            mc.options.keyJump.getKey().getValue(), 0, org.lwjgl.glfw.GLFW.GLFW_PRESS, 0));
            check(body.getWingFlapTime(0) == mc.level.getGameTime() - lastStart,
                    "exhausted charges do not create another flap");
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                var packet = new net.parkabird.changedsynergy.network.SilkMothWingFlapPacket(mc.player.getId());
                net.parkabird.changedsynergy.network.SilkMothWingFlapPacket.encode(packet, buffer);
                check(packet.equals(net.parkabird.changedsynergy.network.SilkMothWingFlapPacket.decode(buffer)),
                        "wing flap packet round trip");
                net.parkabird.changedsynergy.client.SilkMothWingFlapClient.receive(mc.player.getId());
                check(body.getWingFlapTime(0) == 0, "tracking-player packet resolves the player's silkmoth body");
            } finally {
                buffer.release();
            }
            Files.writeString(directory.resolve("silk-moth-extra-jump-result.json"),
                    "{\"pass\":true,\"actualChangedKeyHandler\":true,\"extraJumps\":6,"
                    + "\"allFourWingsFlap\":true,\"returnsToIdle\":true,"
                    + "\"shortStemUpperEndHinges\":true,"
                    + "\"groundAndExhaustedInputsIgnored\":true,\"packetRoundTripAndClientLookup\":true}\n");
        } finally {
            mc.level.setGameTime(time - 10);
            body.flapWings();
            mc.level.setGameTime(time);
            mc.player.setOnGround(true);
        }
    }

    private static float[] wingPose(LatexSilkMothModel<LatexSilkMoth> model, LatexSilkMoth body) {
        body.tickCount = 40;
        model.prepareMobModel(body, 0, 0, 0);
        model.setupAnim(body, 0, 0, 40, 0, 0);
        var torso = model.getTorso();
        var left = torso.getChild("LeftWing").getChild("LeftWingPivot").getChild("LeftWingFlap");
        var right = torso.getChild("RightWing").getChild("RightWingPivot").getChild("RightWingFlap");
        var leftLower = torso.getChild("LeftWing2").getChild("LeftWingPivot2").getChild("LeftWing2Flap");
        var rightLower = torso.getChild("RightWing2").getChild("RightWingPivot2").getChild("RightWing2Flap");
        return new float[]{left.zRot, right.zRot, leftLower.zRot, rightLower.zRot,
                left.x, left.y, left.z, right.x, right.y, right.z,
                leftLower.x, leftLower.y, leftLower.z, rightLower.x, rightLower.y, rightLower.z};
    }

    private static float[] tailRotations(ModelPart tail) {
        var primary = tail.getChild("TailPrimary");
        var secondary = primary.getChild("TailSecondary");
        return new float[]{tail.xRot, tail.yRot, tail.zRot, primary.xRot, primary.yRot, primary.zRot,
                secondary.xRot, secondary.yRot, secondary.zRot};
    }

    private static void gallery(Minecraft mc, Path directory, LatexSilkMoth moth, LatexSilkMoth playerBody,
            String filename) throws Exception {
        int width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
        var target = new TextureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, Minecraft.ON_OSX);
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            modelView.setIdentity();
            modelView.translate(0, 0, -11000);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, width, height, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            target.setClearColor(.10F, .12F, .15F, 1);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            var graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            for (int row = 0; row < 2; row++) {
                for (var slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    var item = slot == EquipmentSlot.CHEST ? Items.IRON_CHESTPLATE
                            : slot == EquipmentSlot.LEGS ? Items.IRON_LEGGINGS : Items.IRON_BOOTS;
                    for (var body : new net.minecraft.world.entity.LivingEntity[]{moth, playerBody, mc.player})
                        body.setItemSlot(slot, row == 0 ? ItemStack.EMPTY : new ItemStack(item));
                }
                for (int column = 0; column < 4; column++) {
                    var body = column == 3 ? playerBody : moth;
                    body.tickCount = row == 0 ? 30 : 90;
                    float yaw = column == 1 ? 15 : column == 2 ? 250 : 180;
                    body.yBodyRot = body.yBodyRotO = yaw;
                    body.yHeadRot = body.yHeadRotO = yaw;
                    body.setYRot(yaw);
                    int x = (column * 2 + 1) * width / 8, y = (row + 1) * height / 2 - 22;
                    InventoryScreen.renderEntityInInventory(graphics, x, y, height / 8,
                            new Quaternionf().rotationZ((float) Math.PI), new Quaternionf(), body);
                    String label = new String[]{"Four hands", "Back / wings", "Side", "Player form"}[column];
                    graphics.drawCenteredString(mc.font, label + (row == 0 ? "" : " / armor"), x, y + 5, 0xFFFFFF);
                }
            }
            graphics.flush();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(target)) {
                int background = image.getPixelRGBA(0, 0), occupied = 0;
                for (int y = 0; y < image.getHeight(); y += 4)
                    for (int x = 0; x < image.getWidth(); x += 4)
                        if (image.getPixelRGBA(x, y) != background) occupied++;
                check(occupied > 1000, "gallery contains rendered models, not only a cleared background");
                image.writeToFile(directory.resolve(filename));
            }
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            target.destroyBuffers();
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
