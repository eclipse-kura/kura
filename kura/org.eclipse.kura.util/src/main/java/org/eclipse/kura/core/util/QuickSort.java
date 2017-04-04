/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.util;

import java.util.Arrays;

/**
 * Utilities to quickly sort an array of values
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
