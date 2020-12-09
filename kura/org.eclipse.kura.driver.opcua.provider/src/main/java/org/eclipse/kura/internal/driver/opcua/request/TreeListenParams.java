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

import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

public class TreeListenParams extends SingleNodeListenParams {

    public TreeListenParams(Map<String, Object> channelConfig) {
        super(channelConfig);
    }

    public TreeListenParams(final ReadValueId readValueId, final double samplingInterval, final long queueSize,
            final boolean discardOldest) {
        super(readValueId, samplingInterval, queueSize, discardOldest);
    }
}
