/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.example.wire.math.singleport;

import java.util.ArrayDeque;
import java.util.TreeMap;

public class RunningExtremum<T extends Number & Comparable<T>> {

    private final ArrayDeque<T> buffer;
    private final TreeMap<T, int[]> map;
    private final int capacity;

    public RunningExtremum(int capacity) {
        super();
        this.buffer = new ArrayDeque<>(capacity);
        this.map = new TreeMap<>();
        this.capacity = capacity;
    }

    public void add(final T num) {
        if (this.buffer.size() >= this.capacity) {
            final T key = this.buffer.remove();
            MapUtil.computeDecrement(this.map, key);
        }

        this.buffer.add(num);
        MapUtil.computeIncrement(this.map, num);
    }

    public T min() {
        return this.map.firstKey();
    }

    public T max() {
        return this.map.lastKey();
    }
}
