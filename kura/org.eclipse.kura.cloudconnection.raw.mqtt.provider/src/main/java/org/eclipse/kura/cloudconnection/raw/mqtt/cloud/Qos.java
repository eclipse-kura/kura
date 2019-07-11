/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
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
        return value;
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