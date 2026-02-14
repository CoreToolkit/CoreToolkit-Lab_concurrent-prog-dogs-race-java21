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
        while (paso < carril.size() && !control.isKilled()) {

            control.awaitIfPaused();

            Thread.sleep(100);

            if (control.isKilled()) {
                return;
            }

            control.awaitIfPaused();

            final int step = paso;

            SwingUtilities.invokeLater(() -> {
                if (!control.isKilled()) {
                    carril.setPasoOn(step);
                    carril.displayPasos(step + 1);
                }
            });

            paso++;

            if (paso == carril.size() && !control.isKilled()) {
                registry.registerArrival(getName());
                SwingUtilities.invokeLater(() -> {
                    if (!control.isKilled()) {
                        carril.finish();
                    }
                });
            }
        }
    }

    @Override
    public void run() {
        try {
            corra();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
