/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.executor;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This is a marker interface for the {@link CommandExecutorService}. It provides methods for starting system processes
 * or executing system commands using a privileged user. The privileged user is the same that started Kura, so this
 * interface provides the highest available level of permissions.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.2
 */
@ProviderType
public interface PrivilegedExecutorService extends CommandExecutorService {

}
