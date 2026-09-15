package com.example.block.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/** Real elapsed time accumulated only during this world's running server sessions. */
public final class JarCuringClock extends PersistentState {
    private long elapsedMillis;
    private long lastSampleNanos;

    public JarCuringClock() { this(System.nanoTime()); }

    JarCuringClock(long initialSample) { lastSampleNanos = initialSample; }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> get(server).lastSampleNanos = System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server -> now(server));
    }

    private static JarCuringClock get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                JarCuringClock::fromNbt, JarCuringClock::new, "smokemod_jar_curing_clock");
    }

    public static long now(MinecraftServer server) {
        JarCuringClock clock = get(server);
        clock.advance(System.nanoTime());
        return clock.elapsedMillis;
    }

    void advance(long sample) {
        long delta = (sample - lastSampleNanos) / 1_000_000L;
        if (delta > 0) {
            elapsedMillis += delta;
            lastSampleNanos += delta * 1_000_000L;
            markDirty();
        }
    }

    static JarCuringClock fromNbt(NbtCompound nbt) {
        JarCuringClock clock = new JarCuringClock();
        clock.elapsedMillis = Math.max(0, nbt.getLong("ElapsedMillis"));
        return clock;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putLong("ElapsedMillis", elapsedMillis);
        return nbt;
    }
}
