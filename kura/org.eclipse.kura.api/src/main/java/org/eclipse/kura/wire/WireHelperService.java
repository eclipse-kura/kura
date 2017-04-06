/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.wire;

import java.util.Optional;
import java.util.Set;

/**
 * The interface {@link WireHelperService} is an service utility API to provide quick
 * and necessary operations for Kura Wires topology.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface WireHelperService {

    /**
     * Retrieves the Kura Service PID (kura.service.pid) of the wire component
     *
     * @param wireComponent
     *            the wire component
     * @return the Service PID of the provided wire component or {@code null} if
     *         the provided Wire Component PID is not associated with any
     *         available Wire Component in the OSGi service registry
     * @throws NullPointerException
     *             if the argument is null
     */
    public String getPid(final WireComponent wireComponent);

    /**
     * Retrieves the OSGi Component Service PID (service.pid) of the provided
     * wire component PID
     *
     * @param wireComponentPid
     *            the wire component PID (kura.service.pid)
     * @return the Service PID of the provided wire component or {@code null} if
     *         the provided Wire Component PID is not associated with any
     *         available Wire Component in the OSGi service registry
     * @throws NullPointerException
     *             if the argument is null
     */
    public String getServicePid(final String wireComponentPid);

    /**
     * Retrieves the OSGi Component Service PID (service.pid) of the wire
     * component
     *
     * @param wireComponent
     *            the wire component
     * @return the Service PID of the provided wire component or {@code null} if
     *         the provided Wire Component PID is not associated with any
     *         available Wire Component in the OSGi service registry
     * @throws NullPointerException
     *             if the argument is null
     */
    public String getServicePid(final WireComponent wireComponent);

    /**
     * Retrieves the created {@link WireConfiguration} instance
     *
     * @param emitterPid
     *            the {@link WireEmitter} {@code ConfigurationService#KURA_SERVICE_PID}
     * @param receiverPid
     *            the {@link WireReceiver} {@code ConfigurationService#KURA_SERVICE_PID}
     * @return an {@link Optional} with the existent {@link WireConfiguration} instance if
     *         existent {@link WireConfiguration} instance is {@code non-null}, otherwise
     *         an empty {@link Optional}
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    public Optional<WireConfiguration> getWireConfiguration(String emitterPid, String receiverPid);

    /**
     * Retrieves the created {@link WireConfiguration} instances associated with the
     * provided {@link WireEmitter} {@code ConfigurationService#KURA_SERVICE_PID}
     *
     * @param emitterPid
     *            the {@link WireEmitter} {@code ConfigurationService#KURA_SERVICE_PID}
     * @return the collection of existent {@link WireConfiguration} instances or empty
     * @throws NullPointerException
     *             if the argument is null
     */
    public Set<WireConfiguration> getWireConfigurationsByEmitterPid(String emitterPid);

    /**
     * Retrieves the created {@link WireConfiguration} instances associated with the
     * provided {@link WireReceiver} {@code ConfigurationService#KURA_SERVICE_PID}
     *
     * @param receiverPid
     *            the {@link WireReceiver} {@code ConfigurationService#KURA_SERVICE_PID}
     * @return the collection of existent {@link WireConfiguration} instances or empty
     * @throws NullPointerException
     *             if the argument is null
     */
    public Set<WireConfiguration> getWireConfigurationsByReceiverPid(String receiverPid);

    /**
     * Checks whether the provided Wire Component PID belongs to a Wire Emitter
     *
     * @param wireComponentPid
     *            the wire component PID (kura.service.pid)
     * @return true if the provided Wire Component PID belongs to a Wire Emitter
     * @throws NullPointerException
     *             if the argument is null
     */
    public boolean isEmitter(final String wireComponentPid);

    /**
     * Checks whether the provided Wire Component PID belongs to a Wire Receiver
     *
     * @param wireComponentPid
     *            the wire component PID (kura.service.pid)
     * @return true if the provided Wire Component PID belongs to a Wire
     *         Receiver
     * @throws NullPointerException
     *             if the argument is null
     */
    public boolean isReceiver(final String wireComponentPid);

    /**
     * Returns a {@link WireSupport} instance of the provided {@link WireComponent}
     * instance
     *
     * @param wireComponent
     *            the {@link WireComponent}
     *            instance
     * @return the {@link WireSupport} instance
     * @throws NullPointerException
     *             if the argument is null
     */
    public WireSupport newWireSupport(WireComponent wireComponent);

}
