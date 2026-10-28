package net.parkabird.changedsynergy.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Compose native GUI icons in code so saddle resource-pack overrides remain usable. */
@Mod.EventBusSubscriber(modid = "changed_synergy", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CompanionSeatIcons {
    public static final int SIZE = 24;
    private static final ResourceLocation SADDLE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/saddle.png");
    private static final ResourceLocation PLAIN = ResourceLocation.fromNamespaceAndPath("changed_synergy", "dynamic/seat_saddle");
    private static final ResourceLocation PLUS = ResourceLocation.fromNamespaceAndPath("changed_synergy", "dynamic/seat_saddle_plus");
    private static boolean ready;
    private CompanionSeatIcons() {}
    public static ResourceLocation id(boolean plus) { return plus ? PLUS : PLAIN; }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                manager -> Minecraft.getInstance().execute(() -> ready = false));
    }
    public static void ensureLoaded() {
        if (ready) return;
        var mc = Minecraft.getInstance();
        try (var stream = mc.getResourceManager().open(SADDLE); var source = NativeImage.read(stream)) {
            for (boolean plus : new boolean[]{false, true}) {
                var image = new NativeImage(SIZE, SIZE, true);
                int startX = plus ? 2 : 4, startY = plus ? 3 : 4;
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                    int color = source.getPixelRGBA(x * source.getWidth() / 16, y * source.getHeight() / 16);
                    int gray = ((color & 255) * 30 + ((color >> 8) & 255) * 59 + ((color >> 16) & 255) * 11) / 100;
                    image.setPixelRGBA(startX + x, startY + y, (color & 0xFF000000) | gray | gray << 8 | gray << 16);
                }
                if (plus) {
                    // Five-pixel cross, with a one-pixel dark shadow to separate it from the saddle.
                    for (int[] point : new int[][]{{0,2},{1,2},{2,0},{2,1},{2,2},{2,3},{2,4},{3,2},{4,2}})
                        image.setPixelRGBA(17 + point[0] + 1, 15 + point[1] + 1, 0xFF303030);
                    for (int[] point : new int[][]{{0,2},{1,2},{2,0},{2,1},{2,2},{2,3},{2,4},{3,2},{4,2}})
                        image.setPixelRGBA(17 + point[0], 15 + point[1], 0xFFE0E0E0);
                }
                int minX = SIZE, minY = SIZE, maxX = -1, maxY = -1;
                for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) if ((image.getPixelRGBA(x, y) >>> 24) != 0) {
                    minX = Math.min(minX, x); minY = Math.min(minY, y); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                }
                var centered = new NativeImage(SIZE, SIZE, true);
                int shiftX = Math.round((SIZE - 1 - minX - maxX) / 2F), shiftY = Math.round((SIZE - 1 - minY - maxY) / 2F);
                for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++)
                    centered.setPixelRGBA(x + shiftX, y + shiftY, image.getPixelRGBA(x, y));
                image.close();
                mc.getTextureManager().register(id(plus), new DynamicTexture(centered));
            }
            ready = true;
        } catch (java.io.IOException failure) {
            // A missing pack resource falls back to the original icon instead of an unregistered texture.
            ready = false;
        }
    }
    public static ResourceLocation resolve(ResourceLocation id) { ensureLoaded(); return ready ? id : SADDLE; }
}
