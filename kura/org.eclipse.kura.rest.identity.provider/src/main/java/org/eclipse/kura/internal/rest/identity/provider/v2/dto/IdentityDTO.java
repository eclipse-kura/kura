/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.identity.provider.v2.dto;

import static java.util.Objects.requireNonNull;
import static org.eclipse.kura.internal.rest.identity.provider.util.StringUtils.requireNotEmpty;

public class IdentityDTO {

    private final String name;

    public IdentityDTO(String name) {
        requireNonNull(name, "name cannot be null");
        requireNotEmpty(name, "name cannot be empty");

        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "IdentityDTO [name=" + this.name + "]";
    }
}
