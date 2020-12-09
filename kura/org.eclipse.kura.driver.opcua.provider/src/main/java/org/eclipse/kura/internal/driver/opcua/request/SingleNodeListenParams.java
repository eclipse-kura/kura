/**
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
 */

package org.eclipse.kura.internal.driver.opcua.request;

import static org.eclipse.kura.internal.driver.opcua.Utils.tryExtract;

import java.util.Map;

import org.eclipse.kura.internal.driver.opcua.OpcUaChannelDescriptor;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

public class SingleNodeListenParams extends ListenParams {

    private final double samplingInterval;
    private final long queueSize;
    private final boolean discardOldest;

    public SingleNodeListenParams(final ReadValueId readValueId, final double samplingInterval, final long queueSize,
            final boolean discardOldest) {
        super(readValueId);
        this.samplingInterval = samplingInterval;
        this.queueSize = queueSize;
        this.discardOldest = discardOldest;
    }

    public SingleNodeListenParams(Map<String, Object> channelConfig) {
        super(channelConfig);
        this.samplingInterval = tryExtract(channelConfig, OpcUaChannelDescriptor::getSamplingInterval,
                "Error while retrieving Sampling Interval");
        this.queueSize = tryExtract(channelConfig, OpcUaChannelDescriptor::getQueueSize,
                "Error while retrieving Queue Size");
        this.discardOldest = tryExtract(channelConfig, OpcUaChannelDescriptor::getDiscardOldest,
                "Error while retrieving Discard Oldest parameter");
    }

    @Override
    public double getSamplingInterval() {
        return this.samplingInterval;
    }

    @Override
    public long getQueueSize() {
        return this.queueSize;
    }

    @Override
    public boolean getDiscardOldest() {
        return this.discardOldest;
    }

}
