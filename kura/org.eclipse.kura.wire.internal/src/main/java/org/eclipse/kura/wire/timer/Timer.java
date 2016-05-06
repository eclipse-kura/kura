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
package org.eclipse.kura.wire.timer;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

/**
 * The Class Timer represents a Wire Component which triggers an event on every
 * interval as configured.
 */
public final class Timer implements WireEmitter, ConfigurableComponent {

	/** The Constant PROP_INTERVAL denoting the property from the metatype */
	private static final String PROP_INTERVAL = "interval";

	/** The Constant PROP_TIMER_NAME denoting the property from the metatype */
	private static final String PROP_TIMER_NAME = "name";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(Timer.class);

	/** The Constant denotes the field name of the timer */
	public static final String TIMER_EVENT_FIELD_NAME = "KuraDeviceTimerEvent";

	/** The interval time (in seconds). */
	private int m_interval;

	/** The Name of this wire component. */
	private String m_name;

	/** The properties as provided by configuration admin. */
	private Map<String, Object> m_properties;

	/** The thread pool executor. */
	private final ExecutorService m_tickExecutor;

	/** The future handle of the thread pool executor service. */
	private Future<?> m_tickHandle;

	/** The wire supporter component. */
	private final WireSupport m_wireSupport;

	/**
	 * Instantiates a new timer.
	 */
	public Timer() {
		this.m_tickExecutor = Executors.newSingleThreadExecutor();
		this.m_wireSupport = WireSupport.of(this);
	}

	/**
	 * OSGi service component activation callback
	 *
	 * @param ctx
	 *            the ctx component context
	 * @param properties
	 *            the properties
	 */
	protected void activate(final ComponentContext ctx, final Map<String, Object> properties) {
		s_logger.info("Activating Wire timer...");

		this.m_properties = properties;

		this.doUpdate();
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
	 *            the ctx component context
	 */
	protected void deactivate(final ComponentContext ctx) {
		s_logger.info("Deactivating Wire timer...");

		if (this.m_tickHandle != null) {
			this.m_tickHandle.cancel(true);
		}

		this.m_tickExecutor.shutdown();
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

		this.m_tickHandle = this.m_tickExecutor.submit(new Runnable() {

			@Override
			public void run() {

				while (true) {
					try {
						TimeUnit.SECONDS.sleep(Timer.this.m_interval);
					} catch (final InterruptedException e) {
						s_logger.error(Throwables.getStackTraceAsString(e));
					}
					Timer.this.m_wireSupport.emit(
							new WireRecord(new WireField(TIMER_EVENT_FIELD_NAME, new StringValue(Timer.this.m_name))));
				}
			}
		});
	}

	/** {@inheritDoc} */
	@Override
	public String getName() {
		return this.getClass().toString();
	}

	/** {@inheritDoc} */
	@Override
	public Object polled(final Wire wire) {
		return this.m_wireSupport.polled(wire);
	}

	/**
	 * OSGi service component modification callback
	 *
	 * @param ctx
	 *            the ctx component context
	 */
	protected void updated(final Map<String, Object> properties) {
		s_logger.info("Updating Wire timer...");
		this.m_properties = properties;
		this.doUpdate();
		s_logger.info("Updating Wire timer...Done");
	}
}
