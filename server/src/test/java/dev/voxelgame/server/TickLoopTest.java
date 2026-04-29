package dev.voxelgame.server;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TickLoopTest {
    @Test
    void ticksAtLeastOnce() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        TickLoop loop = new TickLoop(tick -> latch.countDown());
        loop.start();
        try {
            assertTrue(latch.await(250, TimeUnit.MILLISECONDS));
        } finally {
            loop.close();
        }
    }
}
