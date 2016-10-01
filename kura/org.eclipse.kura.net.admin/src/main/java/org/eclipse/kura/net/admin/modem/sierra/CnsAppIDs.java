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
package org.eclipse.kura.net.admin.modem.sierra;

public enum CnsAppIDs {

    /**
     * CnS 'MC572x Application' ID
     */
    MC572x_APPLICATION_ID(0x10000001),

    /**
     * CnS 'MC87xx Application' ID
     */
    MC87xx_APPLICATION_ID(0x10000002),

    /**
     * CnS 'C8xx Application' ID
     */
    C8xx_APPLICATION_ID(0x10000003),

    /**
     * CnS 'USB 598 Application' ID
     */
    USB598_APPLICATION_ID(0x10000004);

    private int m_appID = 0;

    private CnsAppIDs(int appID) {
        this.m_appID = appID;
    }

    public int getID() {
        return this.m_appID;
    }

}
