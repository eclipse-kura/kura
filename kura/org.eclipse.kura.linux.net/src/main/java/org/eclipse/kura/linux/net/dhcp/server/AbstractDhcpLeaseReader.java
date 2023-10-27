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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.dhcp.DhcpLease;

public abstract class AbstractDhcpLeaseReader {

    protected abstract List<DhcpLease> parseDhcpLeases(InputStream in);

    protected List<DhcpLease> getDhcpLeases(String[] commandLine, CommandExecutorService commandService)
            throws KuraException {
        CommandStatus status = execute(commandLine, commandService);

        if (!status.getExitStatus().isSuccessful()) {
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, String.join(" ", commandLine),
                    status.getExitStatus().getExitCode());
        }

        return parseDhcpLeases(
                new ByteArrayInputStream(((ByteArrayOutputStream) status.getOutputStream()).toByteArray()));
    }

    private static CommandStatus execute(final String[] commandLine, CommandExecutorService executorService)
            throws KuraException {
        Command command = new Command(commandLine);
        command.setOutputStream(new ByteArrayOutputStream());
        return executorService.execute(command);
    }

}
