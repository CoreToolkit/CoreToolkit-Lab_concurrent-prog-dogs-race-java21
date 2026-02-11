package edu.eci.arsw.dogsrace.control;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Common monitor to pause/resume all runners.
 *
 * Uses wait()/notifyAll() as requested by the lab.
 */
public final class RaceControl {

    private final Object monitor = new Object();
    private boolean paused = false;
    private final AtomicBoolean killed = new AtomicBoolean(false);




    public void pause() {
        synchronized (monitor) {
            paused = true;
        }
    }

    public void resume() {
        synchronized (monitor) {
            paused = false;
            monitor.notifyAll();
        }
    }

    public boolean isPaused() {
        synchronized (monitor) {
            return paused;
        }
    }

    /**
     * Call frequently from the running threads to honor pause/resume.
     */
    public void awaitIfPaused() throws InterruptedException {
        synchronized (monitor) {
            while (paused) {
                monitor.wait();
            }
        }
    }

    public void killRace() {
        killed.set(true);
        synchronized (monitor) {
            paused = false;     // por si estaba pausado
            monitor.notifyAll(); // despierta todos los hilos
        }
    }

    public boolean isKilled() {
        return killed.get();
    }

    public void resetKill() {
        killed.set(false);
    }

}
