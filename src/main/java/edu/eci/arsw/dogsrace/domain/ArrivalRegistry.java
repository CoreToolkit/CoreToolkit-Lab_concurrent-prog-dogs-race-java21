package edu.eci.arsw.dogsrace.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import edu.eci.arsw.dogsrace.control.RaceControl;

/**
 * Thread-safe arrival registry using AtomicInteger
 * and supporting early termination (kill).
 */
public final class ArrivalRegistry {

    // Contador atómico para asignación de posiciones
    private final AtomicInteger nextPosition = new AtomicInteger(1);

    // Lista protegida manualmente (ArrayList no es thread-safe)
    private final List<String> arrivals = new ArrayList<>();

    // Volatile para visibilidad entre hilos
    private volatile String winner = null;

    // Referencia al control global de carrera
    private final RaceControl control;

    public ArrivalRegistry(RaceControl control) {
        this.control = control;
    }

    public ArrivalSnapshot registerArrival(String dogName) {
        Objects.requireNonNull(dogName, "dogName");

        // 🔥 Si la carrera fue matada, no registrar
        if (control.isKilled()) {
            return null;
        }

        // Asignación atómica de posición
        int position = nextPosition.getAndIncrement();

        synchronized (this) {

            // 🔥 Doble verificación dentro de la región crítica
            if (control.isKilled()) {
                return null;
            }

            arrivals.add(dogName);

            if (position == 1) {
                winner = dogName;
            }

            return new ArrivalSnapshot(position, winner);
        }
    }

    public synchronized List<String> getArrivals() {
        return List.copyOf(arrivals);
    }

    public int getNextPosition() {
        return nextPosition.get();
    }

    public String getWinner() {
        return winner;
    }

    // 🔥 Necesario para botón Kill o reinicio
    public synchronized void reset() {
        nextPosition.set(1);
        winner = null;
        arrivals.clear();
    }

    public record ArrivalSnapshot(int position, String winner) {
    }
}
