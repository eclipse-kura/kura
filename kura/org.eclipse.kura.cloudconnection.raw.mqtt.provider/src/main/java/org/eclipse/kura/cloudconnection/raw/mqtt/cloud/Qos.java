/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.raw.mqtt.cloud;

public enum Qos {

    QOS0(0),
    QOS1(1),
    QOS2(2);

    private final int value;

    private Qos(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static Qos valueOf(final int i) {
        if (i == 0) {
            return QOS0;
        } else if (i == 1) {
            return QOS1;
        } else if (i == 2) {
            return QOS2;
        }

        throw new IllegalArgumentException();
    }

}