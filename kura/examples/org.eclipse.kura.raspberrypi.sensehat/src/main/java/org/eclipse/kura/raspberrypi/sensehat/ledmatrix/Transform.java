/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
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
