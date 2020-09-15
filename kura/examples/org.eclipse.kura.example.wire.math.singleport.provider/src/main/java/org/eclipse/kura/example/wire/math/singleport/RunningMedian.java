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
import java.util.TreeMap;

public class RunningMedian<T extends Number & Comparable<T>> {

    private final ArrayDeque<T> buffer;
    private final TreeMap<T, int[]> lower;
    private final TreeMap<T, int[]> higher;
    private final int capacity;
    private int lowerCount;
    private int higherCount;

    public RunningMedian(final int capacity) {
        super();
        this.capacity = capacity;
        this.buffer = new ArrayDeque<>(capacity);
        this.lower = new TreeMap<>();
        this.higher = new TreeMap<>();
    }

    public void add(final T num) {
        if (this.buffer.size() >= this.capacity) {
            final T key = this.buffer.remove();
            if (key.compareTo(this.higher.firstKey()) >= 0) {
                MapUtil.computeDecrement(this.higher, key);
                this.higherCount--;
            } else {
                MapUtil.computeDecrement(this.lower, key);
                this.lowerCount--;
            }
        }

        this.buffer.add(num);
        if (this.higher.isEmpty() || num.compareTo(this.higher.firstKey()) >= 0) {
            MapUtil.computeIncrement(this.higher, num);
            this.higherCount++;
        } else {
            MapUtil.computeIncrement(this.lower, num);
            this.lowerCount++;
        }

        balance();
    }

    public Double median() {
        if (this.higherCount > this.lowerCount) {
            return this.higher.firstKey().doubleValue();
        } else {
            return (this.lower.lastKey().doubleValue() + this.higher.firstKey().doubleValue()) / 2;
        }
    }

    private void balance() {
        if (this.lowerCount > this.higherCount) {
            final T key = this.lower.lastKey();
            MapUtil.computeDecrement(this.lower, key);
            MapUtil.computeIncrement(this.higher, key);
            this.lowerCount--;
            this.higherCount++;
        } else if (this.higherCount > this.lowerCount + 1) {
            final T key = this.higher.firstKey();
            MapUtil.computeDecrement(this.higher, key);
            MapUtil.computeIncrement(this.lower, key);
            this.lowerCount++;
            this.higherCount--;
        }
    }

}
