package com.example.screen;

import com.example.ExampleMod;
import com.example.block.entity.JarBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class JarScreen extends HandledScreen<JarScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(ExampleMod.MOD_ID, "textures/gui/container/jar.png");

    public JarScreen(JarScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 230;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight,
                backgroundWidth, backgroundHeight);
        for (Slot slot : handler.slots) {
            drawWoodenSlot(context, x + slot.x, y + slot.y, slot.id == 0);
        }
    }

    private static void drawWoodenSlot(DrawContext context, int x, int y, boolean jar) {
        int edge = jar ? 0xFFA9BDA0 : 0xFF9C7A48;
        context.fill(x - 1, y - 1, x + 17, y + 17, 0xFF17120C);
        context.fill(x, y, x + 17, y + 17, edge);
        context.fill(x, y, x + 16, y + 16, 0xFF32281C);
        context.fill(x + 1, y + 1, x + 15, y + 15, 0xFF403221);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawCenteredTextWithShadow(textRenderer, title, backgroundWidth / 2, 15, 0xF1DFC0);
        Text counter = Text.translatable("gui.smokemod.jar_count", handler.getStoredCount(), handler.getCapacity());
        context.drawCenteredTextWithShadow(textRenderer, counter, backgroundWidth / 2, 98, 0xF1DFC0);
        context.drawText(textRenderer, playerInventoryTitle, 8, 136, 0xD6C3A1, false);
        int percent = Math.round(handler.getCuringProgress() * 100);
        Text status = handler.isCured() ? Text.translatable("gui.smokemod.jar_ready")
                : handler.isCuring() ? Text.translatable("gui.smokemod.jar_curing", percent)
                : Text.translatable("gui.smokemod.jar_waiting");
        context.drawCenteredTextWithShadow(textRenderer, status, 88, 110, 0xF1DFC0);
        int fill = Math.round(142 * handler.getCuringProgress());
        if (fill > 0) {
            context.fill(17, 124, 17 + fill, 131, 0xFF6E913C);
            context.fill(17, 124, 17 + fill, 126, 0xFFB6CC70);
            context.fill(17, 130, 17 + fill, 131, 0xFF425C26);
            context.fill(16 + fill, 124, 17 + fill, 130, 0xFFD7E79A);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        if (handler.isCuring() && mouseX >= x + 15 && mouseX < x + 161
                && mouseY >= y + 110 && mouseY < y + 134) {
            int seconds = Math.max(0, JarBlockEntity.CURING_TIME - handler.getCuringSeconds());
            context.drawTooltip(textRenderer, Text.translatable("gui.smokemod.jar_remaining",
                    seconds / 60, String.format(java.util.Locale.ROOT, "%02d", seconds % 60)), mouseX, mouseY);
        }
        if (focusedSlot != null && focusedSlot.id == 0 && !focusedSlot.hasStack()
                && handler.getCursorStack().isEmpty()) {
            context.drawTooltip(textRenderer,
                    Text.translatable("gui.smokemod.jar_hint", handler.getCapacity()), mouseX, mouseY);
        }
    }
}
