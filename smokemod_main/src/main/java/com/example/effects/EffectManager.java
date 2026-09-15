package com.example.effects;

import com.example.ExampleMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Обобщенный менеджер эффектов для управления визуальными эффектами от курения.
 * Поддерживает различные типы эффектов (серость, размытие, искажение и т.д.).
 */
public class EffectManager {

    public static final Identifier EFFECT_SYNC_ID = Identifier.of(ExampleMod.MOD_ID, "effect_sync");

    /** Пакет мгновенного сброса всех эффектов (смерть игрока и т.п.) */
    public static final Identifier EFFECT_CLEAR_ID = Identifier.of(ExampleMod.MOD_ID, "effect_clear");

    /** Типы эффектов */
    public enum EffectType {
        DESATURATION,  // Серость мира
        BLUR,          // Размытие
        DISTORTION,    // Искажение
        JOINT,         // Насыщенность, инерция камеры и motion blur
        CURED_JOINT     // Плавное раскачивание и дыхание изображения
    }

    /**
     * Данные о состоянии эффекта для передачи на клиент.
     */
    public static class EffectInstance {
        public float value;
        public float max;
        public float fadeInSpeed;
        public float fadeOutSpeed;

        public EffectInstance(float value, float max, float fadeIn, float fadeOut) {
            this.value = value;
            this.max = max;
            this.fadeInSpeed = fadeIn;
            this.fadeOutSpeed = fadeOut;
        }
    }

    /** Минимальный уровень эффекта */
    public static final float MIN_EFFECT = 0.0f;

    /**
     * Период убывания эффектов: каждые N тиков.
     */
    private static final int DECAY_PERIOD_TICKS = 20; // каждую секунду

    private static int tickCounter = 0;

    /** Хранение уровней эффектов для всех игроков */
    private static final Map<UUID, Map<EffectType, EffectInstance>> playerEffects = new HashMap<>();

    static float getEffectValue(ServerPlayerEntity player, EffectType type) {
        Map<EffectType, EffectInstance> effects = playerEffects.get(player.getUuid());
        EffectInstance effect = effects == null ? null : effects.get(type);
        return effect == null ? 0.0f : effect.value;
    }

    /**
     * Регистрирует обработчики событий для менеджера эффектов.
     */
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncToClient(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> playerEffects.remove(handler.player.getUuid()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            playerEffects.clear();
            tickCounter = 0;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Covers all deaths, including falling, drowning and /kill (not just combat).
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!player.isAlive() && playerEffects.containsKey(player.getUuid())) {
                    clearEffects(player);
                }
            }
            tickCounter++;

            // Убывание эффектов каждые 20 тиков (1 секунда)
            if (tickCounter >= DECAY_PERIOD_TICKS) {
                tickCounter = 0;
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (decayEffects(player)) {
                        syncToClient(player);
                    }
                }
            }
        });
    }

    /**
     * Добавляет или обновляет эффект игроку с заданными параметрами.
     */
    public static void addEffect(ServerPlayerEntity player, EffectType type, float amount, float max, float fadeInSpeed, float fadeOutSpeed) {
        Map<EffectType, EffectInstance> effects = playerEffects.computeIfAbsent(player.getUuid(), k -> new HashMap<>());

        EffectInstance instance = effects.get(type);
        if (instance == null) {
            instance = new EffectInstance(0, max, fadeInSpeed, fadeOutSpeed);
            effects.put(type, instance);
        }

        // Обновляем параметры (предмет может их менять)
        instance.max = max;
        instance.fadeInSpeed = fadeInSpeed;
        instance.fadeOutSpeed = fadeOutSpeed;

        // Добавляем значение
        instance.value = Math.min(instance.max, instance.value + amount);

        syncToClient(player);
    }

    /**
     * Уменьшает эффекты игрока со временем.
     * @return true если были изменения
     */
    private static boolean decayEffects(ServerPlayerEntity player) {
        Map<EffectType, EffectInstance> effects = playerEffects.get(player.getUuid());
        if (effects == null) return false;

        boolean changed = false;
        for (Map.Entry<EffectType, EffectInstance> entry : effects.entrySet()) {
            EffectInstance inst = entry.getValue();
            if (inst.value > MIN_EFFECT) {
                // Уменьшаем на скорость затухания (она в сек, так что за период DECAY_PERIOD_TICKS)
                inst.value = Math.max(MIN_EFFECT, inst.value - (inst.fadeOutSpeed * (DECAY_PERIOD_TICKS / 20.0f)));
                changed = true;
            }
        }

        // Удаляем эффекты с нулевым уровнем
        int sizeBefore = effects.size();
        effects.entrySet().removeIf(entry -> entry.getValue().value <= MIN_EFFECT);
        if (sizeBefore != effects.size()) changed = true;

        if (effects.isEmpty()) {
            playerEffects.remove(player.getUuid());
        }
        return changed;
    }

    /**
     * Синхронизирует уровни эффектов с клиентом.
     */
    public static void syncToClient(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        Map<EffectType, EffectInstance> effects = playerEffects.getOrDefault(player.getUuid(), new HashMap<>());

        buf.writeInt(effects.size());

        for (Map.Entry<EffectType, EffectInstance> entry : effects.entrySet()) {
            buf.writeString(entry.getKey().name());
            buf.writeFloat(entry.getValue().value);          // Целевое значение (исправлено)
            buf.writeFloat(entry.getValue().fadeInSpeed);   // Скорость появления
            buf.writeFloat(entry.getValue().fadeOutSpeed);  // Скорость затухания
        }

        ServerPlayNetworking.send(player, EFFECT_SYNC_ID, buf);
    }

    /**
     * Мгновенно очищает все эффекты игрока и отправляет на клиент сигнал
     * мгновенного сброса (без плавного затухания).
     * Используется при смерти игрока.
     *
     * @param player Игрок
     */
    public static void clearEffects(ServerPlayerEntity player) {
        playerEffects.remove(player.getUuid());
        // Сначала шлём мгновенный сброс
        PacketByteBuf clearBuf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, EFFECT_CLEAR_ID, clearBuf);
        // Затем синхронизируем нулевые значения
        syncToClient(player);
    }
}
