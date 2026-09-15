package com.example.shader;

/** A server target with frame-rate-independent visual fade, in units per second. */
public final class FadingEffect {
    private float value;
    private float target;
    private float fadeIn;
    private float fadeOut;

    public FadingEffect(float fadeIn, float fadeOut) {
        setTarget(0.0f, fadeIn, fadeOut);
    }

    public void setTarget(float target, float fadeIn, float fadeOut) {
        this.target = Float.isFinite(target) ? Math.max(0.0f, Math.min(1.0f, target)) : 0.0f;
        this.fadeIn = validSpeed(fadeIn);
        this.fadeOut = validSpeed(fadeOut);
    }

    private static float validSpeed(float speed) {
        return Float.isFinite(speed) && speed > 0.0f ? speed : 0.01f;
    }

    public void clearTarget() {
        target = 0.0f;
    }

    public void update(float seconds) {
        if (!Float.isFinite(seconds) || seconds <= 0.0f) return;
        if (value < target) value = Math.min(target, value + fadeIn * seconds);
        else value = Math.max(target, value - fadeOut * seconds);
    }

    public void reset() {
        value = target = 0.0f;
    }

    public float value() { return value; }
    public float target() { return target; }
}
