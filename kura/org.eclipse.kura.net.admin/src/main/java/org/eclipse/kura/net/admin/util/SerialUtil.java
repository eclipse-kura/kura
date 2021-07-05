/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.util;

import java.util.OptionalInt;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.comm.CommConnection;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.io.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SerialUtil {

    private static final String RECEIVE_TIMEOUT_PROP_NAME = "net.admin.modem.receive.timeout.ms";
    private static final Logger logger = LoggerFactory.getLogger(SerialUtil.class);
    private static final String DEFAULT_RECEIVE_TIMEOUT = "5000";
    private static OptionalInt receiveTimeout;

    private SerialUtil() {
    }

    public static synchronized OptionalInt getReceiveTimeout() {
        if (receiveTimeout == null) {
            receiveTimeout = getReceiveTimeoutInternal();
        }

        return receiveTimeout;
    }

    public static CommConnection openSerialPort(final ConnectionFactory factory, final String port)
            throws KuraException {

        CommConnection connection = null;

        final OptionalInt currentReceiveTimeout = SerialUtil.getReceiveTimeout();

        CommURI.Builder builder = new CommURI.Builder(port).withBaudRate(115200).withDataBits(8).withStopBits(1)
                .withParity(0).withOpenTimeout(2000);

        if (currentReceiveTimeout.isPresent()) {
            logger.debug("Setting receive timeout to {} ms", currentReceiveTimeout.getAsInt());

            builder = builder.withReceiveTimeout(currentReceiveTimeout.getAsInt());
        }

        final String uri = builder.build().toString();

        try {
            connection = (CommConnection) factory.createConnection(uri, 1, false);
        } catch (Exception e) {
            logger.debug("Exception creating connection", e);
            throw new KuraException(KuraErrorCode.CONNECTION_FAILED, e, "Connection Failed");
        }

        return connection;
    }

    private static OptionalInt getReceiveTimeoutInternal() {
        final BundleContext bundleContext = FrameworkUtil.getBundle(SerialUtil.class).getBundleContext();

        final ServiceReference<SystemService> systemServiceRef = bundleContext.getServiceReference(SystemService.class);

        OptionalInt result = OptionalInt.empty();

        if (systemServiceRef == null) {
            return result;
        }

        SystemService systemService = null;

        try {
            systemService = bundleContext.getService(systemServiceRef);

            final String raw = systemService.getProperties().getProperty(RECEIVE_TIMEOUT_PROP_NAME,
                    DEFAULT_RECEIVE_TIMEOUT);

            final int parsed = Integer.parseInt(raw);

            if (parsed > 0) {
                result = OptionalInt.of(parsed);
            } else {
                result = OptionalInt.empty();
            }

        } catch (final Exception e) {
            result = OptionalInt.empty();
        } finally {
            if (systemService != null) {
                bundleContext.ungetService(systemServiceRef);
            }
        }

        return result;
    }
}
