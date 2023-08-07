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
package org.eclipse.kura.nm.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkStatusService;
import org.eclipse.kura.nm.NMDbusConnector;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NMStatusServiceImpl implements NetworkStatusService {

    private static final Logger logger = LoggerFactory.getLogger(NMStatusServiceImpl.class);

    private CommandExecutorService commandExecutorService;

    private NMDbusConnector nmDbusConnector;

    public void setCommandExecutorService(CommandExecutorService executorService) {
        this.commandExecutorService = executorService;
    }

    public NMStatusServiceImpl() {
        try {
            this.nmDbusConnector = NMDbusConnector.getInstance();
        } catch (DBusExecutionException | DBusException e) {
            logger.error("Cannot initialize NMDbusConnector due to: ", e);
        }
    }

    public NMStatusServiceImpl(NMDbusConnector nmDbusConnector) {
        this.nmDbusConnector = Objects.requireNonNull(nmDbusConnector);
    }

    public void activate() {
        logger.info("Activate NMStatusService...");
        logger.info("Activate NMStatusService... Done.");
    }

    public void update() {
        logger.info("Update NMStatusService...");
        logger.info("Update NMStatusService... Done.");
    }

    public void deactivate() {
        logger.info("Deactivate NMStatusService...");
        logger.info("Deactivate NMStatusService... Done.");
    }

    @Override
    public Optional<NetworkInterfaceStatus> getNetworkStatus(String interfaceId) throws KuraException {

        return getNetworkStatus(interfaceId, false);
    }

    @Override
    public List<String> getInterfaceIds() throws KuraException {
        List<String> interfaces = new ArrayList<>();
        try {
            interfaces = this.nmDbusConnector.getInterfaceIds();
        } catch (DBusException e) {
            throw new KuraIOException(e, "Could not retrieve interfaces from NM.");
        }

        return interfaces;
    }

    @Override
    public Optional<NetworkInterfaceStatus> getNetworkStatus(String interfaceId, boolean recompute)
            throws KuraException {

        Optional<NetworkInterfaceStatus> networkInterfaceStatus = Optional.empty();
        try {
            NetworkInterfaceStatus status = this.nmDbusConnector.getInterfaceStatus(interfaceId, recompute,
                    this.commandExecutorService);
            if (Objects.nonNull(status)) {
                networkInterfaceStatus = Optional.of(status);
            }
        } catch (UnknownMethod e) {
            throw new KuraIOException(e, "Could not retrieve status for " + interfaceId
                    + " interface from NM because the DBus object path references got invalidated.");
        } catch (DBusException e) {
            throw new KuraIOException(e, "Could not retrieve status for " + interfaceId + " interface from NM.");
        }

        return networkInterfaceStatus;
    }

}
