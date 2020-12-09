/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.util;

import java.util.Arrays;

/**
 * Utilities to quickly sort an array of values
 *
 * @deprecated Use sort methods from {@link Arrays} instead
 */
@Deprecated
public final class QuickSort {

    private QuickSort() {
    }

    /**
     * @deprecated Use {@link Arrays#sort(int[])} instead
     */
    @Deprecated
    public static void sort(int[] array) {
        Arrays.sort(array);
    }

    /**
     * @deprecated Use {@link Arrays#sort(long[])} instead
     */
    @Deprecated
    public static void sort(long[] array) {
        Arrays.sort(array);
    }
}
