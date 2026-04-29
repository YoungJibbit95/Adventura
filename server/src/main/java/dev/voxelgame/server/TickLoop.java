package dev.voxelgame.server;

import java.util.concurrent.atomic.AtomicBoolean;

public final class TickLoop implements AutoCloseable {
    public static final int TPS = 20;
    private static final long NANOS_PER_TICK = 1_000_000_000L / TPS;

    private final Tickable tickable;
    private final AtomicBoolean running = new AtomicBoolean();
    private Thread thread;

    public TickLoop(Tickable tickable) {
        this.tickable = tickable;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        thread = new Thread(this::run, "server-tick-loop");
        thread.setDaemon(false);
        thread.start();
    }

    private void run() {
        long nextTick = System.nanoTime();
        long tick = 0;
        while (running.get()) {
            long now = System.nanoTime();
            if (now >= nextTick) {
                tickable.tick(tick++);
                nextTick += NANOS_PER_TICK;
                if (now - nextTick > NANOS_PER_TICK * 5) {
                    nextTick = now + NANOS_PER_TICK;
                }
            } else {
                long sleepNanos = nextTick - now;
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running.set(false);
                }
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }

    @FunctionalInterface
    public interface Tickable {
        void tick(long tick);
    }
}
