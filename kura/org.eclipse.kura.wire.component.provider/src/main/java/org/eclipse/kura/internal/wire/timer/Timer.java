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
package org.eclipse.kura.internal.wire.timer;

import static org.eclipse.kura.asset.AssetConstants.TIMER_EVENT;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class Timer represents a Wire Component which triggers a ticking event on
 * every interval as configured. It fires the event on every tick.
 */
public final class Timer implements WireEmitter, ConfigurableComponent {

	/** The Constant denoting the interval property from the metatype */
	private static final String PROP_INTERVAL = "interval";

	/** The Constant denoting the name property from the metatype */
	private static final String PROP_TIMER_NAME = "name";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(Timer.class);

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** Schedule Executor Service **/
	private final ScheduledExecutorService m_executorService;

	/** The interval time (in seconds). */
	private int m_interval;

	/** The Name of this wire component. */
	private String m_name;

	/** The properties as provided by configuration admin. */
	private Map<String, Object> m_properties;

	/** The future handle of the thread pool executor service. */
	private ScheduledFuture<?> m_tickHandle;

	/** The Wire Helper Service. */
	private volatile WireHelperService m_wireHelperService;

	/** The wire supporter component. */
	private final WireSupport m_wireSupport;

	/**
	 * Instantiates a new timer.
	 */
	public Timer() {
		this.m_executorService = Executors.newSingleThreadScheduledExecutor();
		this.m_wireSupport = this.m_wireHelperService.newWireSupport(this);
	}

	/**
	 * OSGi service component activation callback
	 *
	 * @param ctx
	 *            the component context
	 * @param properties
	 *            the configured properties
	 */
	protected synchronized void activate(final ComponentContext ctx, final Map<String, Object> properties) {
		s_logger.debug(s_message.activatingTimer());
		this.m_properties = properties;
		this.doUpdate();
		s_logger.debug(s_message.activatingTimerDone());
	}

	/**
	 * Binds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void bindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == null) {
			this.m_wireHelperService = wireHelperService;
		}
	}

	/** {@inheritDoc} */
	@Override
	public void consumersConnected(final Wire[] wires) {
		this.m_wireSupport.consumersConnected(wires);
	}

	/**
	 * OSGi service component deactivation callback
	 *
	 * @param ctx
	 *            the component context
	 */
	protected synchronized void deactivate(final ComponentContext ctx) {
		s_logger.debug(s_message.deactivatingTimer());
		if (this.m_tickHandle != null) {
			this.m_tickHandle.cancel(true);
		}
		if (this.m_executorService != null) {
			this.m_executorService.shutdown();
		}
		s_logger.debug(s_message.deactivatingTimerDone());
	}

	/**
	 * Perform update operation which internally emits a Wire Record every
	 * interval
	 */
	private void doUpdate() {
		this.m_name = this.m_properties.get(PROP_TIMER_NAME).toString();
		this.m_interval = (Integer) this.m_properties.get(PROP_INTERVAL);
		if (this.m_tickHandle != null) {
			this.m_tickHandle.cancel(true);
		}
		this.m_tickHandle = this.m_executorService.scheduleAtFixedRate(new Runnable() {
			/** {@inheritDoc} */
			@Override
			public void run() {
				m_wireSupport
						.emit(Arrays.asList(m_wireHelperService.newWireRecord(m_wireHelperService
								.newWireField(TIMER_EVENT.value(), TypedValues.newStringValue(m_name)))));
			}
		}, 0, this.m_interval, TimeUnit.SECONDS);
	}

	/** {@inheritDoc} */
	@Override
	public Object polled(final Wire wire) {
		return this.m_wireSupport.polled(wire);
	}

	/**
	 * Unbinds the Wire Helper Service.
	 *
	 * @param wireHelperService
	 *            the new Wire Helper Service
	 */
	public synchronized void unbindWireHelperService(final WireHelperService wireHelperService) {
		if (this.m_wireHelperService == wireHelperService) {
			this.m_wireHelperService = null;
		}
	}

	/**
	 * OSGi service component modification callback
	 *
	 * @param properties
	 *            the updated properties
	 */
	protected synchronized void updated(final Map<String, Object> properties) {
		s_logger.debug(s_message.updatingTimer());
		this.m_properties = properties;
		this.doUpdate();
		s_logger.debug(s_message.updatingTimerDone());
	}
}
