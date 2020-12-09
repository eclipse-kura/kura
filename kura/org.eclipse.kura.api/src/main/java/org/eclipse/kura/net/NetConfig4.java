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
 ******************************************************************************/
package org.eclipse.kura.net;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Marker interface for IPv4-based configurations of network interfaces
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface NetConfig4 extends NetConfig {

}
