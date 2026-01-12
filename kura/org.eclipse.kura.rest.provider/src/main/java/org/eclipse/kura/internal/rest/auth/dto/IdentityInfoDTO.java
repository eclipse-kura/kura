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
package org.eclipse.kura.internal.rest.auth.dto;

import java.util.Set;

public class IdentityInfoDTO {

    private final String name;
    private final boolean passwordChangeNeeded;
    private final Set<String> permissions;

    public IdentityInfoDTO(String name, boolean passwordChangeNeeded, Set<String> permissions) {
        this.name = name;
        this.passwordChangeNeeded = passwordChangeNeeded;
        this.permissions = permissions;
    }

    public String getName() {
        return name;
    }

    public boolean isPasswordChangeNeeded() {
        return passwordChangeNeeded;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

}
