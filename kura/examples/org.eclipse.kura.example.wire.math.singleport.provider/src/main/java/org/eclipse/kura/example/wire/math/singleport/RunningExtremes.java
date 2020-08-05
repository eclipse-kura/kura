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

public class RunningExtremes {

    private final ArrayDeque<Double> window;
    private final int windowSize;

    private double max;
    private double min;
    private Double last = Double.NaN;

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
        if (this.last.doubleValue() == this.max || this.last.doubleValue() == this.min) {
            updateExtremes();
        } else {
            if (value > this.max) {
                this.max = value;
            }
            if (value < this.min) {
                this.min = value;
            }
        }
    }

    private void updateExtremes() {
        this.max = this.window.getLast();
        this.min = this.window.getLast();
        for (double number : window) {
            if (number > this.max) {
                this.max = number;
            }
            if (number < this.min) {
                this.min = number;
            }
        }
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
