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

public class InterfaceNamesDTO {

    private final Set<String> interfaceNames;

    public InterfaceNamesDTO(final Set<String> interfaceNames) {
        this.interfaceNames = interfaceNames;
    }

    public void validate() throws KuraException {

        if (this.interfaceNames == null) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "interfaceNames cannot be null");
        }

        if (this.interfaceNames.isEmpty()) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "interfaceNames cannot be empty");
        }

        if (this.interfaceNames.stream().anyMatch(Objects::isNull)) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "interfaceNames elements cannot be null");
        }

        if (this.interfaceNames.stream().anyMatch(i -> i.trim().isEmpty())) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "interfaceNames elements cannot be empty");
        }
    }

    public Set<String> getInterfacesIds() {
        return this.interfaceNames;
    }
}
