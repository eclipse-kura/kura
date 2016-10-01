/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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
