package com.example.shader;

import com.example.ExampleMod;
import com.example.item.JointItem;
import com.example.item.CuredJointItem;
import com.example.mixin.client.PostEffectProcessorAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

/** Owns cigarette desaturation and the independent Joint world/camera effect. */
public class ShaderManager implements SimpleSynchronousResourceReloadListener {
    private static final Identifier ID = Identifier.of(ExampleMod.MOD_ID, "shader_manager");
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final FadingEffect desaturation = new FadingEffect(0.2f, 0.02f);
    private final FadingEffect joint = new FadingEffect(JointItem.EFFECT_FADE_IN, JointItem.EFFECT_FADE_OUT);
    private final FadingEffect curedJoint = new FadingEffect(CuredJointItem.EFFECT_FADE_IN, CuredJointItem.EFFECT_FADE_OUT);
    private final JointEffectRenderer jointRenderer = new JointEffectRenderer(joint, curedJoint);
    private PostEffectProcessor shaderEffect;
    private long lastUpdateNanos;
    private int width;
    private int height;
    private float blur;
    private float distortion;

    @Override
    public Identifier getFabricId() { return ID; }

    @Override
    public void reload(ResourceManager manager) {
        close();
        try {
            shaderEffect = new PostEffectProcessor(client.getTextureManager(), manager, client.getFramebuffer(),
                    Identifier.of(ExampleMod.MOD_ID, "shaders/post/desaturation.json"));
            width = height = 0;
        } catch (Exception e) {
            ExampleMod.LOGGER.error("Failed to load cigarette desaturation", e);
        }
        jointRenderer.reload(manager);
        lastUpdateNanos = 0;
    }

    /** Called once at the start of a rendered frame, independently of shader availability. */
    public void updateUniforms(float tickDelta) {
        long now = System.nanoTime();
        float seconds = lastUpdateNanos == 0 ? 0.0f : Math.min(0.1f, (now - lastUpdateNanos) / 1_000_000_000.0f);
        lastUpdateNanos = now;
        if (client.world == null || client.player == null || !client.player.isAlive()) {
            reset();
            return;
        }
        if (client.isPaused()) seconds = 0.0f;
        desaturation.update(seconds);
        joint.update(seconds);
        curedJoint.update(seconds);
        jointRenderer.update(seconds);
    }

    /** Joint affects the rendered world and hand, before the HUD and screens are drawn. */
    public void renderJoint(float tickDelta) {
        jointRenderer.render(tickDelta);
    }

    /** Retains the cigarette's existing final-frame desaturation. */
    public void render(float tickDelta) {
        if (shaderEffect == null || desaturation.value() <= 0.001f || client.world == null) return;
        int newWidth = client.getWindow().getFramebufferWidth();
        int newHeight = client.getWindow().getFramebufferHeight();
        if (newWidth <= 0 || newHeight <= 0) return;
        if (width != newWidth || height != newHeight) {
            width = newWidth;
            height = newHeight;
            shaderEffect.setupDimensions(width, height);
        }
        setUniform(shaderEffect, "desaturation", desaturation.value());
        renderProcessor(shaderEffect, tickDelta);
    }

    static void setUniform(PostEffectProcessor processor, String name, float value) {
        for (var pass : ((PostEffectProcessorAccessor) processor).getPasses()) {
            var uniform = pass.getProgram().getUniformByName(name);
            if (uniform != null) uniform.set(value);
        }
    }

    static void renderProcessor(PostEffectProcessor processor, float tickDelta) {
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            processor.render(tickDelta);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
        }
    }

    /** Sync packets are full snapshots: omitted effects must fade out too. */
    public void clearTargets() {
        desaturation.clearTarget();
        joint.clearTarget();
        curedJoint.clearTarget();
        blur = distortion = 0.0f;
    }

    public void setDesaturationParams(float target, float fadeIn, float fadeOut) {
        desaturation.setTarget(target, fadeIn, fadeOut);
    }

    public void setJointParams(float target, float fadeIn, float fadeOut) {
        joint.setTarget(target, fadeIn, fadeOut);
    }

    public JointEffectRenderer getJointRenderer() { return jointRenderer; }

    public void setCuredJointParams(float target, float fadeIn, float fadeOut) {
        curedJoint.setTarget(target, fadeIn, fadeOut);
    }

    public void setDesaturation(float value) { setDesaturationParams(value, 0.2f, 0.02f); }
    public void setInstantReset(boolean reset) { if (reset) reset(); }
    public void setBlur(float value) { blur = Math.max(0.0f, Math.min(1.0f, value)); }
    public void setDistortion(float value) { distortion = Math.max(0.0f, Math.min(1.0f, value)); }
    public float getDesaturation() { return desaturation.value(); }
    public float getTargetDesaturation() { return desaturation.target(); }
    public float getBlur() { return blur; }
    public float getDistortion() { return distortion; }

    public void reset() {
        clearTargets();
        desaturation.reset();
        joint.reset();
        curedJoint.reset();
        jointRenderer.reset();
        lastUpdateNanos = 0;
    }

    public void close() {
        if (shaderEffect != null) {
            shaderEffect.close();
            shaderEffect = null;
        }
        jointRenderer.close();
    }
}
