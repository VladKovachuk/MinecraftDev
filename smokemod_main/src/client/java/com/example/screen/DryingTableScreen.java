package com.example.screen;

import com.example.ExampleMod;
import com.example.block.entity.DryingTableBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class DryingTableScreen extends HandledScreen<DryingTableScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.of(ExampleMod.MOD_ID, "textures/gui/container/drying_table.png");

    // TEX_W/TEX_H = PANEL_W/PANEL_H → вся текстура 891×1001 масштабируется в размер панели
    private static final int TEX_W = 176;
    private static final int TEX_H = 200;

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 200;

    public DryingTableScreen(DryingTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth  = PANEL_W;
        this.backgroundHeight = PANEL_H;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Заголовок и метку "Инвентарь" не рисуем
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width  - this.backgroundWidth)  / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(TEXTURE, x, y, 0, 0, PANEL_W, PANEL_H, TEX_W, TEX_H);
    }

    @Override
    protected List<Text> getTooltipFromItem(ItemStack stack) {
        List<Text> tooltip = new ArrayList<>(super.getTooltipFromItem(stack));
        if (focusedSlot != null && focusedSlot.id >= 0
                && focusedSlot.id < DryingTableBlockEntity.INPUT_SLOTS
                && DryingTableBlockEntity.canDry(stack)) {
            int percent = (int) (handler.getDryingProgress(focusedSlot.id) * 100);
            tooltip.add(Text.translatable("gui.smokemod.drying_progress", percent).formatted(Formatting.GRAY));
        }
        return tooltip;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

    }
}
