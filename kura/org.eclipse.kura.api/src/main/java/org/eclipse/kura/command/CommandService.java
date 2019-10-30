/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.command;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface provides methods for running system commands from the web console.
 *
 * @deprecated use {@link CommandExecutorService}
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
@Deprecated
public interface CommandService {

    @Deprecated
    public String execute(String cmd) throws KuraException;
}
