package com.example.effects;

import com.example.shader.FadingEffect;
import com.example.shader.JointCameraFilter;
import com.example.item.CuredJointItem;

/** Deterministic behavioral tests; no running game or OpenGL required. */
public final class JointEffectRegressionTest {
    public static void main(String[] args) {
        for (int fps : new int[]{30, 60, 144, 240}) {
            FadingEffect cured = new FadingEffect(CuredJointItem.EFFECT_FADE_IN, CuredJointItem.EFFECT_FADE_OUT);
            cured.setTarget(CuredJointItem.EFFECT_PER_PUFF, CuredJointItem.EFFECT_FADE_IN, CuredJointItem.EFFECT_FADE_OUT);
            for (int i = 0; i < fps; i++) cured.update(1.0f / fps);
            near(cured.value(), 0.085, 0.00002, "Cured onset must develop slowly at " + fps + " FPS");
            cured.clearTarget();
            for (int i = 0; i < fps; i++) cured.update(1.0f / fps);
            near(cured.value(), 0.076, 0.00002, "Cured fade-out must remain gentle");
            cured.reset();
            near(cured.value() + cured.target(), 0, 0, "Cured profile resets immediately");
            FadingEffect effect = new FadingEffect(0.22f, 0.012f);
            effect.setTarget(0.38f, 0.22f, 0.012f);
            for (int i = 0; i < fps; i++) effect.update(1.0f / fps);
            near(effect.value(), 0.22, 0.00002, "One-second fade at " + fps + " FPS");
            effect.update(0.0f);
            near(effect.value(), 0.22, 0.00002, "Pause freezes the effect");
            effect.clearTarget();
            for (int i = 0; i < fps * 20; i++) effect.update(1.0f / fps);
            near(effect.value(), 0, 0, "An omitted snapshot effect must reach exactly zero");
            effect.setTarget(5, 10, 10);
            effect.update(1);
            near(effect.value(), 1, 0, "Repeated puffs are bounded");
            effect.reset();
            near(effect.value() + effect.target(), 0, 0, "Death/disconnect clears current and target");
        }

        JointCameraFilter filter = new JointCameraFilter();
        filter.update(12, -8, 0, 1.0 / 60);
        near(filter.x(), 12, 0, "Unaffected X input");
        near(filter.y(), -8, 0, "Unaffected inverted Y input");
        filter.update(12, -8, 1, 1.0 / 60);
        check(filter.x() > 0 && filter.x() < 6, "Initial response must be slowed");
        filter.update(0, 0, 1, 1.0 / 60);
        check(filter.x() > 0 && filter.y() < 0, "Motion must continue briefly after input stops");
        filter.clearPitch();
        filter.update(0, 0, 1, 1.0 / 60);
        near(filter.y(), 0, 0, "Pitch must not stick after reaching the vertical limit");
        filter.reset();
        filter.update(0, 0, 1, 1.0 / 60);
        near(filter.x() + filter.y(), 0, 0, "Reset discards camera inertia");
        filter.update(100, 0, 1, 1.0 / 60);
        filter.update(0, 0, 1, 2.0);
        near(filter.x(), 0, 0, "A long stall cannot release queued motion");
        filter.update(100, 0, 1, 1.0 / 60);
        filter.update(0, 0, 0, 1.0 / 60);
        near(filter.x(), 0, 0, "Expired effect restores normal input immediately");

        double reference = sweep(60, 1);
        for (int fps : new int[]{30, 144, 240}) {
            near(sweep(fps, 1), reference, 0.000001, "Inertia independent of FPS: " + fps);
        }
        near(sweep(60, 5), 120 * 0.65, 0.0001, "Camera settles without losing delayed input");
        System.out.println("Joint regression checks passed: fade, snapshot removal, reset, camera inertia, 30/60/144/240 FPS.");
    }

    private static double sweep(int fps, int seconds) {
        JointCameraFilter filter = new JointCameraFilter();
        double rotation = 0;
        for (int frame = 0; frame < fps * seconds; frame++) {
            filter.update(frame < fps ? 120.0 / fps : 0, 0, 1, 1.0 / fps);
            rotation += filter.x();
        }
        return rotation;
    }

    private static void near(double actual, double expected, double tolerance, String message) {
        check(Math.abs(actual - expected) <= tolerance, message + ": " + actual + " != " + expected);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
