/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.inventory.resources;

import java.util.Optional;

public class SystemBundleRef {

    private final String name;
    private final Optional<String> version;

    public SystemBundleRef(String name, Optional<String> version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getVersion() {
        return version;
    }
}
