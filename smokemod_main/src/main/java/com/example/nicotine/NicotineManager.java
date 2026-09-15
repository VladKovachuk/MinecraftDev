package com.example.nicotine;

import com.example.ExampleMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Управление уровнем никотина (лёгких) игрока.
 * 0–100%, по умолчанию 100. Убывает со временем, пополняется курением.
 * 
 * Данные сохраняются на диске через NicotineState (PersistentState)
 * и переживают перезапуски сервера.
 * 
 * Для разных типов сигарет используй addPuff(player, amount) с собственным
 * значением.
 */
public class NicotineManager {

	public static final Identifier NICOTINE_SYNC_ID = Identifier.of(ExampleMod.MOD_ID, "nicotine_sync");

	/** Максимальный уровень никотина */
	public static final int MAX = 100;
	/** Минимальный уровень никотина */
	public static final int MIN = 0;
	/** Стартовый уровень при первом входе */
	public static final int DEFAULT = 100;
	/** Пополнение за одну затяжку обычной сигареты (по умолчанию 20) */
	public static final int ADD_PER_PUFF = 20;
	/** Период убывания: 20 тиков = 1 секунд. СТАНДАРТ 120 ТИКОВ = 6 СЕКУНД */
	public static final int DECAY_PERIOD_TICKS = 120;
	/** Период урона: 40 тиков = 2 секунды */
	public static final int DAMAGE_PERIOD_TICKS = 40;

	private static int tickCounter = 0;
	private static int damageTickCounter = 0;

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickCounter++;
			damageTickCounter++;

			// Убывание: каждые 200 тиков -1
			if (tickCounter >= DECAY_PERIOD_TICKS) {
				tickCounter = 0;
				for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
					int level = getLevel(player);
					if (level > MIN) {
						setLevel(player, level - 1);
					}
					syncToClient(player); // синхронизация HUD каждые 10 сек
				}
			}

			// Урон при 0: каждые 40 тиков (2 сек)
			if (damageTickCounter >= DAMAGE_PERIOD_TICKS) {
				damageTickCounter = 0;
				for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
					if (player.isCreative() || player.isSpectator())
						continue;
					int level = getLevel(player);
					if (level <= MIN) {
						player.damage(player.getWorld().getDamageSources().wither(), 1.0f); // урон от Wither
						player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0)); // эффект Wither

					}
				}
			}
		});
	}

	public static int getLevel(ServerPlayerEntity player) {
		return NicotineState.getServerState(player.getServer())
				.getLevel(player.getUuid(), DEFAULT);
	}

	public static void setLevel(ServerPlayerEntity player, int level) {
		int clamped = Math.max(MIN, Math.min(MAX, level));
		NicotineState.getServerState(player.getServer())
				.setLevel(player.getUuid(), clamped);
	}

	/**
	 * Добавляет никотин игроку на указанное количество.
	 * Используй этот метод для разных типов сигарет с разными значениями.
	 * 
	 * @param player Игрок
	 * @param amount Количество никотина для добавления (например, 20 для обычной
	 *               сигареты)
	 */
	public static void addNicotine(ServerPlayerEntity player, int amount) {
		int current = getLevel(player);
		setLevel(player, current + amount);
		syncToClient(player);
	}

	/**
	 * Добавляет никотин игроку при затяжке сигареты.
	 * Использует значение по умолчанию (ADD_PER_PUFF = 20).
	 * 
	 * @param player Игрок
	 */
	public static void addPuff(ServerPlayerEntity player) {
		addNicotine(player, ADD_PER_PUFF);
	}

	/**
	 * Добавляет никотин игроку при затяжке с кастомным значением.
	 * Используй для разных типов сигарет (например, сигарета = 20, сигара = 40).
	 * 
	 * @param player Игрок
	 * @param amount Количество никотина для добавления
	 */
	public static void addPuff(ServerPlayerEntity player, int amount) {
		addNicotine(player, amount);
	}

	public static void onPlayerJoin(ServerPlayerEntity player) {
		NicotineState state = NicotineState.getServerState(player.getServer());
		// Инициализируем уровень только если игрок заходит впервые
		if (!state.hasPlayer(player.getUuid())) {
			state.setLevel(player.getUuid(), DEFAULT);
		}
		syncToClient(player);
	}

	public static void syncToClient(ServerPlayerEntity player) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(getLevel(player));
		ServerPlayNetworking.send(player, NICOTINE_SYNC_ID, buf);
	}
}
