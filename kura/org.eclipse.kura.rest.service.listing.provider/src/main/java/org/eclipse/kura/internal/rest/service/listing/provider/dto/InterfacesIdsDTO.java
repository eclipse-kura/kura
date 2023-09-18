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

package org.eclipse.kura.internal.rest.service.listing.provider.dto;

import java.util.Objects;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class InterfacesIdsDTO {

    private final Set<String> interfacesIds;

    public InterfacesIdsDTO(final Set<String> interfaceIds) {
        this.interfacesIds = interfaceIds;
    }

    public void idsValidation() throws KuraException {

        if (this.interfacesIds == null) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "interfacesIds must not be null");
        }

        if (this.interfacesIds.isEmpty()) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "interfacesIds must not be empty");
        }

        if (this.interfacesIds.stream().anyMatch(Objects::isNull)) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "none of the interfacesIds can be null");
        }

        if (this.interfacesIds.stream().anyMatch(i -> i.trim().isEmpty())) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "none of the interfacesIds can be empty");
        }
    }

    public Set<String> getInterfacesIds() {
        return this.interfacesIds;
    }
}
