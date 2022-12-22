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
 ******************************************************************************/
package org.eclipse.kura.net2;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The NetworkConfigurationService is a marker interface
 * used to identify the services responsible for storing
 * and applying a network configuration to the system.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface NetworkConfigurationService {

}
