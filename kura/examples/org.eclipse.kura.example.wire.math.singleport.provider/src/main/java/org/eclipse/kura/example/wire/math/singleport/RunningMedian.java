package org.eclipse.kura.example.wire.math.singleport;

import java.util.ArrayDeque;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunningMedian {

    private final ArrayDeque<Double> window;
    private final int windowSize;

    private static final Logger logger = LoggerFactory.getLogger(RunningMedian.class);

    public RunningMedian(final int windowSize) {
        this.window = new ArrayDeque<>(windowSize);
        this.windowSize = windowSize;
    }

    public double updateAndGetMedian(double value) {
        int index = 0;
        if (this.window.size() >= windowSize) {
            this.window.removeFirst();
        }
        this.window.addLast(value);
        double[] values = new double[windowSize];
        for (Double windowValue : window) {
            values[index++] = windowValue.doubleValue();
        }
        Arrays.sort(values);
        logger.info("Values are {}", values);
        logger.info("Asd are {} and {}", values[windowSize / 2], values[windowSize / 2 - 1]);
        if (windowSize % 2 == 0) {
            return (values[windowSize / 2] + values[windowSize / 2 - 1]) / 2;
        } else {
            return values[windowSize / 2];
        }

    }

}
