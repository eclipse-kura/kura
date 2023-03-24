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

import java.util.List;
import java.util.Objects;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class InterfaceIdsDTO {

    private final List<String> interfaceIds;

    public InterfaceIdsDTO(final List<String> interfaceIds) {
        this.interfaceIds = interfaceIds;
    }

    public List<String> getInterfaceIds() {
        return this.interfaceIds;
    }

    public void validate() throws KuraException {
        if (this.interfaceIds == null) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "interfaceIds must be specified");
        }

        if (this.interfaceIds.isEmpty()) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "interfaceIds cannot be empty");
        }

        if (this.interfaceIds.stream().anyMatch(Objects::isNull)) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "null interfaceIds are not allowed");
        }

        if (this.interfaceIds.stream().anyMatch(i -> i.trim().isEmpty())) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "empty interfaceIds are not allowed");
        }
    }
}
