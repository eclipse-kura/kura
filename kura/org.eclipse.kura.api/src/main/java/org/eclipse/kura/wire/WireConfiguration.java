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

import static java.util.Objects.requireNonNull;

import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.annotation.Nullable;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;

/**
 * The Class {@link WireConfiguration} represents a wiring configuration between a Wire
 * Emitter and a Wire Receiver. In a Wire Graph in Kura Wires, the connection that
 * connect two different Wire Components is known as {@link WireConfiguration}.
 * <br/>
 * <br/>
 * Two {@link WireConfiguration}s with equal {@code Emitter PID} and {@code Receiver PID}
 * are considered to be equal {@link WireConfiguration} instances and it is validated through
 * its {@link WireConfiguration#equals(Object)} and {@link WireConfiguration#hashCode()}
 * methods' contract and hence, it is suitable to be used with hash based collections.
 *
 * @see Wire
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.2
 */
@NotThreadSafe
@ProviderType
public class WireConfiguration {

    private final String emitterPid;

    @Nullable
    private String filter;

    private final String receiverPid;

    @Nullable
    private Wire wire;

    /**
     * Instantiates a new {@link WireConfiguration}.
     *
     * @param emitterPid
     *            the Wire Emitter PID
     * @param receiverPid
     *            the Wire Receiver PID
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    public WireConfiguration(final String emitterPid, final String receiverPid) {
        requireNonNull(emitterPid, "Emitter PID cannot be null");
        requireNonNull(receiverPid, "Receiver PID cannot be null");

        this.emitterPid = emitterPid;
        this.receiverPid = receiverPid;
    }

    /**
     * Gets the Wire Emitter PID.
     *
     * @return the Wire Emitter PID
     */
    public String getEmitterPid() {
        return this.emitterPid;
    }

    /**
     * Gets the associated filter.
     *
     * @return the filter
     */
    public String getFilter() {
        return this.filter;
    }

    /**
     * Gets the Wire Receiver PID.
     *
     * @return the Wire Receiver PID
     */
    public String getReceiverPid() {
        return this.receiverPid;
    }

    /**
     * Gets the associated {@link WireAdmin}' {@link Wire} instance.
     *
     * @return the {@link Wire} instance
     */
    public Wire getWire() {
        return this.wire;
    }

    /**
     * Sets the filter for this {@link WireConfiguration}
     *
     * @param filter
     *            the new filter
     */
    public void setFilter(final String filter) {
        this.filter = filter;
    }

    /**
     * Sets the {@link Wire} instance.
     *
     * @param wire
     *            the new {@link Wire} instance
     */
    public void setWire(final Wire wire) {
        this.wire = wire;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final WireConfiguration other = (WireConfiguration) obj;
        if (this.emitterPid == null) {
            if (other.emitterPid != null) {
                return false;
            }
        } else if (!this.emitterPid.equals(other.emitterPid)) {
            return false;
        }
        if (this.receiverPid == null) {
            if (other.receiverPid != null) {
                return false;
            }
        } else if (!this.receiverPid.equals(other.receiverPid)) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.emitterPid == null) ? 0 : this.emitterPid.hashCode());
        result = (prime * result) + ((this.receiverPid == null) ? 0 : this.receiverPid.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "WireConfiguration [emitterPid=" + this.emitterPid + ", filter=" + this.filter + ", receiverPid="
                + this.receiverPid + ", wire=" + this.wire + "]";
    }

}