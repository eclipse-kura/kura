/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
