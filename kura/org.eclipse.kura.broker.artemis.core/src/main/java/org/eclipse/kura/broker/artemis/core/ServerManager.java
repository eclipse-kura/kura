/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.broker.artemis.core;

import java.util.Collection;
import java.util.Set;

import org.apache.activemq.artemis.spi.core.protocol.ProtocolManagerFactory;
import org.eclipse.kura.broker.artemis.core.internal.ProtocolTracker;
import org.eclipse.kura.broker.artemis.core.internal.ProtocolTrackerListener;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerManager {

    private static final Logger logger = LoggerFactory.getLogger(ServerManager.class);

    private final ServerConfiguration configuration;
    private final ProtocolTracker protocolTracker;

    private final ProtocolTrackerListener listener = new ProtocolTrackerListener() {

        @Override
        public void protocolsAdded(final Set<String> protocols) {
            ServerManager.this.protocolsAdded(protocols);
        }

        @Override
        public void protocolsRemoved(final Set<String> protocols) {
            ServerManager.this.protocolsRemoved(protocols);
        }

    };

    private ServerRunner runner;

    public ServerManager(final ServerConfiguration configuration) {
        this.configuration = configuration;

        this.protocolTracker = new ProtocolTracker(FrameworkUtil.getBundle(ServerManager.class).getBundleContext(),
                this.listener);
    }

    public synchronized void start() throws Exception {
        this.protocolTracker.start();
        testStart();
    }

    public synchronized void stop() throws Exception {
        performStop();
        this.protocolTracker.stop();
    }

    protected synchronized void protocolsAdded(final Set<String> protocols) {
        logger.info("Protocols added - {}", protocols);
        try {
            testStart();
        } catch (final Exception e) {
            logger.warn("Failed to start", e);
        }
    }

    protected synchronized void protocolsRemoved(final Set<String> protocols) {
        logger.info("Protocols removed - {}", protocols);
        try {
            testStop();
        } catch (final Exception e) {
            logger.warn("Failed to stop", e);
        }
    }

    private void testStart() throws Exception {
        if (this.runner != null) {
            logger.debug("Already running");
            return;
        }

        final Collection<ProtocolManagerFactory<?>> protocols = this.protocolTracker
                .resolveProtocols(this.configuration.getRequiredProtocols());

        if (protocols == null) {
            logger.warn("Unable to resolve protocols: {}", this.configuration.getRequiredProtocols());
            return;
        }

        this.runner = new ServerRunner(this.configuration, protocols);
        this.runner.start();
    }

    private void testStop() throws Exception {
        if (this.runner == null) {
            logger.debug("Not running anyway");
            return;
        }

        final Collection<ProtocolManagerFactory<?>> protocols = this.protocolTracker
                .resolveProtocols(this.configuration.getRequiredProtocols());

        if (protocols != null) {
            return;
        }

        performStop();
    }

    private void performStop() throws Exception {
        this.runner.stop();
        this.runner = null;
    }

}
