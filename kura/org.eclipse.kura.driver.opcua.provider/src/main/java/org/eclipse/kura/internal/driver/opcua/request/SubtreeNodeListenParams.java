/**
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.eclipse.kura.internal.driver.opcua.request;

import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

public class SubtreeNodeListenParams extends ListenParams {

    final SingleNodeListenParams rootParams;

    public SubtreeNodeListenParams(final ReadValueId readValueId, final SingleNodeListenParams rootParams) {
        super(readValueId);
        this.rootParams = rootParams;
    }

    @Override
    public double getSamplingInterval() {
        return this.rootParams.getSamplingInterval();
    }

    @Override
    public long getQueueSize() {
        return this.rootParams.getQueueSize();
    }

    @Override
    public boolean getDiscardOldest() {
        return this.rootParams.getDiscardOldest();
    }
}
