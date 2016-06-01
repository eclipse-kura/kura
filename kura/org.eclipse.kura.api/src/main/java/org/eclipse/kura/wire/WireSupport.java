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

import java.util.List;

import org.eclipse.kura.KuraRuntimeException;
import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Producer;
import org.osgi.service.wireadmin.Wire;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * The Class WireSupport is responsible for managing incoming as well as
 * outgoing wires of the contained Wire Component. This is also used to perform
 * wire related operations for instance, emit and receive wire records
 */
public final class WireSupport implements Producer, Consumer {

	/** The incoming wires. */
	private List<Wire> incomingWires;

	/** The outgoing wires. */
	private List<Wire> outgoingWires;

	/** The supported Wire Component */
	private final WireComponent wireSupporter;

	/**
	 * Instantiates a new wire support.
	 *
	 * @param wireSupporter
	 *            the wire supporter component
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public WireSupport(final WireComponent wireSupporter) {
		checkNull(wireSupporter, "Wire supported component cannot be null");
		this.outgoingWires = Lists.newArrayList();
		this.incomingWires = Lists.newArrayList();
		this.wireSupporter = wireSupporter;
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void consumersConnected(final Wire[] wires) {
		this.outgoingWires = Lists.newArrayList(wires);
	}

	/**
	 * Emit the provided wire records
	 *
	 * @param wireRecords
	 *            the wire records
	 */
	public synchronized void emit(final List<WireRecord> wireRecords) {
		if (this.wireSupporter instanceof WireEmitter) {
			final String emitterPid = this.wireSupporter.getName();
			final WireEnvelope wei = new WireEnvelope(emitterPid, wireRecords);
			for (final Wire wire : this.outgoingWires) {
				wire.update(wei);
			}
		}
	}

	/**
	 * Emit the provided wire records
	 *
	 * @param wireRecords
	 *            the wire records to be emitted
	 */
	public synchronized void emit(final WireRecord... wireRecords) {
		this.emit(Lists.newArrayList(wireRecords));
	}

	/**
	 * Gets the incoming wires.
	 *
	 * @return the incoming wires
	 */
	public List<Wire> getIncomingWires() {
		return ImmutableList.copyOf(this.incomingWires);
	}

	/**
	 * Gets the outgoing wires.
	 *
	 * @return the outgoing wires
	 */
	public List<Wire> getOutgoingWires() {
		return ImmutableList.copyOf(this.outgoingWires);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized Object polled(final Wire wire) {
		return wire.getLastValue();
	}

	/** {@inheritDoc} */
	@Override
	public void producersConnected(final Wire[] wires) {
		this.incomingWires = Lists.newArrayList(wires);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("incoming_wires", this.incomingWires)
				.add("outgoing_wires", this.outgoingWires).add("wire_component", this.wireSupporter).toString();
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		checkNull(wire, "Wire cannot be null");
		if ((value instanceof WireEnvelope) && (this.wireSupporter instanceof WireReceiver)) {
			((WireReceiver) this.wireSupporter).onWireReceive((WireEnvelope) value);
		}
	}
}
