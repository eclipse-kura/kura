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
 ******************************************************************************/
package org.eclipse.kura.security;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This marker interface is used to group the services that manage the network threats.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.2
 */
@ProviderType
public interface ThreatManagerService {

}
