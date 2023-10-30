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

import java.util.HashSet;
import java.util.Set;

public class UserConfigDTO {

    private Set<UserDTO> userConfig = new HashSet<>();

    public Set<UserDTO> getUserConfig() {
        return this.userConfig;
    }

    public void setUserConfig(Set<UserDTO> userConfig) {
        this.userConfig = userConfig;
    }

}
