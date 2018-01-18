/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.util;

import java.util.Comparator;
import java.util.Map.Entry;

public class LabelComparator<T> implements Comparator<Entry<String, T>> {

    private enum ComparatorState {
        COMPARE_STRING,
        COMPARE_NUMBER,
    }

    private int getNumberEnd(String s, int index) {

        while (index < s.length()) {
            if (!Character.isDigit(s.charAt(index))) {
                return index;
            }
            index++;
        }

        return index;
    }

    @Override
    public int compare(Entry<String, T> e1, Entry<String, T> e2) {
        String o1 = e1.getKey();
        String o2 = e2.getKey();

        ComparatorState state = ComparatorState.COMPARE_STRING;
        int i1 = 0;
        int i2 = 0;

        while (i1 < o1.length() && i2 < o2.length()) {
            final char c1 = o1.charAt(i1);
            final char c2 = o2.charAt(i2);

            if (state == ComparatorState.COMPARE_STRING) {

                if (Character.isDigit(c1) && Character.isDigit(c2)) {
                    state = ComparatorState.COMPARE_NUMBER;
                    continue;
                }

                if (c1 < c2) {
                    return -1;
                }
                if (c1 > c2) {
                    return 1;
                }

                i1++;
                i2++;

            } else {

                final int s1 = i1;
                final int s2 = i2;

                i1 = getNumberEnd(o1, s1);
                i2 = getNumberEnd(o2, s2);

                int n1 = Integer.parseInt(o1.substring(s1, i1));
                int n2 = Integer.parseInt(o2.substring(s2, i2));

                if (n1 < n2) {
                    return -1;
                }
                if (n1 > n2) {
                    return 1;
                }

                state = ComparatorState.COMPARE_STRING;
            }
        }

        return 0;
    }

}
