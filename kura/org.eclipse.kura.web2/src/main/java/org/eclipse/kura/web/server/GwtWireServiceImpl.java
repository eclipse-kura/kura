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
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtWiresConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class GwtWireServiceImpl implements {@link GwtWireService}
 */
public final class GwtWireServiceImpl extends OsgiRemoteServiceServlet implements GwtWireService {

	private static final String SERVICE_PID = "service.pid";
	/**
	 * Different property related constants
	 */
	private static final String CELL_TYPE = "cellType";
	private static final String CELLS = "cells";
	private static final String CONSUMER = "consumer";
	private static final String DELETE_CELLS = "deleteCells";
	private static final String FACTORY_PID = "factoryPid";
	private static final String GRAPH = "graph";
	private static final String HTML_ELEMENT = "html.Element";
	private static final String JOINT_JS = "jointJs";
	private static final String NEW_WIRE = "newWire";
	private static final String PID = "pid";
	private static final String PRODUCER = "producer";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(GwtWireServiceImpl.class);

	/** Serial Version */
	private static final long serialVersionUID = -6577843865830245755L;

	private static final String TYPE = "type";

	/** Wire Service PID Property */
	private static final String WIRE_SERVICE_PID = "org.eclipse.kura.wire.WireService";

	/** {@inheritDoc} */
	@Override
	public GwtWiresConfiguration getWiresConfiguration(final GwtXSRFToken xsrfToken) throws GwtKuraException {
		this.checkXSRFToken(xsrfToken);

		final ConfigurationService configService = ServiceLocator.getInstance().getService(ConfigurationService.class);
		final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
		ServiceLocator.getInstance().getService(WireHelperService.class);
		final List<WireConfiguration> wireConfigurations = wireService.getWireConfigurations();
		final List<String> wireEmitterFactoryPids = new ArrayList<String>();
		final List<String> wireReceiverFactoryPids = new ArrayList<String>();
		final List<String> wireComponents = new ArrayList<String>();

		String sGraph = null;
		// Get Graph JSON from WireService
		try {
			final Map<String, Object> wsProps = configService.getComponentConfiguration(WIRE_SERVICE_PID)
					.getConfigurationProperties();
			sGraph = (String) wsProps.get(GRAPH);
		} catch (final KuraException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		}
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<WireComponent>[] wireComps = getServiceReferences(context, WireComponent.class, null);
		for (final ServiceReference<WireComponent> wc : wireComps) {
			if (wc instanceof WireEmitter) {
				wireEmitterFactoryPids.add(String.valueOf(wc.getProperty(SERVICE_PID)));
			}
			if (wc instanceof WireReceiver) {
				wireReceiverFactoryPids.add(String.valueOf(wc.getProperty(SERVICE_PID)));
			}
		}

		// create the JSON for the Wires Configuration
		final JSONObject wireConfig = new JSONObject();
		int i = 0;
		for (final WireConfiguration wireConfiguration : wireConfigurations) {
			final String emitterPid = wireConfiguration.getEmitterPid();
			final String receiverPid = wireConfiguration.getReceiverPid();
			wireComponents.add(emitterPid);
			wireComponents.add(receiverPid);

			final JSONObject wireConf = new JSONObject();
			try {
				wireConf.put("p", emitterPid);
				wireConf.put("c", receiverPid);
				wireConf.put("f", wireConfiguration.getFilter());
				wireConfig.put(String.valueOf(++i), wireConf);
			} catch (final JSONException exception) {
				throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
			}
		}
		final GwtWiresConfiguration configuration = new GwtWiresConfiguration();
		configuration.getWireEmitterFactoryPids().addAll(wireEmitterFactoryPids);
		configuration.getWireReceiverFactoryPids().addAll(wireReceiverFactoryPids);
		configuration.getWireComponents().addAll(wireComponents);
		configuration.setWiresConfigurationJson(wireConfig.toString());
		configuration.setGraph(sGraph);
		return configuration;
	}

	/** {@inheritDoc} */
	@Override
	public GwtWiresConfiguration updateWireConfiguration(final GwtXSRFToken xsrfToken,
			final String newJsonConfiguration) throws GwtKuraException {
		this.checkXSRFToken(xsrfToken);

		JSONObject jObj = null; // JSON object containing wires configuration
		JSONObject jGraph = null; // JSON object containing graph configuration
		JSONArray jCells = null; // JSON array of cells within JointJS graph
		JSONArray jDelCells = null; // JSON array of cells to be deleted
		final Map<String, String> idToPid = new HashMap<String, String>();
		final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
		final ConfigurationService configService = ServiceLocator.getInstance().getService(ConfigurationService.class);

		try {
			jObj = new JSONObject(newJsonConfiguration);
			jDelCells = jObj.getJSONArray(DELETE_CELLS);
			jGraph = jObj.getJSONObject(JOINT_JS);
			jCells = jGraph.getJSONArray(CELLS);

			// Create new Wire Component instances
			for (int i = 0; i < jCells.length(); i++) {
				if (HTML_ELEMENT.equalsIgnoreCase(jCells.getJSONObject(i).getString(TYPE))) {
					if ("none".equalsIgnoreCase(jCells.getJSONObject(i).getString(PID))) {
						s_logger.info("Creating new component: Factory PID -> "
								+ jCells.getJSONObject(i).getString(FACTORY_PID) + " | Instance Name -> "
								+ jCells.getJSONObject(i).getString("label"));
						final String pid = jCells.getJSONObject(i).getString("label");
						configService.createFactoryConfiguration(jCells.getJSONObject(i).getString(FACTORY_PID), pid,
								null, false);
						jCells.getJSONObject(i).put(PID, pid);
					}
					idToPid.put(jCells.getJSONObject(i).getString("id"), jCells.getJSONObject(i).getString(PID));
				}
			}
			jGraph.put(CELLS, jCells);

			// Create new wires
			for (int i = 0; i < jCells.length(); i++) {
				if ("customLink.Element".equalsIgnoreCase(jCells.getJSONObject(i).getString(TYPE))
						&& jCells.getJSONObject(i).getBoolean(NEW_WIRE)) {
					final String prod = idToPid.get(jCells.getJSONObject(i).getString(PRODUCER));
					final String cons = idToPid.get(jCells.getJSONObject(i).getString(CONSUMER));
					s_logger.info("Creating new wire: Producer PID -> " + prod + " | Consumer PID -> " + cons);
					wireService.createWireConfiguration(prod, cons);
					jCells.getJSONObject(i).put(NEW_WIRE, false);
				}
			}

			// Delete wires
			for (int i = 0; i < jDelCells.length(); i++) {
				if ("wire".equalsIgnoreCase(jDelCells.getJSONObject(i).getString(CELL_TYPE))) {
					final String prod = idToPid.get(jDelCells.getJSONObject(i).getString("p"));
					final String cons = idToPid.get(jDelCells.getJSONObject(i).getString("c"));
					s_logger.info("Deleting wire: Producer PID -> " + prod + " | Consumer PID -> " + cons);
					final WireConfiguration wireConfiguration = new WireConfiguration(prod, cons, null);
					wireService.deleteWireConfiguration(wireConfiguration);
				}
			}

			// Delete Wire Component instances
			for (int i = 0; i < jDelCells.length(); i++) {
				if ("instance".equalsIgnoreCase(jDelCells.getJSONObject(i).getString(CELL_TYPE))) {
					s_logger.info("Deleting instance: PID -> " + jDelCells.getJSONObject(i).getString(PID));
					configService.deleteFactoryConfiguration(jDelCells.getJSONObject(i).getString(PID), false);
				}
			}

			final Map<String, Object> props = configService.getComponentConfiguration(WIRE_SERVICE_PID)
					.getConfigurationProperties();
			props.put(GRAPH, jGraph.toString());
			configService.updateConfiguration(WIRE_SERVICE_PID, props, false);
		} catch (final JSONException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		} catch (final KuraException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		}
		return this.getWiresConfiguration(xsrfToken);
	}

	public static <T> ServiceReference<T>[] getServiceReferences(final BundleContext bundleContext,
			final Class<T> clazz, final String filter) {
		try {
			final ServiceReference<?>[] refs = bundleContext.getServiceReferences(clazz.getName(), filter);
			@SuppressWarnings("unchecked")
			final ServiceReference<T>[] reference = (refs == null ? new ServiceReference[0] : refs);
			return reference;
		} catch (final InvalidSyntaxException ise) {
			throw new KuraRuntimeException(KuraErrorCode.INTERNAL_ERROR, ise);
		}
	}

}
