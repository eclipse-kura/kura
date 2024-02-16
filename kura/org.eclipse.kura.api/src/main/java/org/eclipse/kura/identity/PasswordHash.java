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
 * Represents a password hash computed using an implementation defined
 * algorithm. The implementation must override the
 * {@link Object#hashCode()} and {@link Object#equals()} methods.
 * <br>
 * <br>
 * Instances of this class can be constructed using the
 * {@link IdentityService#computePasswordHash(char[])}.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.7.0
 */
@ProviderType
public interface PasswordHash {
}
