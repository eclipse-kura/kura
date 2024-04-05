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

import java.util.HashSet;
import java.util.Set;

public class PermissionConfigurationDTO {

    private Set<PermissionDTO> permissions = new HashSet<>();

    public void setPermissions(Set<PermissionDTO> permissions) {
        this.permissions = permissions;
    }

    public Set<PermissionDTO> getPermissions() {
        return this.permissions;
    }

    @Override
    public String toString() {
        return "PermissionConfigurationDTO [permissions=" + this.permissions + "]";
    }

}
