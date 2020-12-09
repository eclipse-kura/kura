/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.executor;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This is a marker interface for the {@link CommandExecutorService}. It provides methods for starting system processes
 * or executing system commands using a specific user. The commands are run with the permissions that are granted to
 * that user.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.2
 */
@ProviderType
public interface UnprivilegedExecutorService extends CommandExecutorService {

}
