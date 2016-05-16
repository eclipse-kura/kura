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
package org.eclipse.kura.wire.store;

import static org.eclipse.kura.device.internal.DevicePreconditions.checkCondition;

import java.util.Map;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.db.DbService;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;

/**
 * The Class DbWireRecordFilter is responsible for representing a wire component
 * which is mainly used to filter records as received from the wire record
 */
@Beta
public final class DbWireRecordFilter implements WireEmitter, WireReceiver, ConfigurableComponent {

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(DbWireRecordFilter.class);

	// FIXME: Remove refresh rate parameter and add a new DataEventTimer service
	// FIXME: Add support for Cloudlet

	/** The component context. */
	private ComponentContext m_ctx;

	/** The DB Service dependency. */
	private volatile DbService m_dbService;

	/** The DB Filter Options. */
	private DbWireRecordFilterOptions m_options;

	/** The Wire Supporter component. */
	private WireSupport m_wireSupport;

	/**
	 * OSGi service component callback for deactivation
	 *
	 * @param componentContext
	 *            the component context
	 * @param properties
	 *            the properties
	 */
	protected synchronized void activate(final ComponentContext componentContext,
			final Map<String, Object> properties) {
		s_logger.info("Activating DB Wire Record Filter...");
		this.m_ctx = componentContext;
		this.m_wireSupport = WireSupport.of(this);
		this.m_options = new DbWireRecordFilterOptions(properties);
		s_logger.info("Activating DB Wire Record Filter...Done");
	}

	/** {@inheritDoc} */
	@Override
	public void consumersConnected(final Wire[] wires) {
		this.m_wireSupport.consumersConnected(wires);
	}

	/**
	 * OSGi service component callback for deactivation
	 *
	 * @param componentContext
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext componentContext) {
		s_logger.info("deactivate...");
		// no need to release the cloud clients as the updated app
		// certificate is already published due the missing dependency
		// we only need to empty our CloudClient list
		this.m_dbService = null;
	}

	/**
	 * Gets the db service.
	 *
	 * @return the db service
	 */
	public DbService getDbService() {
		return this.m_dbService;
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return (String) this.m_ctx.getProperties().get("service.pid");
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void onWireReceive(final WireEnvelope wireEvelope) {
		checkCondition(wireEvelope == null, "Wire envelope cannot be null");
		s_logger.debug("Wire Enveloped received..." + this.m_wireSupport);
		// FIXME: add implementation
	}

	/** {@inheritDoc} */
	@Override
	public Object polled(final Wire wire) {
		return this.m_wireSupport.polled(wire);
	}

	/** {@inheritDoc} */
	@Override
	public void producersConnected(final Wire[] wires) {
		this.m_wireSupport.producersConnected(wires);
	}

	/**
	 * Sets the DB service.
	 *
	 * @param dbService
	 *            the new DB service
	 */
	public synchronized void setDbService(final DbService dbService) {
		this.m_dbService = dbService;
	}

	/**
	 * Unset DB service.
	 *
	 * @param dataService
	 *            the DB service
	 */
	public synchronized void unsetDbService(final DbService dataService) {
		this.m_dbService = null;
	}

	/**
	 * OSGi service component callback for updating
	 *
	 * @param properties
	 *            the updated properties
	 */
	public synchronized void updated(final Map<String, Object> properties) {
		s_logger.info("Updating DBWireRecordFilter..." + properties);
		this.m_options = new DbWireRecordFilterOptions(properties);
		s_logger.info("Updating DBWireRecordFilter...Done " + properties);
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		this.m_wireSupport.updated(wire, value);
	}
}
