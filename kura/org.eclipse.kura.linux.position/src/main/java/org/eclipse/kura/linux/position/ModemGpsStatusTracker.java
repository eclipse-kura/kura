/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.linux.position;

import static org.eclipse.kura.net.modem.ModemGpsDisabledEvent.MODEM_EVENT_GPS_DISABLED_TOPIC;
import static org.eclipse.kura.net.modem.ModemGpsEnabledEvent.MODEM_EVENT_GPS_ENABLED_TOPIC;

import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.net.modem.ModemGpsEnabledEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemGpsStatusTracker implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ModemGpsStatusTracker.class);

    private CommURI modemGpsPort;

    private GpsDeviceAvailabilityListener listener;

    public synchronized CommURI getGpsDeviceUri() {
        return this.modemGpsPort;
    }

    public synchronized void reset() {
        this.modemGpsPort = null;
    }

    @Override
    public synchronized void handleEvent(Event event) {
        logger.debug("Received event: {}", event.getTopic());

        if (MODEM_EVENT_GPS_ENABLED_TOPIC.equals(event.getTopic())) {

            handleModemGpsEnabledEvent(event);

        } else if (MODEM_EVENT_GPS_DISABLED_TOPIC.equals(event.getTopic())) {

            handleModemGpsDisabledEvent();
        }
    }

    private void handleModemGpsDisabledEvent() {
        if (this.modemGpsPort == null) {
            return;
        }

        this.modemGpsPort = null;
        notifyGpsAvailabilityChanged();
    }

    private void handleModemGpsEnabledEvent(final Event event) {

        if (this.modemGpsPort != null) {
            return;
        }

        try {
            final String port = (String) event.getProperty(ModemGpsEnabledEvent.PORT);
            final int baudRate = (Integer) event.getProperty(ModemGpsEnabledEvent.BAUD_RATE);
            final int dataBits = (Integer) event.getProperty(ModemGpsEnabledEvent.DATA_BITS);
            final int stopBits = (Integer) event.getProperty(ModemGpsEnabledEvent.STOP_BITS);
            final int parity = (Integer) event.getProperty(ModemGpsEnabledEvent.PARITY);

            final CommURI uri = new CommURI.Builder(port).withBaudRate(baudRate).withDataBits(dataBits)
                    .withStopBits(stopBits).withParity(parity).build();

            this.modemGpsPort = uri;
            notifyGpsAvailabilityChanged();
        } catch (Exception e) {
            logger.warn("Failed to build comm uri from ModemGpsEnabledEvent");
        }

    }

    private void notifyGpsAvailabilityChanged() {
        if (this.listener != null) {
            this.listener.onGpsDeviceAvailabilityChanged();
        }
    }

    public void setListener(final GpsDeviceAvailabilityListener listener) {
        this.listener = listener;
    }
}
