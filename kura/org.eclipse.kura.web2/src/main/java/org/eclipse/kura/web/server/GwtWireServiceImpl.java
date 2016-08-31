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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.driver.Driver;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The class GwtWireServiceImpl implements {@link GwtWireService}
 */
public final class GwtWireServiceImpl extends OsgiRemoteServiceServlet implements GwtWireService {

	/**
	 * Different property related constants
	 */
	private static final String CELL_TYPE = "cellType";
	private static final String CELLS = "cells";
	private static final String CONSUMER = "consumer";
	private static final String DELETE_CELLS = "deleteCells";
	private static final String DEVS_MODEL_ELEMENT = "devs.Atomic";
	private static final String FACTORY_PID = "factoryPid";
	private static final String GRAPH = "graph";
	private static final String ID = "id";
	private static final String JOINT_JS = "jointJs";
	private static final String LABEL = "label";
	private static final String NEW_WIRE = "newWire";
	private static final String PATTERN_CONFIGURATION_REQUIRE = "configuration-policy=\"require\"";
	private static final String PATTERN_SERVICE_PROVIDE_CONFIGURABLE_COMP = "provide interface=\"org.eclipse.kura.configuration.ConfigurableComponent\"";
	private static final String PATTERN_SERVICE_PROVIDE_EMITTER = "provide interface=\"org.eclipse.kura.wire.WireEmitter\"";
	private static final String PATTERN_SERVICE_PROVIDE_RECEIVER = "provide interface=\"org.eclipse.kura.wire.WireReceiver\"";
	private static final String PATTERN_SERVICE_PROVIDE_SELF_CONFIGURING_COMP = "provide interface=\"org.eclipse.kura.configuration.SelfConfiguringComponent\"";
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
	public List<String> getDriverInstances(final GwtXSRFToken xsrfToken) throws GwtKuraException {
		this.checkXSRFToken(xsrfToken);
		final ServiceReference[] refs = ServiceLocator.getInstance().getServiceReferences(Driver.class, null);
		final List<String> drivers = new ArrayList<String>();
		for (final ServiceReference ref : refs) {
			drivers.add(String.valueOf(ref.getProperty("kura.service.pid")));
		}
		return drivers;
	}

	/** {@inheritDoc} */
	@Override
	public GwtWiresConfiguration getWiresConfiguration(final GwtXSRFToken xsrfToken) throws GwtKuraException {
		this.checkXSRFToken(xsrfToken);
		return this.getWiresConfigurationInternal();
	}

	private GwtWiresConfiguration getWiresConfigurationInternal() throws GwtKuraException {
		final ConfigurationService configService = ServiceLocator.getInstance().getService(ConfigurationService.class);
		final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
		ServiceLocator.getInstance().getService(WireHelperService.class);
		final Set<WireConfiguration> wireConfigurations = wireService.getWireConfigurations();
		final List<String> wireEmitterFactoryPids = new ArrayList<String>();
		final List<String> wireReceiverFactoryPids = new ArrayList<String>();
		final List<String> wireComponents = new ArrayList<String>();

		fillFactoriesLists(wireEmitterFactoryPids, wireReceiverFactoryPids);

		String sGraph = null;
		// Get Graph JSON from WireService
		try {
			final Map<String, Object> wsProps = configService.getComponentConfiguration(WIRE_SERVICE_PID)
					.getConfigurationProperties();
			sGraph = (String) wsProps.get(GRAPH);
		} catch (final KuraException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		}

		// create the JSON for the Wires Configuration
		final JSONObject wireConfig = new JSONObject();
		int i = 0;
		for (final WireConfiguration wireConfiguration : wireConfigurations) {
			final String emitterPid = wireConfiguration.getEmitterPid();
			final String receiverPid = wireConfiguration.getReceiverPid();
			wireComponents.add(getComponentString(emitterPid));
			wireComponents.add(getComponentString(receiverPid));

			final JSONObject wireConf = new JSONObject();
			try {
				wireConf.put("p", emitterPid);
				wireConf.put("c", receiverPid);
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
		configuration.setGraph(sGraph == null ? "{}" : sGraph);
		return configuration;
	}

	/** {@inheritDoc} */
	@Override
	public GwtWiresConfiguration updateWireConfiguration(final GwtXSRFToken xsrfToken,
			final String newJsonConfiguration) throws GwtKuraException {
		this.checkXSRFToken(xsrfToken);

		final Map<String, String> idToPid = new HashMap<String, String>();

		JSONObject jObj = null; // JSON object containing wires configuration
		JSONObject jGraph = null; // JSON object containing graph configuration
		JSONArray jCells = null; // JSON array of cells within JointJS graph
		JSONArray jDelCells = null; // JSON array of cells to be deleted

		final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
		final WireHelperService wireHelperService = ServiceLocator.getInstance().getService(WireHelperService.class);
		final ConfigurationService configService = ServiceLocator.getInstance().getService(ConfigurationService.class);

		try {
			jObj = new JSONObject(newJsonConfiguration);
			jDelCells = jObj.getJSONArray(DELETE_CELLS);
			jGraph = jObj.getJSONObject(JOINT_JS);
			jCells = jGraph.getJSONArray(CELLS);

			// Create new Wire Component instances
			for (int i = 0; i < jCells.length(); i++) {
				final JSONObject jsonObject = jCells.getJSONObject(i);
				if (DEVS_MODEL_ELEMENT.equalsIgnoreCase(jsonObject.getString(TYPE))) {
					String elementPid = jsonObject.getString(PID);
					if ("none".equalsIgnoreCase(elementPid)) {
						final String elementFactoryPid = jsonObject.getString(FACTORY_PID);
						final String elementLabel = jsonObject.getString(LABEL);
						s_logger.info("Creating new component: Factory PID -> " + elementFactoryPid + " | PID -> "
								+ elementLabel);
						elementPid = elementLabel;
						Map<String, Object> properties = null;
						String driver = null;
						try {
							driver = jsonObject.getString("driver");
						} catch (final JSONException ex) {
							// do nothing
						}
						if (driver != null) {
							properties = new HashMap<String, Object>();
							properties.put("asset.desc", "Sample Asset");
							properties.put("driver.pid", driver);
						}
						configService.createFactoryConfiguration(elementFactoryPid, elementPid, properties, false);
						jsonObject.put(PID, elementPid);
					}
					final String elementId = jsonObject.getString(ID);
					idToPid.put(elementId, elementPid);
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
					s_logger.info("Service Pid for Producer: {}", wireHelperService.getServicePid(prod));
					s_logger.info("Service Pid for Consumer: {}", wireHelperService.getServicePid(cons));

					// track and wait for the producer
					final String pPid = wireHelperService.getServicePid(prod);
					final BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
					String filterString = "(" + Constants.SERVICE_PID + "=" + pPid + ")";
					Filter filter = bundleContext.createFilter(filterString);
					final ServiceTracker producerTracker = new ServiceTracker(bundleContext, filter, null);
					producerTracker.open();
					producerTracker.waitForService(5000);
					producerTracker.close();

					// track and wait for the consumer
					final String cPid = wireHelperService.getServicePid(cons);
					filterString = "(" + Constants.SERVICE_PID + "=" + cPid + ")";
					filter = bundleContext.createFilter(filterString);
					final ServiceTracker consumerTracker = new ServiceTracker(bundleContext, filter, null);
					consumerTracker.open();
					consumerTracker.waitForService(5000);
					consumerTracker.close();

					wireService.createWireConfiguration(prod, cons);
					jCells.getJSONObject(i).put(NEW_WIRE, false);
				}
			}

			// Delete wires
			for (int i = 0; i < jDelCells.length(); i++) {
				final JSONObject jsonObject = jDelCells.getJSONObject(i);
				final String deleteCells = jsonObject.getString(CELL_TYPE);
				String producerPid = null;
				String consumerPid = null;
				if ("wire".equalsIgnoreCase(deleteCells)) {
					// delete wires must rely on the previous config saved in
					// the Wire Service properties
					try {
						final Map<String, Object> wsProps = configService.getComponentConfiguration(WIRE_SERVICE_PID)
								.getConfigurationProperties();
						final String graph = (String) wsProps.get(GRAPH);

						if (graph != null) {
							final JSONObject oldGraph = new JSONObject(graph);
							final JSONArray oldArray = oldGraph.getJSONArray("cells");
							for (int k = 0; k < oldArray.length(); k++) {
								final JSONObject oldJson = oldArray.getJSONObject(k);
								if (jsonObject.getString("p").equalsIgnoreCase(oldJson.getString(ID))) {
									producerPid = oldJson.getString(PID);
								}
								if (jsonObject.getString("c").equalsIgnoreCase(oldJson.getString(ID))) {
									consumerPid = oldJson.getString(PID);
								}
							}
						}
					} catch (final KuraException exception) {
						throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
					} catch (final JSONException exception) {
						throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
					}
					s_logger.info(
							"Deleting Wire: Producer PID -> " + producerPid + " | Consumer PID -> " + consumerPid);
					final WireConfiguration wireConfiguration = new WireConfiguration(producerPid, consumerPid, null);
					wireService.deleteWireConfiguration(wireConfiguration);
				}
			}

			// Delete Wire Component instances
			for (int i = 0; i < jDelCells.length(); i++) {
				final JSONObject jsonObject = jDelCells.getJSONObject(i);
				if ("instance".equalsIgnoreCase(jsonObject.getString(CELL_TYPE))) {
					final String componentPid = jsonObject.getString(PID);
					s_logger.info("Deleting Wire Component: PID -> " + componentPid);
					configService.deleteFactoryConfiguration(componentPid, false);
				}
			}
			final Map<String, Object> props = configService.getComponentConfiguration(WIRE_SERVICE_PID)
					.getConfigurationProperties();
			props.put(GRAPH, jGraph.toString());
			configService.updateConfiguration(WIRE_SERVICE_PID, props, true);
		} catch (final JSONException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		} catch (final KuraException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		} catch (final InterruptedException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		} catch (final InvalidSyntaxException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		}
		return this.getWiresConfigurationInternal();
	}

	/**
	 * Fills the provided lists with the proper factory IDs of the available
	 * configurable or self configuring components
	 *
	 * @param emitters
	 *            the emitters factory PID list
	 * @param receivers
	 *            the receivers factory PID list
	 * @throws GwtKuraException
	 *             if any exception is encountered
	 */
	private static void fillFactoriesLists(final List<String> emitters, final List<String> receivers)
			throws GwtKuraException {
		final Bundle[] bundles = FrameworkUtil.getBundle(GwtWireService.class).getBundleContext().getBundles();
		for (final Bundle bundle : bundles) {
			final Enumeration<URL> enumeration = bundle.findEntries("OSGI-INF", "*.xml", false);
			if (enumeration != null) {
				while (enumeration.hasMoreElements()) {
					final URL entry = enumeration.nextElement();
					BufferedReader reader = null;
					try {
						reader = new BufferedReader(new InputStreamReader(entry.openConnection().getInputStream()));
						final StringBuilder contents = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) {
							contents.append(line);
						}
						// Configruation Policy=Require and
						// SelfConfiguringComponent or ConfigurableComponent
						if ((contents.toString().contains(PATTERN_SERVICE_PROVIDE_SELF_CONFIGURING_COMP)
								|| contents.toString().contains(PATTERN_SERVICE_PROVIDE_CONFIGURABLE_COMP))
								&& contents.toString().contains(PATTERN_CONFIGURATION_REQUIRE)) {
							final Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
									.parse(entry.openConnection().getInputStream());
							final NodeList nl = dom.getElementsByTagName("property");
							for (int i = 0; i < nl.getLength(); i++) {
								final Node n = nl.item(i);
								if (n instanceof Element) {
									final String name = ((Element) n).getAttribute("name");
									if ("service.pid".equals(name)) {
										final String factoryPid = ((Element) n).getAttribute("value");
										if (contents.toString().contains(PATTERN_SERVICE_PROVIDE_EMITTER)) {
											emitters.add(factoryPid);
										}
										if (contents.toString().contains(PATTERN_SERVICE_PROVIDE_RECEIVER)) {
											receivers.add(factoryPid);
										}
									}
								}
							}
						}
					} catch (final Exception ex) {
						s_logger.error("Error while reading Component Definition file {}", entry.getPath());
					} finally {
						try {
							if (reader != null) {
								reader.close();
							}
						} catch (final IOException e) {
							s_logger.error("Error closing File Reader!" + e);
						}
					}
				}
			}
		}
	}

	/**
	 * Returns the formatted component string required for JS
	 *
	 * @param pid
	 *            the PID to parse
	 * @return the formatted string
	 * @throws GwtKuraException
	 */
	private static String getComponentString(final String pid) throws GwtKuraException {
		final StringBuilder result = new StringBuilder();

		final BundleContext ctx = FrameworkUtil.getBundle(GwtWireServiceImpl.class).getBundleContext();
		final ServiceReference[] refs = ServiceLocator.getInstance().getServiceReferences(WireComponent.class, null);
		for (final ServiceReference ref : refs) {
			if (ref.getProperty(ConfigurationService.KURA_SERVICE_PID).equals(pid)) {
				final String fPid = (String) ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);
				final WireComponent comp = (WireComponent) ctx.getService(ref);
				String compType;
				if ((comp instanceof WireEmitter) && (comp instanceof WireReceiver)) {
					compType = "both";
				} else if (comp instanceof WireEmitter) {
					compType = "producer";
				} else {
					compType = "consumer";
				}
				result.append(fPid).append("|").append(pid).append("|").append(pid).append("|").append(compType);
				return result.toString();
			}
		}
		s_logger.error("Could not find WireComponent for pid {}", pid);
		throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
	}

}
