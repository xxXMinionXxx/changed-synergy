package net.parkabird.changedsynergy.client;

import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.client.gui.ClipboardScreen;
import net.ltxprogrammer.changed.client.gui.ComputerTextScreen;
import net.ltxprogrammer.changed.client.gui.NoteScreen;
import net.ltxprogrammer.changed.client.gui.TextMenuScreen;
import net.ltxprogrammer.changed.world.inventory.ClipboardMenu;
import net.ltxprogrammer.changed.world.inventory.ComputerMenu;
import net.ltxprogrammer.changed.world.inventory.TextMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Keeps Changed's editor and menu protocol, with a content-sized, scrollable sheet. */
@Mod.EventBusSubscriber(modid = "changed_synergy", value = Dist.CLIENT)
public class AdaptiveDocumentScreen<T extends TextMenu> extends TextMenuScreen<T> {
    private record Paper(int width, int height, int left, int top, int right, int bottom, String texture) {}
    private static final Paper NOTE = new Paper(87, 121, 5, 15, 5, 5, "note_container.png");
    private static final Paper CLIPBOARD = new Paper(111, 151, 15, 40, 15, 20, "clipboard_container.png");
    private static final Paper DISC = new Paper(176, 166, 10, 22, 10, 10, "computer.png");
    private int sheetWidth, sheetHeight, textWidth, contentHeight, scroll;
    private int measuredWidth = -1, measuredHeight = -1;
    private String measuredText;

    /** Implemented on the native editor by AdaptiveDocumentMixin. */
    public interface NativeEditor {
        void changedSynergy$setEditor(TextFieldHelper editor);
        TextFieldHelper changedSynergy$editor();
        void changedSynergy$invalidateDisplay();
    }

    public AdaptiveDocumentScreen(T menu, Player player, Component title) {
        super(menu, player, title);
        // Keep the native 1024-character limit, but remove the old fixed-page height limit.
        nativeEditor().changedSynergy$setEditor(new TextFieldHelper(() -> menu.textCopy, text -> {
            menu.textCopy = text;
            nativeEditor().changedSynergy$invalidateDisplay();
        }, () -> Minecraft.getInstance().keyboardHandler.getClipboard(),
                text -> Minecraft.getInstance().keyboardHandler.setClipboard(text), text -> text.length() < 1024));
    }

    @SubscribeEvent
    public static void open(ScreenEvent.Opening event) {
        var previous = event.getNewScreen();
        // Leave other mods' custom readers alone.
        if (previous != null && (previous.getClass() == NoteScreen.class || previous.getClass() == ClipboardScreen.class
                || previous.getClass() == ComputerTextScreen.class) && Minecraft.getInstance().player != null) {
            var reader = (TextMenuScreen<?>)previous;
            event.setNewScreen(new AdaptiveDocumentScreen<>(reader.getMenu(), Minecraft.getInstance().player, reader.getTitle()));
        }
    }

    private Paper paper() { return menu instanceof ClipboardMenu ? CLIPBOARD : menu instanceof ComputerMenu ? DISC : NOTE; }
    private NativeEditor nativeEditor() { return (NativeEditor)this; }
    @Override public int getBackgroundWidth() { return sheetWidth > 0 ? sheetWidth : paper().width(); }
    @Override public int getBackgroundHeight() { return sheetHeight > 0 ? sheetHeight : paper().height(); }
    @Override public int getTextAreaWidth() { return textWidth > 0 ? textWidth : paper().width() - paper().left() - paper().right(); }
    @Override public int getTextAreaHeight() { return getBackgroundHeight() - paper().top() - paper().bottom(); }
    @Override public int getTextAreaX() { return paper().left(); }
    @Override public int getTextAreaY() { return paper().top(); }
    @Override public ResourceLocation getBackground() { return Changed.modResource("textures/gui/" + paper().texture()); }
    @Override public int getTextColor() { return menu instanceof ComputerMenu ? 0x00FF00 : 0x000000; }
    @Override public Component getNoteTitle() {
        return menu instanceof ClipboardMenu ? null : Component.translatable(menu instanceof ComputerMenu
                ? "container.changed.computer_writing" : "container.changed.note");
    }

    @Override protected void init() {
        measuredWidth = -1;
        measure();
        super.init();
        positionSheet();
    }

    private void measure() {
        if (width == measuredWidth && height == measuredHeight && menu.textCopy.equals(measuredText)) return;
        measuredWidth = width; measuredHeight = height; measuredText = menu.textCopy;
        var paper = paper();
        int maxWidth = Math.max(48, width - 24), maxHeight = Math.max(36, height - 52);
        int preferredHeight = Math.min(maxHeight, Math.max(paper.height(), height * 2 / 3));
        sheetWidth = Math.min(paper.width(), maxWidth);
        int padding = paper.left() + paper.right();
        while (sheetWidth < maxWidth) {
            int needed = font.wordWrapHeight(menu.textCopy, Math.max(8, sheetWidth - padding))
                    + paper.top() + paper.bottom() + 9;
            int balancedHeight = sheetWidth * paper.height() / paper.width();
            if (needed <= Math.min(preferredHeight, Math.max(paper.height(), balancedHeight))) break;
            sheetWidth = Math.min(maxWidth, sheetWidth + 8);
        }
        textWidth = Math.max(8, sheetWidth - padding);
        contentHeight = Math.max(9, font.wordWrapHeight(menu.textCopy, textWidth) + (canEdit() ? 9 : 0));
        sheetHeight = Mth.clamp(contentHeight + paper.top() + paper.bottom(), Math.min(paper.height(), maxHeight), maxHeight);
        if (contentHeight > getTextAreaHeight()) {
            textWidth = Math.max(8, textWidth - 5);
            contentHeight = Math.max(9, font.wordWrapHeight(menu.textCopy, textWidth) + (canEdit() ? 9 : 0));
        }
        imageWidth = sheetWidth; imageHeight = sheetHeight;
        scroll = Mth.clamp(scroll, 0, maxScroll());
        positionSheet();
    }

    private void positionSheet() {
        leftPos = (width - imageWidth) / 2;
        topPos = Math.max(8, (height - imageHeight - 30) / 2);
        for (var child : children()) if (child instanceof AbstractWidget widget) widget.setY(topPos + imageHeight + 10);
        nativeEditor().changedSynergy$invalidateDisplay();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        measure();
        super.render(graphics, mouseX, mouseY, partialTicks);
        if (maxScroll() > 0) {
            int x = leftPos + paper().left() + textWidth + 2, y = topPos + paper().top();
            int h = getTextAreaHeight(), thumb = Math.max(8, h * h / contentHeight);
            int top = y + (h - thumb) * scroll / maxScroll();
            graphics.fill(x, y, x + 2, y + h, menu instanceof ComputerMenu ? 0xFF283028 : 0xFFD0CFCB);
            graphics.fill(x, top, x + 2, top + thumb, menu instanceof ComputerMenu ? 0xFF79B179 : 0xFF77736A);
        }
    }

    public void renderPaper(GuiGraphics graphics) {
        var paper = paper();
        int[] sx = {0, paper.left(), paper.width() - paper.right(), paper.width()};
        int[] sy = {0, paper.top(), paper.height() - paper.bottom(), paper.height()};
        int[] dx = {leftPos, leftPos + paper.left(), leftPos + imageWidth - paper.right(), leftPos + imageWidth};
        int[] dy = {topPos, topPos + paper.top(), topPos + imageHeight - paper.bottom(), topPos + imageHeight};
        for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) {
            graphics.blit(getBackground(), dx[x], dy[y], dx[x + 1] - dx[x], dy[y + 1] - dy[y],
                    sx[x], sy[y], sx[x + 1] - sx[x], sy[y + 1] - sy[y], paper.width(), paper.height());
        }
    }
    public void beginText(GuiGraphics graphics) {
        graphics.enableScissor(leftPos + paper().left(), topPos + paper().top() - 1,
                leftPos + paper().left() + textWidth, topPos + paper().top() + getTextAreaHeight());
        graphics.pose().pushPose(); graphics.pose().translate(0, -scroll, 0);
    }
    public void endText(GuiGraphics graphics) { graphics.pose().popPose(); graphics.disableScissor(); }
    public int textScroll() { return scroll; }
    public int maxScroll() { return Math.max(0, contentHeight - getTextAreaHeight()); }
    public boolean insideText(double x, double y) {
        return x >= leftPos + paper().left() && x < leftPos + imageWidth - paper().right()
                && y >= topPos + paper().top() && y < topPos + imageHeight - paper().bottom();
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (insideText(x, y) && maxScroll() > 0) { scroll = Mth.clamp(scroll - (int)(delta * 27), 0, maxScroll()); return true; }
        return super.mouseScrolled(x, y, delta);
    }
    @Override public boolean charTyped(char character, int modifiers) {
        boolean result = super.charTyped(character, modifiers);
        if (result) revealCaret();
        return result;
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_PAGE_UP || key == GLFW.GLFW_KEY_PAGE_DOWN || !canEdit()
                && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_HOME || key == GLFW.GLFW_KEY_END)) {
            int amount = key == GLFW.GLFW_KEY_PAGE_UP ? -getTextAreaHeight() : key == GLFW.GLFW_KEY_PAGE_DOWN ? getTextAreaHeight()
                    : key == GLFW.GLFW_KEY_UP ? -9 : key == GLFW.GLFW_KEY_DOWN ? 9 : key == GLFW.GLFW_KEY_HOME ? -maxScroll() : maxScroll();
            scroll = Mth.clamp(scroll + amount, 0, maxScroll()); return true;
        }
        boolean result = super.keyPressed(key, scanCode, modifiers);
        if (result && canEdit()) revealCaret();
        return result;
    }
    private void revealCaret() {
        measure();
        int cursor = nativeEditor().changedSynergy$editor().getCursorPos();
        int y = Math.max(0, font.wordWrapHeight(menu.textCopy.substring(0, cursor) + "_", textWidth) - 9);
        scroll = Mth.clamp(scroll, Math.max(0, y + 9 - getTextAreaHeight()), Math.max(0, y));
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }
}
