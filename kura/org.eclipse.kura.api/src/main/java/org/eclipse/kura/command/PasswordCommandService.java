/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
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
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;

/**
 * This interface provides methods for running system commands from the web console.
 *
 */
public interface PasswordCommandService {

    /**
     * Password protected command execution service
     *
     * @param cmd
     *            Command to be executed
     * @param password
     *            Password as specified in the CommandService
     * @return String output as returned by the command
     * @throws KuraException
     *             raised if the command service is disabled, if the password is not correct
     *             or if an internal error occurs
     */
    public String execute(String cmd, String password) throws KuraException;

    /**
     * Password protected command execution service
     *
     * @param commandReq
     *            Payload containing command information
     * @return KuraResponsePayload containing the result of the command execution and details on the result
     */
    public KuraResponsePayload execute(KuraRequestPayload commandReq);
}
