/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.emulator;

import org.osgi.service.component.ComponentContext;

public class Emulator {

    @SuppressWarnings("unused")
    private ComponentContext m_componentContext;

    protected void activate(ComponentContext componentContext) {
        m_componentContext = componentContext;

        try {
            // Properties props = System.getProperties();
            String mode = System.getProperty("org.eclipse.kura.mode");
            if (mode.equals("emulator")) {
                System.out.println("Framework is running in emulation mode");
            } else {
                System.out.println("Framework is not running in emulation mode");
            }
        } catch (Exception e) {
            System.out.println("Framework is not running in emulation mode");
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        m_componentContext = null;
    }
}
