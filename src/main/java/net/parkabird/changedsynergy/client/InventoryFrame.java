package net.parkabird.changedsynergy.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Vanilla inventory pixels, with straight edges drawn at their destination size. */
final class InventoryFrame {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "textures/gui/container/inventory.png");

    private InventoryFrame() {}

    static void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFFC6C6C6);
        graphics.fill(x + 4, y, x + width - 4, y + 1, 0xFF000000);
        graphics.fill(x + 4, y + 1, x + width - 4, y + 3, 0xFFFFFFFF);
        graphics.fill(x, y + 4, x + 1, y + height - 4, 0xFF000000);
        graphics.fill(x + 1, y + 4, x + 3, y + height - 4, 0xFFFFFFFF);
        graphics.fill(x + width - 1, y + 4, x + width, y + height - 4, 0xFF000000);
        graphics.fill(x + width - 3, y + 4, x + width - 1, y + height - 4, 0xFF555555);
        graphics.fill(x + 4, y + height - 1, x + width - 4, y + height, 0xFF000000);
        graphics.fill(x + 4, y + height - 3, x + width - 4, y + height - 1, 0xFF555555);
        graphics.blit(TEXTURE, x, y, 0, 0, 4, 4, 256, 256);
        graphics.blit(TEXTURE, x + width - 4, y, 172, 0, 4, 4, 256, 256);
        graphics.blit(TEXTURE, x, y + height - 4, 0, 162, 4, 4, 256, 256);
        graphics.blit(TEXTURE, x + width - 4, y + height - 4, 172, 162, 4, 4, 256, 256);
    }

    static void inset(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFFFFFFFF);
        graphics.fill(x, y, x + width - 1, y + height - 1, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B);
    }

    static void slot(GuiGraphics graphics, int x, int y) {
        graphics.blit(TEXTURE, x - 1, y - 1, 7, 83, 18, 18, 256, 256);
    }
}
