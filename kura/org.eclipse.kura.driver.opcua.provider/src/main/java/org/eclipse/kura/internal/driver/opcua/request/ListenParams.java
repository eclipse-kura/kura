/**
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.kura.internal.driver.opcua.request;

import static org.eclipse.kura.internal.driver.opcua.Utils.tryExtract;

import java.util.Map;

import org.eclipse.kura.internal.driver.opcua.OpcUaChannelDescriptor;

public class ListenParams extends ReadParams {

    private final double samplingInterval;
    private final long queueSize;
    private final boolean discardOldest;

    public ListenParams(Map<String, Object> channelConfig) {
        super(channelConfig);
        this.samplingInterval = tryExtract(channelConfig, OpcUaChannelDescriptor::getSamplingInterval,
                "Error while retrieving Sampling Interval");
        this.queueSize = tryExtract(channelConfig, OpcUaChannelDescriptor::getQueueSize,
                "Error while retrieving Queue Size");
        this.discardOldest = tryExtract(channelConfig, OpcUaChannelDescriptor::getDiscardOldest,
                "Error while retrieving Discard Oldest parameter");
    }

    public double getSamplingInterval() {
        return samplingInterval;
    }

    public long getQueueSize() {
        return queueSize;
    }

    public boolean getDiscardOldest() {
        return discardOldest;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (discardOldest ? 1231 : 1237);
        result = prime * result + (int) (queueSize ^ (queueSize >>> 32));
        long temp;
        temp = Double.doubleToLongBits(samplingInterval);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ListenParams other = (ListenParams) obj;
        if (discardOldest != other.discardOldest)
            return false;
        if (queueSize != other.queueSize)
            return false;
        if (Double.doubleToLongBits(samplingInterval) != Double.doubleToLongBits(other.samplingInterval))
            return false;
        return true;
    }

}
