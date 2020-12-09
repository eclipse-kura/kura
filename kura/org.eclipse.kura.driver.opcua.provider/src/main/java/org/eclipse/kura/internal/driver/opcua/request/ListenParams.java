/**
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
 */

package org.eclipse.kura.internal.driver.opcua.request;

import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

public abstract class ListenParams extends ReadParams {

    public ListenParams(final Map<String, Object> channelConfig) {
        super(channelConfig);
    }

    public ListenParams(final ReadValueId readValueId) {
        super(readValueId);
    }

    public abstract double getSamplingInterval();

    public abstract long getQueueSize();

    public abstract boolean getDiscardOldest();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (getDiscardOldest() ? 1231 : 1237);
        result = prime * result + (int) (getQueueSize() ^ getQueueSize() >>> 32);
        long temp;
        temp = Double.doubleToLongBits(getSamplingInterval());
        result = prime * result + (int) (temp ^ temp >>> 32);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SingleNodeListenParams other = (SingleNodeListenParams) obj;
        if (getDiscardOldest() != other.getDiscardOldest()) {
            return false;
        }
        if (getQueueSize() != other.getQueueSize()) {
            return false;
        }
        return Double.doubleToLongBits(getSamplingInterval()) == Double.doubleToLongBits(other.getSamplingInterval());
    }
}
