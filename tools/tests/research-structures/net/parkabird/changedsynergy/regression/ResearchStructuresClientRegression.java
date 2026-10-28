package net.parkabird.changedsynergy.regression;

import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
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
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;

@Mod.EventBusSubscriber(modid="changed_synergy", value=Dist.CLIENT)
public final class ResearchStructuresClientRegression {
    private static int stage, frames, index, captures;
    private static boolean finished;
    private static CompletableFuture<Void> task;
    private static final String[] NAMES = {"water_tower_changed", "white_lab_tower", "red_lab_tower", "green_lab"};
    // Confirmed natural starts in the dedicated-server fixture's normal world.
    private static final BlockPos[] CENTERS = {new BlockPos(-6103, 35, -8279), new BlockPos(-2167, 63, -601),
            new BlockPos(-1127, 88, 1944), new BlockPos(2233, 73, 3305)};
    private static final long START = System.nanoTime();

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !Boolean.getBoolean("changed_synergy.researchStructuresClient")) return;
        var mc = Minecraft.getInstance();
        try {
            if (System.nanoTime() - START > 360_000_000_000L) throw new AssertionError("structure preview timed out at " + stage + "/" + index);
            if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                if (!mc.gameDirectory.getName().startsWith("author-feedback-client-")) throw new AssertionError("private fixture only");
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(10);
                if (Boolean.getBoolean("changed_synergy.greenLabOnly")) index = 3;
                org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
                var rules = new GameRules();
                rules.getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false, null);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
                mc.createWorldOpenFlows().createFreshLevel("research-preview",
                        new LevelSettings("research-preview", GameType.SPECTATOR, false, Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(220261025L, true, false), r -> r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).createWorldDimensions());
                stage = 1;
            }
            if (mc.player == null || mc.level == null || mc.getOverlay() != null) return;
            mc.setWindowActive(true);
            mc.options.hideGui = true;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getTutorial().stop();
            if (stage == 1 && mc.player.tickCount > 20) {
                var point = CENTERS[index];
                task = CompletableFuture.runAsync(() -> {
                    var server = mc.getSingleplayerServer();
                    var owner = server.getPlayerList().getPlayer(mc.player.getUUID());
                    var level = owner.serverLevel();
                    level.setDayTime(6000);
                    level.setWeatherParameters(6000, 0, false, false);
                    for (int x = (point.getX() >> 4) - 4; x <= (point.getX() >> 4) + 4; x++)
                        for (int z = (point.getZ() >> 4) - 4; z <= (point.getZ() >> 4) + 4; z++) level.getChunk(x, z);
                    owner.connection.teleport(point.getX() + 60, point.getY() + 58, point.getZ() + 76, 142, 20);
                }, mc.getSingleplayerServer());
                stage = 2; frames = 0;
            }
            if (stage == 2 && task.isDone()) {
                task.join();
                if (++frames < 200) return;
                var center = CENTERS[index];
                mc.player.setYRot(142); mc.player.setXRot(20);
                if (!mc.level.hasChunkAt(center)) throw new AssertionError("client structure chunks absent");
                var directory = mc.gameDirectory.toPath().resolve("research-structure-previews"); Files.createDirectories(directory);
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(directory.resolve(NAMES[index] + ".png")); }
                System.out.println("RESEARCH_CLIENT_CAPTURE " + NAMES[index]);
                captures++;
                index++;
                if (index == NAMES.length) {
                    System.out.println("RESEARCH_STRUCTURES_CLIENT_PASS: " + captures + " naturally generated structures rendered");
                    finished = true; mc.stop();
                } else { stage = 1; frames = 0; }
            }
        } catch (Throwable failure) {
            finished = true;
            System.err.println("RESEARCH_STRUCTURES_CLIENT_FAIL"); failure.printStackTrace(); mc.stop();
        }
    }
}
