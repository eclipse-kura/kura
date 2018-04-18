/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble;

import java.util.function.Consumer;

import tinyb.BluetoothNotification;

public class BluetoothLeNotification<T> implements BluetoothNotification<T> {

    private final Consumer<T> callback;

    public BluetoothLeNotification(Consumer<T> callback) {
        this.callback = callback;
    }

    @Override
    public void run(T value) {
        this.callback.accept(value);
    }
}
