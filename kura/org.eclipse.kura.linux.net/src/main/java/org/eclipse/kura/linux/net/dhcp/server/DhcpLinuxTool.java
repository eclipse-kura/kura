/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/

package org.eclipse.kura.linux.net.dhcp.server;

import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.executor.CommandStatus;

public interface DhcpLinuxTool {

    public boolean isRunning(String interfaceName) throws KuraProcessExecutionErrorException;

    public CommandStatus startInterface(String interfaceName) throws KuraProcessExecutionErrorException;

    public boolean disableInterface(String interfaceName) throws KuraProcessExecutionErrorException;

}
