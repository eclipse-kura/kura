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

    private int appID = 0;

    private CnsAppIDs(int appID) {
        this.appID = appID;
    }

    public int getID() {
        return this.appID;
    }

}
