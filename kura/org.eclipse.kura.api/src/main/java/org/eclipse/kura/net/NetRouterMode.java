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
package org.eclipse.kura.net;

/**
 * Used to specify the route mode of each interface.
 */
public enum NetRouterMode {
    /** DHCP and NAT **/
    netRouterDchpNat,

    /** DHCP only **/
    netRouterDchp,

    /** NAT only **/
    netRouterNat,

    /** OFF **/
    netRouterOff;
}
