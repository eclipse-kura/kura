/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */

package org.eclipse.kura.wire;

import static org.eclipse.kura.Preconditions.checkNull;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.NotThreadSafe;
import org.eclipse.kura.annotation.Nullable;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class WireConfiguration represents a wiring configuration between a Wire
 * Emitter and a Wire Receiver.
 */
@NotThreadSafe
public final class WireConfiguration {

	/** The Wire Emitter PID. */
	private final String emitterPid;

	/** The Filter. */
	@Nullable
	private final String filter;

	/** The Wire Receiver PID. */
	private final String receiverPid;

	/** The actual Wire Admin Wire. */
	private Wire wire;

	/**
	 * Instantiate a new wire configuration.
	 *
	 * @param emitterPid
	 *            the Wire Emitter PID
	 * @param receiverPid
	 *            the Wire Receiver PID
	 * @param filter
	 *            the filter
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null (except filter)
	 */
	public WireConfiguration(final String emitterPid, final String receiverPid, @Nullable final String filter) {
		checkNull(emitterPid, "Emitter PID cannot be null");
		checkNull(receiverPid, "Receiver PID cannot be null");

		this.emitterPid = emitterPid;
		this.receiverPid = receiverPid;
		this.filter = filter;
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
		if (this.filter == null) {
			if (other.filter != null) {
				return false;
			}
		} else if (!this.filter.equals(other.filter)) {
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

	/**
	 * Get the Wire Emitter PID.
	 *
	 * @return the Wire Emitter PID
	 */
	public String getEmitterPid() {
		return this.emitterPid;
	}

	/**
	 * Get the filter.
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
	 * Gets the wire.
	 *
	 * @return the wire
	 */
	public Wire getWire() {
		return this.wire;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((this.emitterPid == null) ? 0 : this.emitterPid.hashCode());
		result = (prime * result) + ((this.filter == null) ? 0 : this.filter.hashCode());
		result = (prime * result) + ((this.receiverPid == null) ? 0 : this.receiverPid.hashCode());
		return result;
	}

	/**
	 * Sets the wire.
	 *
	 * @param wire
	 *            the new wire
	 */
	public void setWire(final Wire wire) {
		this.wire = wire;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "WireConfiguration [emitterPid=" + this.emitterPid + ", filter=" + this.filter + ", receiverPid="
				+ this.receiverPid + "]";
	}

}
