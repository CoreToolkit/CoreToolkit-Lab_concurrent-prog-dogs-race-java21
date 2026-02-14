package edu.eci.arsw.dogsrace.app;

import edu.eci.arsw.dogsrace.control.RaceControl;
import edu.eci.arsw.dogsrace.domain.ArrivalRegistry;
import edu.eci.arsw.dogsrace.threads.Galgo;
import edu.eci.arsw.dogsrace.ui.Canodromo;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class MainCanodromo {

    private static Galgo[] galgos;
    private static Canodromo can;

    private static final RaceControl control = new RaceControl();
    private static ArrivalRegistry registry = new ArrivalRegistry(control);

    public static void main(String[] args) {
        can = new Canodromo(17, 100);
        galgos = new Galgo[can.getNumCarriles()];
        can.setVisible(true);

        can.setStartAction(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                JButton startButton = (JButton) e.getSource();
                startButton.setEnabled(false);

                control.resetKill();
                registry.reset();
                can.restart();

                new Thread(() -> {
                    for (int i = 0; i < can.getNumCarriles(); i++) {
                        galgos[i] = new Galgo(
                                can.getCarril(i),
                                String.valueOf(i),
                                registry,
                                control
                        );
                        galgos[i].start();
                    }

                    for (Galgo g : galgos) {
                        try {
                            g.join();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    if (!control.isKilled()) {
                        String winner = registry.getWinner();
                        int total = registry.getNextPosition() - 1;
                        var arrivals = registry.getArrivals();
                        SwingUtilities.invokeLater(() ->
                                can.winnerDialog(winner, total, arrivals)
                        );
                    }

                    SwingUtilities.invokeLater(() ->
                            startButton.setEnabled(true)
                    );

                }, "race-orchestrator").start();
            }
        });

        can.setStopAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.pause();
            }
        });

        can.setContinueAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.resume();
            }
        });

        can.setKillAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                control.killRace();

                new Thread(() -> {
                    for (Galgo g : galgos) {
                        if (g != null && g.isAlive()) {
                            try {
                                g.join();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }

                    registry.reset();
                    control.resetKill();

                    SwingUtilities.invokeLater(() -> {
                        can.restart();
                    });

                }).start();
            }
        });
    }
}
