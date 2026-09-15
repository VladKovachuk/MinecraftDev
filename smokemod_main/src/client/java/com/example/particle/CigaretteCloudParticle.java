package com.example.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class CigaretteCloudParticle extends SpriteBillboardParticle {

	private final SpriteProvider spriteProvider;
	private final float baseScale;

	protected CigaretteCloudParticle(ClientWorld world, double x, double y, double z, double velocityX,
			double velocityY, double velocityZ, SpriteProvider spriteProvider) {
		super(world, x, y, z, velocityX, velocityY, velocityZ);
		this.spriteProvider = spriteProvider;
		this.velocityMultiplier = 0.96F; // сопротивление воздуха

		// БАЗОВЫЙ класс Particle в майнкрафте по умолчанию добавляет +0.1 к Y (подлет
		// вверх)
		// и пересчитывает вектор. Нам нужно отменить этот бред и поставить точную
		// скорость:
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;

		// Отключаем гравитацию на всякий случай
		this.gravityStrength = 0.0F;

		// Стартовый размер частицы очень маленький (для струйки дыма изо рта)
		this.baseScale = 0.09F; // или 0.1
		this.scale = this.baseScale;

		// Чисто белый цвет дыма
		this.red = 1.0F;
		this.green = 1.0F;
		this.blue = 1.0F;

		// Время жизни
		this.maxAge = (int) (8.0D / (this.random.nextDouble() * 0.8D + 0.2D)) + 140; // 80
		this.setSpriteForAge(spriteProvider);
	}

	@Override
	public ParticleTextureSheet getType() {
		return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick();

		// Плавно увеличиваем размер частицы со временем, чтобы она "клубилась" и
		// рассеивалась
		float lifeRatio = (float) this.age / (float) this.maxAge;
		this.scale = this.baseScale + (0.50F * lifeRatio); // 0.35F

		// Плавно делаем частицу прозрачной под конец
		this.alpha = 1.0F - (lifeRatio * lifeRatio);

		if (!this.dead) {
			this.setSpriteForAge(this.spriteProvider);
		}
	}

	public static class Factory implements ParticleFactory<DefaultParticleType> {
		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(DefaultParticleType defaultParticleType, ClientWorld clientWorld, double x,
				double y, double z, double velocityX, double velocityY, double velocityZ) {
			return new CigaretteCloudParticle(clientWorld, x, y, z, velocityX, velocityY, velocityZ,
					this.spriteProvider);
		}
	}
}
