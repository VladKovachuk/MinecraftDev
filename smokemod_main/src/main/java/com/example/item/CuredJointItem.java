package com.example.item;

import com.example.effects.EffectManager;
import net.minecraft.server.network.ServerPlayerEntity;

/** A stronger, slowly developing effect from cured buds. */
public final class CuredJointItem extends JointItem {
    public static final float EFFECT_PER_PUFF = 0.60f;
    public static final float EFFECT_FADE_IN = 0.085f;
    public static final float EFFECT_FADE_OUT = 0.009f;

    public CuredJointItem(Settings settings) { super(settings); }

    @Override
    protected void applySmokingEffect(ServerPlayerEntity player) {
        EffectManager.addEffect(player, EffectManager.EffectType.CURED_JOINT,
                EFFECT_PER_PUFF, EFFECT_MAX, EFFECT_FADE_IN, EFFECT_FADE_OUT);
    }
}
