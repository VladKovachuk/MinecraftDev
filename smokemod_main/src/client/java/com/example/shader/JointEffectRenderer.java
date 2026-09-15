package com.example.shader;

import com.example.ExampleMod;
import com.example.mixin.client.PostEffectProcessorAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Cannabis colors/distortion plus a motion-dependent adaptation of the donor's frame history. */
public final class JointEffectRenderer {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final FadingEffect strength;
    private final FadingEffect curedStrength;
    private final JointCameraFilter cameraFilter = new JointCameraFilter();
    private PostEffectProcessor processor;
    private int width;
    private int height;
    private boolean historyValid;
    private ClientWorld lastWorld;
    private ClientPlayerEntity lastPlayer;
    private Perspective lastPerspective;
    private Vec3d lastPosition;
    private float lastYaw;
    private float lastPitch;
    private float seconds;
    private float animationSeconds;
    private long lastMouseNanos;

    public JointEffectRenderer(FadingEffect strength, FadingEffect curedStrength) {
        this.strength = strength;
        this.curedStrength = curedStrength;
    }

    private float combinedStrength() { return Math.max(strength.value(), curedStrength.value()); }

    public void reload(ResourceManager resources) {
        close();
        try {
            processor = new PostEffectProcessor(client.getTextureManager(), resources, client.getFramebuffer(),
                    Identifier.of(ExampleMod.MOD_ID, "shaders/post/joint.json"));
            width = height = 0;
        } catch (Exception e) {
            ExampleMod.LOGGER.error("Failed to load Joint post effect", e);
        }
    }

    public void update(float seconds) {
        this.seconds = seconds;
        if (lastWorld != client.world || lastPlayer != client.player || lastPerspective != client.options.getPerspective()) {
            resetMotion();
            lastWorld = client.world;
            lastPlayer = client.player;
            lastPerspective = client.options.getPerspective();
        }
        if (!canControlCamera()) resetMotion();
        if (combinedStrength() > 0.001f) animationSeconds += seconds;
        else {
            animationSeconds = 0.0f;
            resetMotion();
        }
    }

    private boolean canControlCamera() {
        return client.player != null && client.player.isAlive() && !client.player.isSleeping()
                && client.getCameraEntity() == client.player && client.currentScreen == null
                && !client.isPaused() && client.isWindowFocused() && client.mouse.isCursorLocked();
    }

    /** Vanilla has already applied sensitivity, inverted Y and the spyglass multiplier. */
    public void changeLookDirection(ClientPlayerEntity player, double deltaX, double deltaY) {
        if (combinedStrength() <= 0.001f || !canControlCamera()) {
            cameraFilter.reset();
            lastMouseNanos = 0;
            player.changeLookDirection(deltaX, deltaY);
            return;
        }
        long now = System.nanoTime();
        double dt = lastMouseNanos == 0 ? 1.0 / 60.0 : (now - lastMouseNanos) / 1_000_000_000.0;
        lastMouseNanos = now;
        cameraFilter.update(deltaX, deltaY, combinedStrength(), dt);
        player.changeLookDirection(cameraFilter.x(), cameraFilter.y());
        if (player.getPitch() >= 90.0f || player.getPitch() <= -90.0f) cameraFilter.clearPitch();
    }

    public void render(float tickDelta) {
        if (processor == null || client.world == null || client.player == null
                || !client.player.isAlive() || combinedStrength() <= 0.001f) {
            historyValid = false;
            return;
        }
        int newWidth = client.getWindow().getFramebufferWidth();
        int newHeight = client.getWindow().getFramebufferHeight();
        if (newWidth <= 0 || newHeight <= 0) return;
        // Resizing every frame destroys the previous-frame texture and reallocates GPU memory.
        if (width != newWidth || height != newHeight) {
            width = newWidth;
            height = newHeight;
            processor.setupDimensions(width, height);
            resetMotion();
        }

        var camera = client.gameRenderer.getCamera();
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        Vec3d position = camera.getPos();
        float yawDelta = MathHelper.wrapDegrees(yaw - lastYaw);
        float pitchDelta = pitch - lastPitch;
        if (lastPosition == null || lastPosition.squaredDistanceTo(position) > 16.0
                || Math.abs(yawDelta) > 60.0f || Math.abs(pitchDelta) > 60.0f) {
            resetMotion();
        }
        boolean movingView = historyValid && canControlCamera() && seconds > 0.00001f;
        float yawSpeed = movingView ? yawDelta / seconds : 0.0f;
        float pitchSpeed = movingView ? pitchDelta / seconds : 0.0f;
        float motion = MathHelper.clamp((float) Math.hypot(yawSpeed, pitchSpeed) / 140.0f, 0.0f, 1.0f);
        float value = combinedStrength();
        float historyWeight = motion > 0.015f
                ? (float) Math.pow(0.72f * value * motion, seconds * 60.0f) : 0.0f;
        ShaderManager.setUniform(processor, "Strength", value);
        ShaderManager.setUniform(processor, "CuredStrength", curedStrength.value());
        ShaderManager.setUniform(processor, "EffectTime", animationSeconds);
        ShaderManager.setUniform(processor, "HistoryWeight", Math.min(0.96f, historyWeight));
        for (var pass : ((PostEffectProcessorAccessor) processor).getPasses()) {
            var direction = pass.getProgram().getUniformByName("MotionDirection");
            if (direction != null) {
                direction.set(MathHelper.clamp(yawSpeed / 180.0f * 0.025f * value, -0.06f, 0.06f),
                        MathHelper.clamp(-pitchSpeed / 180.0f * 0.025f * value, -0.06f, 0.06f));
            }
        }
        ShaderManager.renderProcessor(processor, tickDelta);
        historyValid = canControlCamera();
        lastYaw = yaw;
        lastPitch = pitch;
        lastPosition = position;
    }

    public void resetMotion() {
        historyValid = false;
        lastPosition = null;
        cameraFilter.reset();
        lastMouseNanos = 0;
    }

    public void reset() {
        resetMotion();
        lastWorld = null;
        lastPlayer = null;
        lastPerspective = null;
        animationSeconds = seconds = 0.0f;
    }

    public void close() {
        if (processor != null) {
            processor.close();
            processor = null;
        }
        reset();
    }
}
