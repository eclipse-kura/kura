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
package org.eclipse.kura.command;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface provides methods for running system commands from the web console.
 *
 * @deprecated use {@link org.eclipse.kura.executor.CommandExecutorService}
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
@Deprecated
public interface CommandService {

    @Deprecated
    public String execute(String cmd) throws KuraException;
}
