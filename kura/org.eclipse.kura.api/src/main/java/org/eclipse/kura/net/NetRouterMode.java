/*******************************************************************************
 * Copyright (c) 2011, 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net;

/**
 * Used to specify the route mode of each interface.
 * 
 * @deprecated since version 3.0.
 */
@Deprecated
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
