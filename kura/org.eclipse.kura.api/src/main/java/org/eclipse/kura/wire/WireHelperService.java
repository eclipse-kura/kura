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

import org.osgi.annotation.versioning.ProviderType;

/**
 * The interface {@link WireHelperService} is an service utility API to provide quick
 * and necessary operations for Kura Wires topology
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.2
 */
@ProviderType
public interface WireHelperService {

    /**
     * Retrieves the Kura PID ({@code kura.service.pid}) of the {@link WireComponent}
     * instance
     *
     * @param wireComponent
     *            the {@link WireComponent} instance
     * @return the PID of the provided {@link WireComponent} instance wrapped
     *         in {@link Optional} instance or an empty {@link Optional} instance
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public Optional<String> getPid(WireComponent wireComponent);

    /**
     * Retrieves the OSGi Component Service PID ({@code service.pid}) of the provided
     * {@link WireComponent} PID ({@code kura.service.pid})
     *
     * @param wireComponentPid
     *            the {@link WireComponent} PID ({@code kura.service.pid})
     * @return the Service PID of the provided {@link WireComponent} PID
     *         ({@code kura.service.pid}) wrapped in {@link Optional} instance or
     *         an empty {@link Optional} instance
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public Optional<String> getServicePid(String wireComponentPid);

    /**
     * Retrieves the OSGi Component Service PID ({@code service.pid}) of the {@link WireComponent}
     *
     * @param wireComponent
     *            the {@link WireComponent} instance
     * @return the Service PID ({@code service.pid}) of the provided {@link WireComponent}
     *         instance wrapped in {@link Optional} instance or an empty {@link Optional} instance
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public Optional<String> getServicePid(WireComponent wireComponent);

    /**
     * Checks whether the provided {@link WireComponent} PID ({@code kura.service.pid}) belongs
     * to a {@link WireEmitter}
     *
     * @param wireComponentPid
     *            the {@link WireComponent} PID ({@code kura.service.pid})
     * @return true if the provided {@link WireComponent} PID ({@code kura.service.pid})
     *         belongs to a {@link WireEmitter}
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public boolean isEmitter(String wireComponentPid);

    /**
     * Checks whether the provided {@link WireComponent} PID ({@code kura.service.pid}) belongs
     * to a {@link WireReceiver}
     *
     * @param wireComponentPid
     *            the {@link WireComponent} PID ({@code kura.service.pid})
     * @return true if the provided {@link WireComponent} PID ({@code kura.service.pid})
     *         belongs to a {@link WireReceiver}
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public boolean isReceiver(String wireComponentPid);

    /**
     * Returns a {@link WireSupport} instance of the provided {@link WireComponent} instance
     *
     * @param wireComponent
     *            the {@link WireComponent} instance
     * @return the {@link WireSupport} instance
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public WireSupport newWireSupport(WireComponent wireComponent);

}
