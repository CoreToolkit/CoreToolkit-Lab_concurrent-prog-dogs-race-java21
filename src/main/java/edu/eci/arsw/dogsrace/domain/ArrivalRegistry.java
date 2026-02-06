package edu.eci.arsw.dogsrace.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Thread-safe arrival registry.
 * Critical section is limited to the position assignment and winner selection.
 */
public final class ArrivalRegistry {

    private int nextPosition = 1;
    private String winner = null;
    private final List<String> arrivals = new ArrayList<>();

    public synchronized ArrivalSnapshot registerArrival(String dogName) {
        Objects.requireNonNull(dogName, "dogName");
        final int position = nextPosition++;

        // Add to arrivals list in order
        arrivals.add(dogName);

        if (position == 1) {
            winner = dogName;
        }
        return new ArrivalSnapshot(position, winner);
    }

    public synchronized int getNextPosition() {
        return nextPosition;
    }

    public synchronized String getWinner() {
        return winner;
    }

    /**
     * Returns an immutable copy of the arrivals list in order.
     * 
     * @return List of dog names in arrival order (1st, 2nd, 3rd, etc.)
     */
    public synchronized List<String> getArrivals() {
        return List.copyOf(arrivals);
    }

    public record ArrivalSnapshot(int position, String winner) {
    }
}
