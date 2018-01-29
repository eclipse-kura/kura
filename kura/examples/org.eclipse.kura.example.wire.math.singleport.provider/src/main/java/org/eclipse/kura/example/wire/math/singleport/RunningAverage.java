/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.example.wire.math.singleport;

import java.util.ArrayDeque;

public class RunningAverage {

    private final ArrayDeque<Double> window;
    private final int windowSize;

    private double last;
    private double sum;

    public RunningAverage(final int windowSize) {
        this.window = new ArrayDeque<>(windowSize);
        this.windowSize = windowSize;
    }

    public double updateAndGet(double value) {
        if (this.window.size() >= windowSize) {
            this.last = this.window.removeFirst();
        }
        this.window.addLast(value);
        this.sum = value + sum - last;
        return this.sum / window.size();
    }

    public int getActualWindowSize() {
        return window.size();
    }
}
