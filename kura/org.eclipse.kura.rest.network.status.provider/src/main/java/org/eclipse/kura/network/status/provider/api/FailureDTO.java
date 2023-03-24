/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.network.status.provider.api;

@SuppressWarnings("unused")
public class FailureDTO {

    private final String interfaceId;
    private final String reason;

    public FailureDTO(final String interfaceId, final String reason) {
        this.interfaceId = interfaceId;
        this.reason = reason;
    }

    public FailureDTO(String interfaceId, final Exception reason) {
        this(interfaceId, reason.getMessage() != null ? reason.getMessage() : "Unknown error");
    }

}
