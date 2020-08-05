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
import java.util.ArrayList;
import java.util.Collections;

public class RunningMedian {

    private final ArrayDeque<Double> window;
    private final int windowSize;
    private final ArrayList<Double> sortedWindow;

    public RunningMedian(final int windowSize) {
        this.window = new ArrayDeque<>(windowSize);
        this.windowSize = windowSize;
        this.sortedWindow = new ArrayList<>();
    }

    public double updateAndGetMedian(double value) {
        if (this.window.size() >= windowSize) {
            Double last = this.window.removeFirst();
            sortedWindow.remove(last);
        }
        this.window.addLast(value);
        this.sortedWindow.add(value);
        Collections.sort(this.sortedWindow);
        int currentSize = window.size();
        if (currentSize % 2 == 0) {
            return ((Double) sortedWindow.toArray()[currentSize / 2]
                    + (Double) sortedWindow.toArray()[currentSize / 2 - 1]) / 2;
        } else {
            return (Double) sortedWindow.toArray()[currentSize / 2];
        }

    }

}
