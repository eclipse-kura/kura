/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.singleport;

import java.util.ArrayDeque;
import java.util.Arrays;

public class RunningMedian {

    private final ArrayDeque<Double> window;
    private final int windowSize;

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
        if (window.size() % 2 == 0) {
            return (values[windowSize / 2] + values[windowSize / 2 - 1]) / 2;
        } else {
            return values[windowSize / 2];
        }

    }

}
