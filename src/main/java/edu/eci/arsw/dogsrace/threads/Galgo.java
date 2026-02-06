package edu.eci.arsw.dogsrace.threads;

import edu.eci.arsw.dogsrace.control.RaceControl;
import edu.eci.arsw.dogsrace.domain.ArrivalRegistry;
import edu.eci.arsw.dogsrace.ui.Carril;
import javax.swing.SwingUtilities;

/**
 * A runner (greyhound) in the race.
 */
public class Galgo extends Thread {

    private final Carril carril;
    private final ArrivalRegistry registry;
    private final RaceControl control;

    private int paso = 0;

    public Galgo(Carril carril, String name, ArrivalRegistry registry, RaceControl control) {
        super(name);
        this.carril = carril;
        this.registry = registry;
        this.control = control;
    }

    private void corra() throws InterruptedException {
        while (paso < carril.size()) {
            control.awaitIfPaused();

            Thread.sleep(100);
            control.awaitIfPaused(); // doble verificación para reducir la latencia del Stop

            // usamos step pq se puede demorar swingUtilities en ejecutar la orden que puede
            // causar que se vuelva
            // a actualizar el paso antes pintarlo haciendo que pinte incorrectamente.
            final int step = paso;

            // EDT para volver thread safe la actualización de la UI, ejecuta la orden
            // cuando pueda "encola la tarea"
            SwingUtilities.invokeLater(() -> {
                carril.setPasoOn(step);
                carril.displayPasos(step + 1);
            });
            paso++;

            if (paso == carril.size()) {
                registry.registerArrival(getName());
                // Misma idea de arriba, edt para evitar sobreescrituras erroneas en la UI
                SwingUtilities.invokeLater(carril::finish);

            }
        }
    }

    @Override
    public void run() {
        try {
            corra();
        } catch (InterruptedException e) {
            // Restore interruption status and exit
            Thread.currentThread().interrupt();
        }
    }
}
