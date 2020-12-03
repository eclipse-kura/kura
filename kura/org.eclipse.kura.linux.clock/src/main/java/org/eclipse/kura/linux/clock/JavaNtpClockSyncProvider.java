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
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.linux.clock;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaNtpClockSyncProvider extends AbstractNtpClockSyncProvider {

    private static final Logger logger = LoggerFactory.getLogger(JavaNtpClockSyncProvider.class);

    // ----------------------------------------------------------------
    //
    // Concrete Methods
    //
    // ----------------------------------------------------------------

    @Override
    protected boolean syncClock() throws KuraException {
        boolean ret = false;
        // connect and get the delta
        NTPUDPClient ntpClient = new NTPUDPClient();
        ntpClient.setDefaultTimeout(this.ntpTimeout);
        try {
            ntpClient.open();
            try {
                InetAddress ntpHostAddr = InetAddress.getByName(this.ntpHost);
                TimeInfo info = ntpClient.getTime(ntpHostAddr, this.ntpPort);
                this.lastSync = new Date();
                info.computeDetails();
                Long delayValue = info.getDelay();
                if (delayValue != null && delayValue.longValue() < 1000) {
                    this.listener.onClockUpdate(info.getOffset());
                    ret = true;
                } else {
                    logger.error("Incorrect delay value({}), clock will not be updated", info.getDelay());
                }
            } catch (IOException e) {
                logger.warn(
                        "Error while synchronizing System Clock with NTP host {}. Please verify network connectivity ...",
                        this.ntpHost);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e);
        } finally {
            ntpClient.close();
        }
        return ret;
    }
}
