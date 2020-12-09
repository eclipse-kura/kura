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
