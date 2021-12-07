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

import org.eclipse.kura.internal.rest.configuration.FailureHandler;

public class SnapshotId implements Validable {

    private final Long id;

    public SnapshotId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Override
    public void validate() {
        FailureHandler.requireParameter(id, "id");
    }

}
