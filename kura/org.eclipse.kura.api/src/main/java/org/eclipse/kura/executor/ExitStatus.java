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
 * This interface provides a method to retrieve the exit status of a system command.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ExitStatus {

    /**
     * Returns a value representing the exit status of a command or process
     * 
     * @return an integer that represents the exit code
     */
    public int getExitCode();

    /**
     * Returns if a command or process is successful
     * 
     * @return a boolean that is true if the command is successful, false otherwise
     */
    public boolean isSuccessful();

}
