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
package org.eclipse.kura.internal.rest.identity.provider.dto;

import java.util.Set;

public class PermissionDTO {

    private final Set<String> permissions;

    public PermissionDTO(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Set<String> getPermissions() {
        return this.permissions;
    }

}
