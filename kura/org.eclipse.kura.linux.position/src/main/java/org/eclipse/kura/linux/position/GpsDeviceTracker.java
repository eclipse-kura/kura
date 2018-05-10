/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.linux.position;

import java.io.File;
import java.util.Optional;

import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.usb.UsbService;
import org.eclipse.kura.usb.UsbTtyDevice;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GpsDeviceTracker implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(GpsDeviceTracker.class);

    private CommURI trackedUri;

    private UsbService usbService;
    private boolean isTrackedDeviceValid;

    private GpsDeviceAvailabilityListener listener;

    public void setUsbService(final UsbService usbService) {
        this.usbService = usbService;
    }

    public void unsetUsbService(final UsbService usbService) {
        this.usbService = null;
    }

    public synchronized CommURI track(final CommURI uri) {
        reset();

        this.trackedUri = uri;
        return getGpsDeviceUri();
    }

    public synchronized void reset() {
        this.trackedUri = null;
        this.isTrackedDeviceValid = false;
    }

    @Override
    public synchronized void handleEvent(Event event) {
        logger.debug("Received event: {}", event.getTopic());

        final boolean wasTrackedDeviceValid = this.isTrackedDeviceValid;

        getGpsDeviceUri();

        if (wasTrackedDeviceValid != this.isTrackedDeviceValid) {
            notifyGpsAvailabilityChanged();
        }

        if (this.isTrackedDeviceValid) {
            logger.debug("GPS device available");
        } else {
            logger.debug("GPS device not available");
        }
    }

    public synchronized void setListener(final GpsDeviceAvailabilityListener listener) {
        this.listener = listener;
    }

    public synchronized CommURI getGpsDeviceUri() {
        if (trackedUri == null) {
            return null;
        }

        final CommURI uri = resolve(trackedUri);
        if (!serialPortExists(uri)) {
            isTrackedDeviceValid = false;
            return null;
        }

        isTrackedDeviceValid = true;
        return uri;
    }

    public synchronized CommURI getTrackedUri() {
        return this.trackedUri;
    }

    protected CommURI resolve(final CommURI uri) {
        final String port = uri.getPort();
        String actualPort = port;

        if (!port.contains("/dev/") && !port.contains("COM")) {
            final Optional<UsbTtyDevice> portDevice = usbService.getUsbTtyDevices().stream()
                    .filter(ttyDev -> ttyDev.getUsbPort().equals(port)).findAny();
            if (portDevice.isPresent()) {
                actualPort = portDevice.get().getDeviceNode();
            }
        }

        return new CommURI.Builder(actualPort).withBaudRate(uri.getBaudRate()).withDataBits(uri.getDataBits())
                .withFlowControl(uri.getFlowControl()).withParity(uri.getParity()).withStopBits(uri.getStopBits())
                .build();
    }

    protected boolean serialPortExists(final CommURI uri) {
        return new File(uri.getPort()).exists();
    }

    private void notifyGpsAvailabilityChanged() {
        if (this.listener != null) {
            this.listener.onGpsDeviceAvailabilityChanged();
        }
    }

}
