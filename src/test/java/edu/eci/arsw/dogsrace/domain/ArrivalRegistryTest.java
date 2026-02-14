package edu.eci.arsw.dogsrace.domain;

import edu.eci.arsw.dogsrace.control.RaceControl;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ArrivalRegistryTest {

    @Test
    void registerArrival_assignsUniquePositionsAndWinner() throws Exception {

        int n = 50;

        // 🔥 Crear el RaceControl requerido por el constructor
        RaceControl control = new RaceControl();
        ArrivalRegistry registry = new ArrivalRegistry(control);

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);

        var futures = IntStream.range(0, n)
                .mapToObj(i -> pool.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return registry.registerArrival("dog-" + i);
                }))
                .toList();

        assertEquals(n, futures.size(), "All tasks must be submitted");
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        Set<Integer> positions = futures.stream()
                .map(f -> {
                    try {
                        var snapshot = f.get(5, TimeUnit.SECONDS);
                        assertNotNull(snapshot, "Snapshot must not be null");
                        return snapshot.position();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());

        pool.shutdownNow();

        assertEquals(n, positions.size(), "All positions must be unique");
        assertTrue(positions.contains(1), "There must be a first place");
        assertTrue(positions.contains(n), "There must be an n-th place");

        assertNotNull(registry.getWinner(), "Winner must be set");
        assertEquals(n + 1, registry.getNextPosition(),
                "Next position must be n+1");
    }

    @Test
    void registerArrival_returnsNullIfKilled() {

        RaceControl control = new RaceControl();
        ArrivalRegistry registry = new ArrivalRegistry(control);

        control.killRace();

        var result = registry.registerArrival("dog-x");

        assertNull(result, "Should return null if race was killed");
        assertNull(registry.getWinner());
        assertEquals(1, registry.getNextPosition());
    }

    @Test
    void registerArrival_stopsRegisteringAfterKill() throws Exception {

        int n = 100;

        RaceControl control = new RaceControl();
        ArrivalRegistry registry = new ArrivalRegistry(control);

        ExecutorService pool = Executors.newFixedThreadPool(20);

        var futures = IntStream.range(0, n)
                .mapToObj(i -> pool.submit(() -> {
                    if (i == 50) {
                        control.killRace(); // 🔥 kill en medio
                    }
                    return registry.registerArrival("dog-" + i);
                }))
                .toList();

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);

        long nonNullCount = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(r -> r != null)
                .count();

        assertTrue(nonNullCount < n,
                "Not all arrivals should be registered after kill");
    }

    @Test
    void registerArrival_whenKilledBeforeStart_registersNothing() {

        RaceControl control = new RaceControl();
        ArrivalRegistry registry = new ArrivalRegistry(control);

        control.killRace();

        for (int i = 0; i < 20; i++) {
            assertNull(registry.registerArrival("dog-" + i));
        }

        assertNull(registry.getWinner());
        assertEquals(1, registry.getNextPosition());
        assertTrue(registry.getArrivals().isEmpty());
    }

}
