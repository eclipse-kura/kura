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

public class InterfaceNamesDTO {

    private final List<String> interfaceNames;

    public InterfaceNamesDTO(final List<String> interfaceNames) {
        this.interfaceNames = interfaceNames;
    }

    public List<String> getInterfaceNames() {
        return interfaceNames;
    }
}
