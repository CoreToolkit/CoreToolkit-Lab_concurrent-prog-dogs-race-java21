package edu.eci.arsw.dogsrace.control;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RaceControlTest {

    @Test
    void pauseAndResume_blocksAndReleasesThreads() throws Exception {

        RaceControl control = new RaceControl();
        AtomicInteger ticks = new AtomicInteger(0);
        CountDownLatch started = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            started.countDown();
            try {
                while (!control.isKilled()) {
                    control.awaitIfPaused();
                    ticks.incrementAndGet();
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        worker.start();
        assertTrue(started.await(1, TimeUnit.SECONDS));

        TimeUnit.MILLISECONDS.sleep(50);
        int beforePause = ticks.get();

        control.pause();
        TimeUnit.MILLISECONDS.sleep(80);
        int duringPause = ticks.get();

        assertEquals(beforePause, duringPause,
                "Ticks must not increase while paused");

        control.resume();
        TimeUnit.MILLISECONDS.sleep(50);
        int afterResume = ticks.get();

        assertTrue(afterResume > duringPause,
                "Ticks must increase after resume");

        // 🔥 terminamos el hilo correctamente usando kill
        control.killRace();
        worker.join(500);

        assertFalse(worker.isAlive());
    }

    @Test
    void isPaused_returnsCorrectState() {

        RaceControl control = new RaceControl();

        control.pause();
        assertTrue(control.isPaused(),
                "Should be paused after pause()");

        control.resume();
        assertFalse(control.isPaused(),
                "Should not be paused after resume()");
    }

    @Test
    void killRace_setsKilledFlag() {

        RaceControl control = new RaceControl();

        assertFalse(control.isKilled());
        control.killRace();
        assertTrue(control.isKilled());

        control.resetKill();
        assertFalse(control.isKilled());
    }

    @Test
    void killRace_wakesUpPausedThreads() throws Exception {

        RaceControl control = new RaceControl();
        CountDownLatch reachedPause = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            try {
                control.pause();
                reachedPause.countDown();
                control.awaitIfPaused(); // debería bloquearse
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        worker.start();
        assertTrue(reachedPause.await(1, TimeUnit.SECONDS));

        TimeUnit.MILLISECONDS.sleep(50);
        assertTrue(worker.isAlive(), "Thread should be waiting");

        control.killRace(); // 🔥 debe despertarlo

        worker.join(500);
        assertFalse(worker.isAlive(), "Thread must terminate after kill");
    }

    @Test
    void killRace_stopsRunningThread() throws Exception {

        RaceControl control = new RaceControl();
        AtomicInteger counter = new AtomicInteger(0);

        Thread worker = new Thread(() -> {
            try {
                while (!control.isKilled()) {
                    control.awaitIfPaused();
                    counter.incrementAndGet();
                    Thread.sleep(5);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        worker.start();
        TimeUnit.MILLISECONDS.sleep(50);

        control.killRace();

        worker.join(500);

        int finalCount = counter.get();
        TimeUnit.MILLISECONDS.sleep(50);

        assertEquals(finalCount, counter.get(),
                "Counter must not increase after kill");
        assertFalse(worker.isAlive());
    }

}
