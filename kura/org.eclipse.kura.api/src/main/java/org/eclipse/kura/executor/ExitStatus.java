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
 * This interface provides a method to retrieve the exit status of a system command.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.2
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
