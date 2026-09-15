package com.example.screen;

import com.example.ExampleMod;
import com.example.block.CookpotBlock;
import com.example.block.CookpotFrameBlock;
import com.example.block.entity.CookpotBlockEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads real baked assets and captures the actual screen, without touching a user's world. */
public final class CookpotClientSmokeTest implements ClientModInitializer {
    private boolean ran;
    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("smokemod.cookpotSmokeTest")) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ran || client.getOverlay() != null) return;
            ran = true;
            try {
                for (Item item : new Item[]{ExampleMod.COOKPOT_ITEM, ExampleMod.COOKPOT_FRAME_ITEM, Items.CALCITE, ExampleMod.REFINED_OPIUM}) {
                    var model = client.getItemRenderer().getModel(new ItemStack(item), null, null, 0);
                    check(model != client.getBakedModelManager().getMissingModel(), "Missing item model: " + item);
                    check(!model.getParticleSprite().getContents().getId().getPath().contains("missingno"), "Missing item texture");
                }
                for (int water = 0; water <= 3; water++) {
                    var state = ExampleMod.COOKPOT.getDefaultState().with(CookpotBlock.WATER_LEVEL, water);
                    var model = client.getBlockRenderManager().getModel(state);
                    check(model != client.getBakedModelManager().getMissingModel(), "Missing water model " + water);
                    var quads = model.getQuads(state, null, Random.create());
                    check(quads.size() >= 57 * 6, "Cookpot geometry missing at water level " + water);
                    check(quads.stream().noneMatch(q -> q.getSprite().getContents().getId().getPath().contains("missingno")), "Missing block texture");
                }
                for (boolean lit : new boolean[]{false, true}) for (boolean wet : new boolean[]{false, true}) {
                    var state = ExampleMod.COOKPOT_FRAME.getDefaultState().with(CookpotFrameBlock.LIT, lit).with(CookpotFrameBlock.WATERLOGGED, wet);
                    var model = client.getBlockRenderManager().getModel(state);
                    check(model != client.getBakedModelManager().getMissingModel(), "Missing frame model");
                    var quads = model.getQuads(state, null, Random.create());
                    check(quads.size() >= 26 * 6, "Frame geometry missing");
                    check(quads.stream().noneMatch(q -> q.getSprite().getContents().getId().getPath().contains("missingno")), "Missing frame texture");
                    check(quads.stream().anyMatch(q -> q.getSprite().getContents().getId().toString().equals("minecraft:block/netherrack")),
                            "The fire tray must use vanilla netherrack");
                    check(quads.stream().anyMatch(q -> q.getSprite().getContents().getId().getPath().contains("fire_0")) == lit,
                            "Contained flame must appear only in the lit frame model");
                }
                client.setScreen(new CaptureScreen());
            } catch (Throwable failure) { fail(failure); }
        });
    }
    private static final class CaptureScreen extends Screen {
        private CookpotScreen potScreen;
        private SimpleInventory inventory;
        private ArrayPropertyDelegate data;
        private int frames;
        private CaptureScreen() { super(Text.literal("Cookpot visual verification")); }
        @Override protected void init() {
            inventory = new SimpleInventory(CookpotBlockEntity.SIZE);
            data = new ArrayPropertyDelegate(3);
            PlayerInventory player = new PlayerInventory(null);
            CookpotScreenHandler handler = new CookpotScreenHandler(0, player, inventory, data);
            check(handler.slots.get(5).x == 80 && handler.slots.get(5).y == 36, "Output must be in the pot centre");
            check(!handler.slots.get(5).canInsert(new ItemStack(ExampleMod.REFINED_OPIUM)), "Output must reject insertion");
            inventory.setStack(5, new ItemStack(ExampleMod.REFINED_OPIUM, 2));
            check(handler.slots.get(5).takeStack(1).isOf(ExampleMod.REFINED_OPIUM)
                    && inventory.getStack(5).getCount() == 1, "Centre slot must allow collecting the result");
            for (int state = 0; state <= CookpotBlockEntity.COOKING; state++) {
                data.set(2, state);
                check(handler.hasHeat() == (state != CookpotBlockEntity.NO_HEAT), "Heat indicator must be independent of recipe readiness");
            }
            potScreen = new CookpotScreen(handler, player, Text.translatable("block.smokemod.cookpot"));
            potScreen.init(client, width, height);
            inventory.setStack(1, new ItemStack(Items.BUCKET, 3));
            inventory.setStack(2, new ItemStack(Items.CALCITE, 2));
            inventory.setStack(3, new ItemStack(Items.CHARCOAL, 8));
            inventory.setStack(4, new ItemStack(ExampleMod.RAW_OPIUM, 4));
            inventory.setStack(5, new ItemStack(ExampleMod.REFINED_OPIUM, 2));
            data.set(0, 3);
            data.set(1, 720);
            data.set(2, CookpotBlockEntity.COOKING);
        }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            potScreen.render(context, 0, 0, delta);
            context.draw();
            try {
                if (++frames == 4) {
                    capture("cookpot-ui-filled.png");
                    inventory.clear();
                    data.set(0, 0); data.set(1, 0); data.set(2, CookpotBlockEntity.NO_HEAT);
                } else if (frames == 8) {
                    capture("cookpot-ui-empty.png");
                    data.set(0, 2); data.set(2, CookpotBlockEntity.NEED_WATER);
                } else if (frames == 12) {
                    capture("cookpot-ui-heated-idle.png");
                    Files.writeString(Path.of("cookpot-result.txt"), "PASS: frame lit/unlit/waterlogged models, flame textures, calcite, all water models and actual GUI rendering.\n");
                    System.out.println("COOKPOT_CLIENT_SMOKE_TEST_PASSED");
                    client.scheduleStop();
                }
            } catch (Throwable failure) { fail(failure); }
        }
        private void capture(String name) throws Exception {
            try (var screenshot = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) { screenshot.writeTo(Path.of(name)); }
        }
    }
    private static void check(boolean passed, String message) { if (!passed) throw new AssertionError(message); }
    private static void fail(Throwable failure) {
        failure.printStackTrace();
        try { Files.writeString(Path.of("cookpot-result.txt"), "FAIL: " + failure); } catch (Exception ignored) { }
        System.exit(1);
    }
}
