/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.identity;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a portion of the configuration of an identity that can be
 * retrieved and updated individually.
 * 
 * The currently supported types are {@link PasswordConfiguration},
 * {@link AssignedPermissions} and {@link AdditionalConfigurations}.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.7.0
 */
@ProviderType
public interface IdentityConfigurationComponent {
}
