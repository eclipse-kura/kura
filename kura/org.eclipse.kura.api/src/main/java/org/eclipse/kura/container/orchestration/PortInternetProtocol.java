/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.container.orchestration;

/**
 * 
 * This is an enum containing all supported internet protocols that can be run at a port in a container.
 * 
 * @since 2.5
 *
 */
public enum PortInternetProtocol {
    TCP,
    UDP,
    SCTP

}
