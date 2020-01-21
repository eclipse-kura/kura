/**
 * Copyright (c) 2018, 2019 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
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
