/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.deployment;

public enum InstallStatus {
    IDLE("IDLE"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    ALREADY_DONE("ALREADY DONE");

    private final String status;

    InstallStatus(String status) {
        this.status = status;
    }

    public String getStatusString() {
        return this.status;
    }
}