package com.example.item;

import com.example.effects.EffectManager;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Joint keeps the cigarette's smoking mechanics, with its own cannabis effect.
 */
public class JointItem extends CigaretteItem {

	// Units per completed puff / second. Several puffs build up to the maximum.
	public static final float EFFECT_PER_PUFF = 0.38f;  // сколько силы добавляет полная затяжка
	public static final float EFFECT_MAX = 1.0f;  // предел накопления
	public static final float EFFECT_FADE_IN = 0.22f; // скорость визуального появления
	public static final float EFFECT_FADE_OUT = 0.012f; // скорость серверного затухания в секунду

	public JointItem(Settings settings) {
		super(settings);
	}

	@Override
	protected void applySmokingEffect(ServerPlayerEntity player) {
		EffectManager.addEffect(player, EffectManager.EffectType.JOINT,
				EFFECT_PER_PUFF, EFFECT_MAX, EFFECT_FADE_IN, EFFECT_FADE_OUT);
	}
}
