/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint;


public class SeqCounter {

    private int seq = 1; // start from 1 since 0 is reserved for BIRTH messages

    public synchronized void next() {
        if (this.seq == 255) {
            this.seq = 1;
        } else {
            this.seq = this.seq + 1;
        }
    }

    public synchronized int getCurrent() {
        return this.seq;
    }

}
