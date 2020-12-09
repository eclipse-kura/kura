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
package org.eclipse.kura.wire.graph;

import org.eclipse.kura.wire.WireConfiguration;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This POJO is used to represent, in the Wires context, the configuration of a Wire used in a multi-port composer.
 *
 * @since 1.4
 */
@ProviderType
public class MultiportWireConfiguration extends WireConfiguration {

    private int emitterPort;
    private int receiverPort;

    public MultiportWireConfiguration(String emitterPid, String receiverPid, int emitterPort, int receiverPort) {
        super(emitterPid, receiverPid);
        this.emitterPort = emitterPort;
        this.receiverPort = receiverPort;
    }

    public int getEmitterPort() {
        return this.emitterPort;
    }

    public void setEmitterPort(int emitterPort) {
        this.emitterPort = emitterPort;
    }

    public int getReceiverPort() {
        return this.receiverPort;
    }

    public void setReceiverPort(int receiverPort) {
        this.receiverPort = receiverPort;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.emitterPort;
        result = prime * result + this.receiverPort;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MultiportWireConfiguration other = (MultiportWireConfiguration) obj;
        if (this.emitterPort != other.emitterPort) {
            return false;
        }
        if (this.receiverPort != other.receiverPort) {
            return false;
        }
        return true;
    }

}
