package com.example.shader;

import com.example.ExampleMod;
import com.example.mixin.client.PostEffectProcessorAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runs the real Minecraft JSON pipeline on the GPU, then exits the isolated test client. */
public final class JointShaderSmokeTest implements ClientModInitializer {
    private boolean ran;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ran || client.getOverlay() != null) return;
            ran = true;
            try {
                verify(client);
                Files.writeString(Path.of("joint-shader-result.txt"), "PASS: cured models/tint, GPU loading, ordinary no-twist, cured waves, color, history, resize, cigarette compatibility.\n");
                System.out.println("JOINT_SHADER_SMOKE_TEST_PASSED");
                client.scheduleStop();
            } catch (Throwable failure) {
                failure.printStackTrace();
                try { Files.writeString(Path.of("joint-shader-result.txt"), "FAIL: " + failure); }
                catch (Exception ignored) { }
                // A nonzero exit makes Gradle report the graphics test failure.
                System.exit(1);
            }
        });
    }

    private static void verify(MinecraftClient client) throws Exception {
        ItemStack curedStack = new ItemStack(ExampleMod.CURED_JOINT);
        var unlitModel = client.getItemRenderer().getModel(curedStack, null, null, 0);
        check(unlitModel != client.getBakedModelManager().getMissingModel(), "Cured model is missing");
        check(unlitModel.getQuads(null, null, Random.create()).stream().anyMatch(quad -> quad.hasColor()),
                "Cured model must apply the paper tint");
        check(ColorProviderRegistry.ITEM.get(ExampleMod.CURED_JOINT).getColor(curedStack, 0) == 0xB9C58C,
                "Cured paper has the wrong shade");
        curedStack.getOrCreateNbt().putBoolean("lit", true);
        var litModel = client.getItemRenderer().getModel(curedStack, null, null, 0);
        check(litModel != unlitModel && litModel != client.getBakedModelManager().getMissingModel(),
                "Lit cured joint must resolve to its own valid model");
        check(litModel.getQuads(null, null, Random.create()).stream().anyMatch(quad -> quad.hasColor()),
                "Lighting the cured joint must preserve the paper tint");
        int width = client.getFramebuffer().textureWidth;
        int height = client.getFramebuffer().textureHeight;
        try (PostEffectProcessor joint = load(client, "joint");
             PostEffectProcessor cigarette = load(client, "desaturation")) {
            joint.setupDimensions(width, height);
            cigarette.setupDimensions(width, height);
            pattern(client, width, height);
            byte[] original = pixels(client, width, height);
            save(client, "joint-before.png");
            ShaderManager.setUniform(joint, "Strength", 0);
            ShaderManager.setUniform(joint, "HistoryWeight", 0);
            ShaderManager.renderProcessor(joint, 0);
            check(difference(original, pixels(client, width, height)) < 0.01, "Zero effect changes or flips the image");

            pattern(client, width, height);
            ShaderManager.setUniform(joint, "Strength", 1);
            ShaderManager.setUniform(joint, "EffectTime", 2.4f);
            ShaderManager.renderProcessor(joint, 0);
            byte[] ordinary = pixels(client, width, height);
            check(difference(original, ordinary) > 5, "Ordinary colors have no visible effect");
            save(client, "joint-ordinary.png");
            pattern(client, width, height);
            ShaderManager.setUniform(joint, "EffectTime", 8.0f);
            ShaderManager.renderProcessor(joint, 0);
            check(difference(ordinary, pixels(client, width, height)) < 0.01,
                    "Ordinary joint must not animate, twist or breathe while stationary");

            pattern(client, width, height);
            ShaderManager.setUniform(joint, "CuredStrength", 1);
            ShaderManager.setUniform(joint, "EffectTime", 2.4f);
            ShaderManager.renderProcessor(joint, 0);
            byte[] cured = pixels(client, width, height);
            check(difference(ordinary, cured) > 3, "Cured profile must add visible deformation");
            save(client, "joint-active.png");
            pattern(client, width, height);
            ShaderManager.setUniform(joint, "EffectTime", 12.0f);
            ShaderManager.renderProcessor(joint, 0);
            check(difference(cured, pixels(client, width, height)) > 3, "Cured waves must move in time");
            ShaderManager.setUniform(joint, "CuredStrength", 0);

            fill(client, 0.15f, 0.4f, 0.1f);
            ShaderManager.renderProcessor(joint, 0);
            byte[] color = pixels(client, 1, 1);
            check(unsigned(color[1]) > 150 && unsigned(color[1]) > unsigned(color[0]) * 3,
                    "Cannabis must brighten and intensify green");

            // A stationary frame never samples old colors; moving frames retain a visible trail.
            ShaderManager.setUniform(joint, "Strength", 0);
            fill(client, 0, 0, 1);
            ShaderManager.renderProcessor(joint, 0);
            fill(client, 1, 0, 0);
            ShaderManager.setUniform(joint, "HistoryWeight", 0.5f);
            ShaderManager.renderProcessor(joint, 0);
            color = pixels(client, 1, 1);
            check(Math.abs(unsigned(color[0]) - 128) <= 2 && Math.abs(unsigned(color[2]) - 128) <= 2,
                    "Previous-frame history must blend red and blue");
            fill(client, 0, 1, 0);
            ShaderManager.setUniform(joint, "HistoryWeight", 0);
            ShaderManager.renderProcessor(joint, 0);
            color = pixels(client, 1, 1);
            check(unsigned(color[0]) == 0 && unsigned(color[1]) == 255 && unsigned(color[2]) == 0,
                    "Stationary/reset frame retains ghosting");

            pattern(client, width, height);
            for (var pass : ((PostEffectProcessorAccessor) joint).getPasses()) {
                var direction = pass.getProgram().getUniformByName("MotionDirection");
                if (direction != null) direction.set(0.04f, 0.02f);
            }
            ShaderManager.renderProcessor(joint, 0);
            check(difference(original, pixels(client, width, height)) > 2, "Directional camera blur has no effect");

            client.getFramebuffer().resize(320, 240, MinecraftClient.IS_SYSTEM_MAC);
            joint.setupDimensions(320, 240);
            fill(client, 0, 1, 0);
            ShaderManager.renderProcessor(joint, 0);
            color = pixels(client, 1, 1);
            check(unsigned(color[1]) == 255 && unsigned(color[0]) == 0, "Resize reads stale or black history");
            client.getFramebuffer().resize(width, height, MinecraftClient.IS_SYSTEM_MAC);

            fill(client, 1, 0, 0);
            ShaderManager.setUniform(cigarette, "desaturation", 1);
            ShaderManager.renderProcessor(cigarette, 0);
            color = pixels(client, 1, 1);
            check(Math.abs(unsigned(color[0]) - 76) <= 1 && color[0] == color[1] && color[1] == color[2],
                    "Existing cigarette grayscale changed");
            check(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL reported an error");
        }
    }

    private static PostEffectProcessor load(MinecraftClient client, String name) throws Exception {
        return new PostEffectProcessor(client.getTextureManager(), client.getResourceManager(), client.getFramebuffer(),
                Identifier.of("smokemod", "shaders/post/" + name + ".json"));
    }

    private static void fill(MinecraftClient client, float r, float g, float b) {
        client.getFramebuffer().beginWrite(true);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glClearColor(r, g, b, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    private static void pattern(MinecraftClient client, int width, int height) {
        fill(client, 0.15f, 0.25f, 0.35f);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        for (int y = 0; y < height; y += 24) {
            for (int x = 0; x < width; x += 24) {
                GL11.glScissor(x, y, 24, 24);
                GL11.glClearColor((x / 24 % 3) * 0.25f + 0.1f,
                        (y / 24 % 3) * 0.25f + 0.1f, ((x + y) / 24 % 2) * 0.4f + 0.1f, 1);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static byte[] pixels(MinecraftClient client, int width, int height) {
        client.getFramebuffer().beginWrite(false);
        ByteBuffer bytes = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bytes);
        byte[] result = new byte[bytes.remaining()];
        bytes.get(result);
        return result;
    }

    private static void save(MinecraftClient client, String name) throws Exception {
        try (var image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            image.writeTo(Path.of(name));
        }
    }

    private static double difference(byte[] a, byte[] b) {
        long sum = 0;
        for (int i = 0; i < a.length; i++) sum += Math.abs(unsigned(a[i]) - unsigned(b[i]));
        return sum / (double) a.length;
    }

    private static int unsigned(byte value) { return value & 255; }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
