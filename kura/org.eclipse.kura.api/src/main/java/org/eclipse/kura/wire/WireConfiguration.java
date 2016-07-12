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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * The Class WireConfiguration represents a wiring configuration between a Wire
 * Emitter and a Wire Receiver
 */
@NotThreadSafe
public final class WireConfiguration {

	/** The Wire Emitter PID. */
	private String emitterPid;

	/** The Filter. */
	@Nullable
	private final String filter;

	/**
	 * This signifies if Wire Admin Service has already created the wire between
	 * the wire emitter and the wire receiver.
	 */
	private boolean isCreated;

	/** The Wire Receiver PID. */
	private String receiverPid;

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
		this.isCreated = false;
	}

	/**
	 * Instantiate a new wire configuration.
	 *
	 * @param emitterPid
	 *            the Wire Emitter PID
	 * @param receiverPid
	 *            the Wire Receiver PID
	 * @param filter
	 *            the filter
	 * @param isCreated
	 *            the created flag signifying whether Wire Admin has already
	 *            created the wire between the wire emitter and a wire receiver
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null (except filter)
	 */
	public WireConfiguration(final String emitterPid, final String receiverPid, @Nullable final String filter,
			final boolean isCreated) {
		checkNull(emitterPid, "Emitter PID cannot be null");
		checkNull(receiverPid, "Receiver PID cannot be null");

		this.emitterPid = emitterPid;
		this.receiverPid = receiverPid;
		this.filter = filter;
		this.isCreated = isCreated;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof WireConfiguration) {
			final WireConfiguration wireConfiguration = (WireConfiguration) obj;
			return Objects.equal(this.emitterPid, wireConfiguration.getEmitterPid())
					&& Objects.equal(this.receiverPid, wireConfiguration.getReceiverPid())
					&& Objects.equal(this.isCreated, wireConfiguration.isCreated());
		}
		return false;
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

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Objects.hashCode(this.isCreated, this.emitterPid, this.receiverPid);
	}

	/**
	 * Check if is wire admin has already created a wire between the wire
	 * emitter and a wire receiver
	 *
	 * @return true, if it is created
	 */
	public boolean isCreated() {
		return this.isCreated;
	}

	/**
	 * Set the value to the flag to check whether wire admin has already created
	 * a wire between the wire emitter and a wire receiver
	 *
	 * @param isCreated
	 *            the new created flag
	 */
	public void setCreated(final boolean isCreated) {
		this.isCreated = isCreated;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("emitter_pid", this.emitterPid)
				.add("receiver_pid", this.receiverPid).add("filter", this.filter).toString();
	}

	/**
	 * Update the PIDs of the Wire Components associated with this Wire
	 * Configuration
	 *
	 * @param newEmitterPid
	 *            the new Wire Emitter PID
	 * @param newReceiverPid
	 *            the new Wire Receiver PID
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public void update(final String newEmitterPid, final String newReceiverPid) {
		checkNull(newEmitterPid, "Emitter PID cannot be null");
		checkNull(newReceiverPid, "Receiver PID cannot be null");

		this.emitterPid = newEmitterPid;
		this.receiverPid = newReceiverPid;
	}
}
