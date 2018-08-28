/*******************************************************************************
 * Copyright (c) 2016, 2018 Eurotech and/or its affiliates and others
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

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.ServiceReference;

/**
 * The interface WireHelperService is an service utility API to provide quick
 * and necessary operations for Kura Wires topology.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.2
 */
@ProviderType
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
     * Returns a Wire Support instance of the provided wire component
     *
     * @param wireComponent
     *            the wire component
     * @param wireComponentRef
     *            the {@link ServiceReference} that contains the metadata/configuration related to the wireComponent
     * @return the wire support instance
     * @throws NullPointerException
     *             if the argument is null
     * @since 2.0
     */
    public WireSupport newWireSupport(WireComponent wireComponent, ServiceReference<WireComponent> wireComponentRef);

}
