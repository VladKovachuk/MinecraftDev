package com.example.item;

import com.example.ExampleMod;
import com.example.effects.EffectManager;
import com.example.nicotine.NicotineManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Предмет сигареты для Smoke Mod.
 * Можно надеть в слот шлема (головы).
 * Имеет механику "натягивания" (затяжки) как у лука.
 */
public class CigaretteItem extends Item implements Equipment {

	/** Кулдаун после использования: 1 секунды = 20 тиков */
	private static final int COOLDOWN_TICKS = 20;

	// --- ПАРАМЕТРЫ ЭФФЕКТА СЕРОСТИ ДЛЯ ЭТОГО ПРЕДМЕТА ---
	protected float getDesatPuffAmount() { return 0.22f; }  // Сколько добавляет одна затяжка
	protected float getDesatMax() { return 0.9f; }         // Максимальный уровень серости
	protected float getDesatFadeIn() { return 0.2f; }      // Скорость появления (ед/сек)
	protected float getDesatFadeOut() { return 0.02f; }    // Скорость затухания (ед/сек)
	// ----------------------------------------------------

	/** Called once on the server after a completed puff; subclasses select their effect. */
	protected void applySmokingEffect(ServerPlayerEntity player) {
		EffectManager.addEffect(player, EffectManager.EffectType.DESATURATION,
				getDesatPuffAmount(), getDesatMax(), getDesatFadeIn(), getDesatFadeOut());
	}

	public CigaretteItem(Settings settings) {

		super(settings);
	}

	/**
	 * Возвращает количество никотина, которое добавляется при затяжке этой
	 * сигареты.
	 * Переопредели этот метод в подклассах для разных типов сигарет.
	 * 
	 * @return Количество никотина (по умолчанию 20%)
	 */
	protected int getNicotineAmount() {
		return NicotineManager.ADD_PER_PUFF; // 20% по умолчанию
	}

	/**
	 * Сообщаем игре, что этот предмет относится к слоту головы.
	 */
	@Override
	public EquipmentSlot getSlotType() {
		return EquipmentSlot.HEAD;
	}

	/**
	 * Вызывается при ПКМ с сигаретой в руке.
	 * Начинает анимацию "затяжки" (аналог натягивания лука).
	 */
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack itemStack = user.getStackInHand(hand);

		// Не начинаем использование, если предмет на кулдауне
		if (user.getItemCooldownManager().isCoolingDown(this)) {
			return TypedActionResult.pass(itemStack);
		}

		// Проигрываем звук затяжки при начале использования (на сервере для
		// синхронизации)
		if (!world.isClient) {
			world.playSound(null, user.getBlockPos(),
					ExampleMod.SOUND_CIGARETTE, SoundCategory.PLAYERS, 1.0f, 1.0f);
		}

		// Начинаем использование предмета (анимация натягивания)
		user.setCurrentHand(hand);
		return TypedActionResult.consume(itemStack);
	}

	/**
	 * Определяет длительность использования (в тиках).
	 * 20 тиков = 1 секунда.
	 * Здесь можно настроить, как долго нужно "затягиваться".
	 */
	@Override
	public int getMaxUseTime(ItemStack stack) {
		return 50; // 2.5 секунды
	}

	/**
	 * Определяет, как предмет используется (как еда/напиток - можно есть/пить).
	 * Используем UseAction.BOW для анимации как у лука.
	 */
	@Override
	public net.minecraft.util.UseAction getUseAction(ItemStack stack) {
		return net.minecraft.util.UseAction.BOW; // Анимация как у лука
	}

	/**
	 * Вызывается, когда игрок отпускает ПКМ после использования.
	 * Здесь будет логика "выдоха" или эффектов сигареты.
	 */
	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (user instanceof PlayerEntity player) {
			// При первом использовании помечаем сигарету как зажженную
			if (!world.isClient) {
				var nbt = stack.getOrCreateNbt();
				if (!nbt.getBoolean("lit")) {
					nbt.putBoolean("lit", true);
				}
			}

			// Звук выдоха после использования сигареты
			if (!world.isClient) {
				world.playSound(null, user.getBlockPos(),
						ExampleMod.SOUND_EXHALATION, SoundCategory.PLAYERS, 1.0f, 1.0f);
			}

			if (!world.isClient) {
				// Вместо мгновенного спавна запускаем 3-секундный выдох
				if (user instanceof ServerPlayerEntity serverPlayer) {
					ExhalationManager.startExhalation(serverPlayer);
				}
			}

			// Пополнение индикатора лёгких при выдохе (значение зависит от типа сигареты)
			if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
				NicotineManager.addPuff(serverPlayer, getNicotineAmount());

				applySmokingEffect(serverPlayer);
			}

			// Ставим кулдаун 4 секунды после использования
			player.getItemCooldownManager().set(this, COOLDOWN_TICKS);

			if (!player.getAbilities().creativeMode) {
				stack.damage(20, user, (entity) -> {
					if (entity instanceof PlayerEntity) {
						((PlayerEntity) entity).sendToolBreakStatus(user.getActiveHand());
					}
				});
			}
		}

		return stack;
	}

	/**
	 * Вызывается, когда игрок прерывает использование (отпустил ПКМ раньше).
	 * Кулдаун не ставим. Останавливаем звук затяжки у этого игрока.
	 */
	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (world.isClient)
			return;
		if (remainingUseTicks >= getMaxUseTime(stack))
			return; // Полное использование — не сюда
		// Останавливаем звук затяжки у игрока
		if (user instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.networkHandler.sendPacket(new StopSoundS2CPacket(
					ExampleMod.SOUND_CIGARETTE_ID, SoundCategory.PLAYERS));
		}
	}

}
