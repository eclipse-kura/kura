/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *     Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire;

import java.util.Set;

import org.apache.felix.service.command.Descriptor;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireService;

/**
 * Provides Gogo Shell commands to create, delete Wire Configurations and list
 * the available ones
 */
public final class WireServiceCommandProvider {

    private volatile WireService wireService;

    /**
     * Binds the Wire Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void bindWireService(final WireService wireHelperService) {
        if (this.wireService == null) {
            this.wireService = wireHelperService;
        }
    }

    /**
     * Unbinds the Wire Service.
     *
     * @param wireHelperService
     *            the new Wire Helper Service
     */
    public synchronized void unbindWireService(final WireService wireHelperService) {
        if (this.wireService == wireHelperService) {
            this.wireService = null;
        }
    }

    /**
     * The command {@code createWire} creates a Wire Configuration between the
     * provided emitter PID and receiver PID
     *
     * @throws KuraException
     *             In case of an error
     */
    @Descriptor("Creates a Wire Configuration between the provided Emitter and Receiver")
    public void createWire(@Descriptor("Emitter PID") final String emitterPid,
            @Descriptor("Receiver PID") final String receiverPid, @Descriptor("Filter") final String filter)
            throws KuraException {
        final WireConfiguration wireConfiguration = this.wireService.createWireConfiguration(emitterPid, receiverPid);
        wireConfiguration.setFilter(filter);
    }

    /**
     * The command {@code deleteWire} delete existing Wire Configuration between
     * the provided emitter PID and receiver PID
     *
     * @param emitterPid
     *            the emitter PID
     * @param receiverPid
     *            the receiver PID
     */
    @Descriptor("Deletes the already created Wire Configuration between the provided Emitter and Receiver")
    public void deleteWire(@Descriptor("Emitter PID") final String emitterPid,
            @Descriptor("Receiver PID") final String receiverPid) {
        for (final WireConfiguration configuration : this.wireService.getWireConfigurations()) {
            if (configuration.getEmitterPid().equals(emitterPid)
                    && configuration.getReceiverPid().equals(receiverPid)) {
                this.wireService.deleteWireConfiguration(configuration);
            }
        }
    }

    /**
     * The command {@code listWires} lists all the available Wire Configurations
     *
     * @param ci
     *            the console interpreter
     */
    @Descriptor("List all created Wire Configurations")
    public void listWires() {
        System.out.println("=================== Wire Configurations ===================");
        final Set<WireConfiguration> configs = this.wireService.getWireConfigurations();
        int i = 0;
        for (final WireConfiguration config : configs) {
            System.out.format("%d. Emitter PID ===> %s  Receiver PID ===> %s  Filter ===> %s%n", ++i,
                    config.getEmitterPid(), config.getReceiverPid(), config.getFilter());
        }
        System.out.println("===========================================================");
    }
}
