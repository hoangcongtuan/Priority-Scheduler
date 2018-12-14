package main;

import javafx.animation.AnimationTimer;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Scanner;

public abstract class SimulationCore extends AnimationTimer {
    private final static String TAG = SimulationCore.class.getName();
    public enum SimulationState {
        NON_READY("Non Ready"),
        READY("Ready"),
        RUNNING("Running"),
        PAUSE("Pause"),
        FINISH("Finish");

        private final String text;

        SimulationState(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private SimulationState state;
    private Scanner scanner;
    private DecimalFormat df;

    public SimulationCore() {
        super();
        state = SimulationState.NON_READY;
    }

    public void setData(String simulationData) {
        this.scanner = new Scanner(simulationData);
        this.df = new DecimalFormat("#.#");
        this.df.setRoundingMode(RoundingMode.FLOOR);

        this.state = SimulationState.READY;
    }

    @Override
    public void start() {
        if (!((state == SimulationState.READY) || (state == SimulationState.PAUSE))) {
            System.out.println(TAG + ": You must init Simulation Data First");
            return;
        }
        this.state = SimulationState.RUNNING;
        super.start();
    }

    public void pause() {
        if (this.state != SimulationState.RUNNING) {
            System.out.println(TAG + ": Cannot pause when it's not start!!");
            return;
        }
        this.state = SimulationState.PAUSE;
        this.stop();
    }

    public void resume() {
        if (this.state != SimulationState.PAUSE) {
            System.out.println(TAG + ": only resume when it'state is pause");
        }
        this.start();
    }

    public Scanner getScanner() {
        return scanner;
    }

    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
    }

    public DecimalFormat getDf() {
        return df;
    }

    public void setDf(DecimalFormat df) {
        this.df = df;
    }

    public SimulationState getState() {
        return state;
    }
}
