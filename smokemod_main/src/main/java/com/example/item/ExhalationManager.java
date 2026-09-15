package com.example.item;

import com.example.ExampleMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExhalationManager {

    private static class ExhalationTask {
        final ServerPlayerEntity player;
        int ticksRemaining;

        ExhalationTask(ServerPlayerEntity player, int durationTicks) {
            this.player = player;
            this.ticksRemaining = durationTicks;
        }
    }

    private static final List<ExhalationTask> ACTIVE_TASKS = new ArrayList<>();

    /**
     * Запускает процесс выдоха для игрока на 3 секунды (60 тиков).
     * Дым будет лететь туда, куда игрок смотрит в каждый момент времени.
     */
    public static void startExhalation(ServerPlayerEntity player) {
        // 60 тиков = 3 секунды
        ACTIVE_TASKS.add(new ExhalationTask(player, 15));
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<ExhalationTask> iterator = ACTIVE_TASKS.iterator();
            while (iterator.hasNext()) {
                ExhalationTask task = iterator.next();

                // Если игрок вышел с сервера или умер — прекращаем выдох
                if (task.player.isRemoved() || !task.player.isAlive()) {
                    iterator.remove();
                    continue;
                }

                if (task.player.getWorld() instanceof ServerWorld serverWorld) {
                    // Каждый тик берём АКТУАЛЬНЫЙ вектор взгляда игрока
                    Vec3d look = task.player.getRotationVec(1.0F).normalize();

                    // Разлёт скорости — делаем поток шире и рассеяннее
                    double velSpread = 0.045;

                    // Разлёт позиции — почти убираем (0.02), чтобы струя вылетала тонкой из рта
                    double posSpread = 0.02;
                    // Спавним 2 частицы за один тик (в два раза больше дыма)
                    for (int i = 0; i < 1; i++) {
                        // Важно пересчитывать разлёт и смещение для КАЖДОЙ частицы в цикле,
                        // чтобы они не летели идеально в одной точке друг в друге.
                        double curOffsetX = (serverWorld.random.nextDouble() - 0.5) * posSpread;
                        double curOffsetY = (serverWorld.random.nextDouble() - 0.5) * posSpread;
                        double curOffsetZ = (serverWorld.random.nextDouble() - 0.5) * posSpread;
                        double pX = task.player.getX() + look.x * 0.2 + curOffsetX;
                        double pY = task.player.getEyeY() - 0.25 + curOffsetY;
                        double pZ = task.player.getZ() + look.z * 0.2 + curOffsetZ;

                        double forwardSpeed = 0.11;
                        double pVx = look.x * forwardSpeed + (serverWorld.random.nextDouble() - 0.5) * velSpread;
                        double pVy = look.y * forwardSpeed + (serverWorld.random.nextDouble() - 0.5) * velSpread;
                        double pVz = look.z * forwardSpeed + (serverWorld.random.nextDouble() - 0.5) * velSpread;

                        serverWorld.spawnParticles(ExampleMod.CIGARETTE_CLOUD, pX, pY, pZ, 0, pVx, pVy, pVz, 1.0);
                    }
                }

                task.ticksRemaining--;
                if (task.ticksRemaining <= 0) {
                    iterator.remove();
                }
            }
        });
    }
}
