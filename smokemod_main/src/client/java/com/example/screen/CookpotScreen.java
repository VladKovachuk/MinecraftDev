package com.example.screen;

import com.example.ExampleMod;
import com.example.block.entity.CookpotBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CookpotScreen extends HandledScreen<CookpotScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(ExampleMod.MOD_ID, "textures/gui/container/cookpot.png");
    private static final String[] STATUS = {"no_heat", "need_water", "need_input", "output_full", "cooking"};
    private static final int TEXT_COLOR = 0xFFE2B5;

    public CookpotScreen(CookpotScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 200;
    }

    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, 256, 256);
        // Dynamic indicators are cropped sprites from the same PNG atlas.
        for (int i = 0; i < handler.getWater(); i++) {
            context.drawTexture(TEXTURE, x + 21, y + 73 - i * 6, 176, 6, 14, 4, 256, 256);
        }
        int progressWidth = handler.getProgress() * 42 / CookpotBlockEntity.COOK_TICKS;
        if (progressWidth > 0) {
            context.drawTexture(TEXTURE, x + 67, y + 80, 176, 0, progressWidth, 2, 256, 256);
        }
        if (handler.hasHeat())
            context.drawTexture(TEXTURE, x + 76, y + 84, 176, 20, 24, 9, 256, 256);
    }

    private void centeredText(DrawContext context, Text text, int center, int top) {
        context.drawText(textRenderer, text, center - textRenderer.getWidth(text) / 2, top, TEXT_COLOR, true);
    }

    @Override protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        centeredText(context, Text.translatable("gui.smokemod.cookpot.water_count", handler.getWater()), 28, 83);
        context.drawText(textRenderer, playerInventoryTitle, 8, 107, 0x404040, false);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        if (mouseX >= x + 19 && mouseX < x + 38 && mouseY >= y + 59 && mouseY < y + 80)
            context.drawTooltip(textRenderer, Text.translatable("gui.smokemod.cookpot.hint.water"), mouseX, mouseY);
        if (mouseX >= x + 65 && mouseX < x + 111 && mouseY >= y + 78 && mouseY < y + 94)
            context.drawTooltip(textRenderer, java.util.List.of(
                    Text.translatable("gui.smokemod.cookpot.progress", handler.getProgress() * 100 / CookpotBlockEntity.COOK_TICKS),
                    Text.translatable("gui.smokemod.cookpot." + STATUS[handler.getStatus()])), mouseX, mouseY);
    }
}
