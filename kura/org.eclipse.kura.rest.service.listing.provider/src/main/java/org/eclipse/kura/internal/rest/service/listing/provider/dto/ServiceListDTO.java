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

import java.util.List;

public class ServiceListDTO {

    List<String> keystoreServices;

    public ServiceListDTO(List<String> entries) {
        this.keystoreServices = entries;
    }

    public List<String> getInterfacesIds() {
        return this.keystoreServices;
    }
}
