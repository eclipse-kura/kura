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
 *******************************************************************************/
package org.eclipse.kura.linux.clock;

import java.util.Date;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NtpdClockSyncProvider extends AbstractNtpClockSyncProvider {

    private static final Logger logger = LoggerFactory.getLogger(NtpdClockSyncProvider.class);

    private CommandExecutorService executorService;

    public NtpdClockSyncProvider(CommandExecutorService service) {
        this.executorService = service;
    }

    // ----------------------------------------------------------------
    //
    // Concrete Methods
    //
    // ----------------------------------------------------------------

    @Override
    protected boolean syncClock() throws KuraException {
        boolean ret = false;
        try {
            // Execute a native Linux command to perform the NTP time sync.
            int ntpTimeout = this.ntpTimeout / 1000;
            Command command = new Command(new String[] { "ntpdate", "-t", Integer.toString(ntpTimeout), this.ntpHost });
            command.setTimeout(60);
            CommandStatus status = this.executorService.execute(command);
            if (status.getExitStatus().isSuccessful()) {
                logger.info("System Clock Synchronized with {}", this.ntpHost);
                this.lastSync = new Date();

                // Call update method with 0 offset to ensure the clock event gets fired and the HW clock
                // is updated if desired.
                this.listener.onClockUpdate(0);
                ret = true;
            } else {
                logger.warn(
                        "Error while synchronizing System Clock with NTP host {}. Please verify network connectivity ...",
                        this.ntpHost);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
        return ret;
    }

}
