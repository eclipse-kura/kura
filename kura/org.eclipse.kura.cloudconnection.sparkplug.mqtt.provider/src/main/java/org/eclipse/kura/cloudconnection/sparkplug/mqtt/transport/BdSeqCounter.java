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

import java.util.concurrent.atomic.AtomicInteger;

public class BdSeqCounter {

    private AtomicInteger currentBdSeq = new AtomicInteger();
    private AtomicInteger nextBdSeq = new AtomicInteger();

    public void next() {
        if (this.nextBdSeq.get() > 255) {
            this.nextBdSeq.set(0);
        }

        this.currentBdSeq.set(this.nextBdSeq.getAndIncrement());
    }

    public int getCurrent() {
        return this.currentBdSeq.get();
    }

}
