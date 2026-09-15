package com.example.shader;

/**
 * Adapts Psychedelicraft's Cannabis headMotionInertness = strength * 8 and
 * getSmoothVision = 1 / (1 + inertness). Integrates delayed mouse movement in
 * seconds so the camera reacts the same way at different frame rates.
 */
public final class JointCameraFilter {
    private double pendingX;
    private double pendingY;
    private double x;
    private double y;

    public void update(double inputX, double inputY, float strength, double seconds) {
        if (strength <= 0.001f || !Float.isFinite(strength)) {
            reset();
            x = inputX;
            y = inputY;
            return;
        }
        // Discard queued motion after a pause / stall rather than jumping on resume.
        if (!Double.isFinite(seconds) || seconds <= 0.0 || seconds > 0.25) {
            reset();
            seconds = 1.0 / 60.0;
        }
        double s = Math.min(1.0, strength);
        double sensitivity = 1.0 - 0.35 * s;
        double direct = 1.0 / (1.0 + s * 8.0);
        double tau = 0.035 + 0.28 * s;
        double release = -Math.expm1(-seconds / tau);
        // Exact first-order integration for constant input velocity during this frame.
        double inputRelease = 1.0 - tau * release / seconds;
        double delayedX = inputX * sensitivity * (1.0 - direct);
        double delayedY = inputY * sensitivity * (1.0 - direct);
        x = inputX * sensitivity * direct + pendingX * release + delayedX * inputRelease;
        y = inputY * sensitivity * direct + pendingY * release + delayedY * inputRelease;
        pendingX += delayedX - (pendingX * release + delayedX * inputRelease);
        pendingY += delayedY - (pendingY * release + delayedY * inputRelease);
    }

    public void reset() {
        pendingX = pendingY = x = y = 0.0;
    }

    public void clearPitch() { pendingY = 0.0; }
    public double x() { return x; }
    public double y() { return y; }
}
