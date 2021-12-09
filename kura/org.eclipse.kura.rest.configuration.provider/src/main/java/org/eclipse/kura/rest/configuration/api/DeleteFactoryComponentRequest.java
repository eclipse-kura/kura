/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.configuration.api;

import java.util.Set;

import org.eclipse.kura.internal.rest.configuration.FailureHandler;

public class DeleteFactoryComponentRequest implements Validable {

    private final Set<String> pids;
    private final Boolean takeSnapshot;

    public DeleteFactoryComponentRequest(Set<String> pids, boolean takeSnapshot) {
        this.pids = pids;
        this.takeSnapshot = takeSnapshot;
    }

    public Set<String> getPids() {
        return pids;
    }

    public boolean isTakeSnapshot() {
        return this.takeSnapshot == null || this.takeSnapshot;
    }

    @Override
    public void validate() {
        FailureHandler.requireParameter(this.pids, "pids");
    }
}
