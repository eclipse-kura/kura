/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.raspberrypi.sensehat.ledmatrix;

import java.util.function.IntBinaryOperator;

public enum Transform {
    IDENTITY((x, y) -> y * 8 + x),
    ROTATE_90((x, y) -> (x) * 8 + (8 - 1 - y)),
    ROTATE_180((x, y) -> (8 - 1 - y) * 8 + (8 - 1 - x)),
    ROTATE_270((x, y) -> (8 - 1 - x) * 8 + (y));

    private final IntBinaryOperator func;

    Transform(IntBinaryOperator func) {
        this.func = func;
    }

    public int apply(int x, int y) {
        return this.func.applyAsInt(x, y);
    }
}
