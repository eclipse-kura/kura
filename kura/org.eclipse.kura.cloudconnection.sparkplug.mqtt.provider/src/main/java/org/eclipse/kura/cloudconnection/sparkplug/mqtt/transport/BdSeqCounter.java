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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport;

public class BdSeqCounter {

    private int currentBdSeq = 0;
    private int nextBdSeq = 0;

    public synchronized void next() {
        if (this.nextBdSeq > 255) {
            this.nextBdSeq = 0;
        }

        this.currentBdSeq = this.nextBdSeq++;
    }

    public synchronized int getCurrent() {
        return this.currentBdSeq;
    }

}
