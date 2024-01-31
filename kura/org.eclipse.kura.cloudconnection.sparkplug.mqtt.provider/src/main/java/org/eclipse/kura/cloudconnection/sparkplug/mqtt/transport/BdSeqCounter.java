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

    private int bdSeq = -1; // first invocation of next should return 0

    public synchronized void next() {
        if (this.bdSeq == 255) {
            this.bdSeq = 0;
        } else {
            this.bdSeq = this.bdSeq + 1;
        }
    }

    public synchronized int getCurrent() {
        return this.bdSeq;
    }

}
