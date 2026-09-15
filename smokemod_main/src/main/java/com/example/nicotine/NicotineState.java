package com.example.nicotine;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Хранит уровни никотина всех игроков на диске (в папке data мира).
 * Используется как единственный источник истины вместо Map в памяти.
 */
public class NicotineState extends PersistentState {

    private static final String KEY = "smokemod_nicotine";

    /** UUID игрока → уровень никотина (0–100) */
    private final Map<UUID, Integer> levels = new HashMap<>();

    // -----------------------------------------------------------------------
    // Чтение из NBT (вызывается при загрузке мира)
    // -----------------------------------------------------------------------
    public static NicotineState fromNbt(NbtCompound nbt) {
        com.example.ExampleMod.LOGGER.info("NicotineState: Loading from NBT...");
        NicotineState state = new NicotineState();
        NbtCompound data = nbt.getCompound("players");
        for (String key : data.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                int level = data.getInt(key);
                state.levels.put(uuid, level);
                com.example.ExampleMod.LOGGER.info("NicotineState: Loaded player {} with level {}", uuid, level);
            } catch (IllegalArgumentException ignored) {
                // Пропускаем некорректные UUID (не должно происходить)
            }
        }
        return state;
    }

    // -----------------------------------------------------------------------
    // Запись в NBT (вызывается при сохранении мира)
    // -----------------------------------------------------------------------
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        com.example.ExampleMod.LOGGER.info("NicotineState: Saving to NBT...");
        NbtCompound data = new NbtCompound();
        levels.forEach((uuid, level) -> data.putInt(uuid.toString(), level));
        nbt.put("players", data);
        return nbt;
    }

    // -----------------------------------------------------------------------
    // Доступ к данным
    // -----------------------------------------------------------------------
    public int getLevel(UUID uuid, int defaultValue) {
        return levels.getOrDefault(uuid, defaultValue);
    }

    public void setLevel(UUID uuid, int level) {
        // com.example.ExampleMod.LOGGER.info("NicotineState: Setting level for {} to {} (was {})", uuid, level, levels.get(uuid));
        levels.put(uuid, level);
        markDirty(); // Сообщаем Minecraft, что данные изменились и нужно сохранить
    }

    public boolean hasPlayer(UUID uuid) {
        return levels.containsKey(uuid);
    }

    // -----------------------------------------------------------------------
    // Получение / создание синглтона через PersistentStateManager
    // -----------------------------------------------------------------------
    public static NicotineState getServerState(MinecraftServer server) {
        PersistentStateManager manager = server
                .getWorld(net.minecraft.world.World.OVERWORLD)
                .getPersistentStateManager();
        return manager.getOrCreate(NicotineState::fromNbt, NicotineState::new, KEY);
    }
}
