package org.eclipse.kura.example.wire.math.singleport;

import java.util.ArrayDeque;

public class RunningExtremes {

    private final ArrayDeque<Double> window;
    private final int windowSize;

    private double max;
    private double min;
    private double last;

    public RunningExtremes(final int windowSize) {
        this.window = new ArrayDeque<>(windowSize);
        this.windowSize = windowSize;
    }

    private void updateWindow(double value) {
        if (this.window.size() >= windowSize) {
            this.last = this.window.removeFirst();
        }
        if (this.window.isEmpty()) {
            this.min = value;
            this.max = value;
        }
        this.window.addLast(value);
        if (this.last == this.max || this.last == this.min) {
            updateExtremes();
        }

        if (value > this.max) {
            this.max = value;
        }
        if (value < this.min) {
            this.min = value;
        }
    }

    private void updateExtremes() {
        double localMax = this.window.getLast();
        double localMin = this.window.getLast();
        for (double number : window) {
            if (number > localMax) {
                localMax = number;
            }
            if (number < localMin) {
                localMin = number;
            }
        }
        this.min = localMin;
        this.max = localMax;
    }

    public double updateAndGetMax(double value) {
        updateWindow(value);
        return this.max;
    }

    public double updateAndGetMin(double value) {
        updateWindow(value);
        return this.min;
    }
}
