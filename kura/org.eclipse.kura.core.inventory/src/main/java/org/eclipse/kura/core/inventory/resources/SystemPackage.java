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

import org.eclipse.kura.system.SystemResourceInfo;
import org.eclipse.kura.system.SystemResourceType;

public class SystemPackage extends SystemResourceInfo {

    public SystemPackage(String name) {
        super(name);
    }

    public SystemPackage(String name, String version, SystemResourceType type) {
        super(name, version, type);
    }

}
