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
 *******************************************************************************/
package org.eclipse.kura.core.inventory.resources;

import java.util.List;

public class SystemPackages {

    private List<SystemPackage> packages;

    public SystemPackages(List<SystemPackage> packages) {
        this.packages = packages;
    }

    public List<SystemPackage> getSystemPackages() {
        return this.packages;
    }

    public void setSystemPackages(List<SystemPackage> packages) {
        this.packages = packages;
    }

}
