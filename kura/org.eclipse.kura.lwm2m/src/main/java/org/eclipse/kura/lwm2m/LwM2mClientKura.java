/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
/*
 * Copyright (c) 2011 Eurotech Inc. All rights reserved.
 */

package org.eclipse.kura.lwm2m;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import leshan.client.californium.LeshanClient;
import leshan.client.coap.californium.CaliforniumBasedObject;
import leshan.client.exchange.LwM2mExchange;
import leshan.client.request.AbstractRegisteredLwM2mClientRequest;
import leshan.client.request.DeregisterRequest;
import leshan.client.request.RegisterRequest;
import leshan.client.request.UpdateRequest;
import leshan.client.request.identifier.ClientIdentifier;
import leshan.client.resource.LwM2mClientObject;
import leshan.client.resource.LwM2mClientObjectDefinition;
import leshan.client.resource.LwM2mClientObjectInstance;
import leshan.client.resource.LwM2mClientResourceDefinition;
import leshan.client.resource.SingleResourceDefinition;
import leshan.client.resource.bool.BooleanLwM2mExchange;
import leshan.client.resource.bool.BooleanLwM2mResource;
import leshan.client.resource.integer.IntegerLwM2mExchange;
import leshan.client.resource.integer.IntegerLwM2mResource;
import leshan.client.resource.multiple.MultipleLwM2mExchange;
import leshan.client.resource.multiple.MultipleLwM2mResource;
import leshan.client.resource.string.StringLwM2mExchange;
import leshan.client.resource.string.StringLwM2mResource;
import leshan.client.resource.time.TimeLwM2mExchange;
import leshan.client.resource.time.TimeLwM2mResource;
import leshan.client.response.ExecuteResponse;
import leshan.client.response.OperationResponse;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.lwm2m.component.factory.LWM2mConfigurableComponentsFactory;
import org.eclipse.kura.lwm2m.objects.LwM2mPositionObjectDefinition;
import org.eclipse.kura.lwm2m.objects.LwM2mServerObjectDefinition;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mClientKura implements ConfigurableComponent, EventHandler {
	private static final Logger s_logger = LoggerFactory.getLogger(LwM2mClientKura.class);

	private static final int TIMEOUT_MS = 10000;

	private ComponentContext m_ctx;
	private LwM2mClientOptions m_options;

	private leshan.client.LwM2mClient m_lwM2mClient;
	// private RegisterUplink m_registerUplink;
	private ClientIdentifier clientIdentifier;
	// private String m_deviceLocation;

	private SystemService m_systemService;
	private SystemAdminService m_systemAdminService;
	private NetworkService m_networkService;
	private EventAdmin m_eventAdmin;
	private PositionService m_positionService;
	private ConfigurationService m_configurationService;

	private ScheduledExecutorService updater;
	private ScheduledFuture<?> updater_future;

	public LwM2mClientKura() {
		updater = Executors.newSingleThreadScheduledExecutor();
	}

	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public void setConfigurationService(ConfigurationService configurationService) {
		this.m_configurationService = configurationService;
	}

	public void unsetConfigurationService(ConfigurationService configurationService) {
		this.m_configurationService = null;
	}

	public void setPositionService(PositionService positionService) {
		this.m_positionService = positionService;
	}

	public void unsetPositionService(PositionService positionService) {
		this.m_positionService = null;
	}

	public void setSystemAdminService(SystemAdminService systemAdminService) {
		this.m_systemAdminService = systemAdminService;
	}

	public void unsetSystemAdminService(SystemAdminService systemAdminService) {
		this.m_systemAdminService = null;
	}

	public SystemAdminService getSystemAdminService() {
		return m_systemAdminService;
	}

	public void setSystemService(SystemService systemService) {
		this.m_systemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		this.m_systemService = null;
	}

	public SystemService getSystemService() {
		return m_systemService;
	}

	public void setNetworkService(NetworkService networkService) {
		this.m_networkService = networkService;
	}

	public void unsetNetworkService(NetworkService networkService) {
		this.m_networkService = null;
	}

	public NetworkService getNetworkService() {
		return m_networkService;
	}

	public void setEventAdmin(EventAdmin eventAdmin) {
		this.m_eventAdmin = eventAdmin;
	}

	public void unsetEventAdmin(EventAdmin eventAdmin) {
		this.m_eventAdmin = null;
	}

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		s_logger.info("activate...");

		//
		// save the bundle context and the properties
		m_ctx = componentContext;
		m_options = new LwM2mClientOptions(properties, m_systemService, m_networkService);

		//
		// connect and register
		try {
			register();
		} catch (Exception e) {
			s_logger.error("Error during registration", e);
		}

		// prova();
	}

	private void prova() {
		try {
			for (ComponentConfiguration c : m_configurationService.getComponentConfigurations()) {
				for (AD ad : c.getDefinition().getAD()) {
					s_logger.info("********************************");
					s_logger.info("Component: {}", c.getDefinition().getName());
					s_logger.info("           id:{}", ad.getId());
					s_logger.info("         name:{}", ad.getName());
					s_logger.info("  cardinality:{}", ad.getCardinality());
					s_logger.info("         type:{}", ad.getType());
					s_logger.info("      default:{}", ad.getDefault());
					s_logger.info("  description:{}", ad.getDescription());
				}
			}
		} catch (KuraException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updated(Map<String, Object> properties) {
		s_logger.info("updated...: " + properties);

		// Update properties and re-publish Birth certificate
		m_options = new LwM2mClientOptions(properties, m_systemService, m_networkService);

		deregister();

		try {
			register();
		} catch (Exception e) {
			s_logger.error("Error during registration", e);
		}

	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("deactivate...");

		deregister();

		m_systemService = null;
		m_systemAdminService = null;
		m_networkService = null;
		m_eventAdmin = null;
	}

	public void handleEvent(Event event) {
	}

	private void deregister() {
		if (clientIdentifier != null) {
			s_logger.info("\tDevice: Deregistering Client '" + clientIdentifier + "'");
			final AbstractRegisteredLwM2mClientRequest deregisterRequest = new DeregisterRequest(clientIdentifier);
			final OperationResponse deregisterResponse = m_lwM2mClient.send(deregisterRequest);

			if (deregisterResponse.isSuccess()) {
				s_logger.info("\tDevice: Client Location Deregistered'" + deregisterResponse.getClientIdentifier() + "'");
				clientIdentifier = null;
			} else {
				s_logger.error("\tDevice Deregistration Error: " + deregisterResponse.getErrorMessage());
			}
			m_lwM2mClient.stop();
		}
	}

	private void register() throws KuraException, UnknownHostException, ComponentException {

		CoapServer coapServer = new CoapServer();
		LwM2mClientObjectDefinition deviceObject = createDeviceDefinition();

		s_logger.info("Before creating bundles...");
		// CaliforniumBasedObject calObjectBundles = createBundles();
		s_logger.info("After creating bundles...");
		// coapServer.add(calObjectBundles);
		LWM2mConfigurableComponentsFactory.getDefault().setConfigurationService(m_configurationService);
		CaliforniumBasedObject[] comps = LWM2mConfigurableComponentsFactory.getDefault().createComponentObjects();
		coapServer.add(comps);

		s_logger.info("After coapServer.add...");

		// Connect to the server provided
		s_logger.info("Connecting from: " + m_options.getClientIpAddress() + ":" + m_options.getClientIpPort() + " to " + m_options.getServerIpAddress() + ":"
				+ m_options.getServerIpPort());
		final InetSocketAddress clientAddress = new InetSocketAddress(m_options.getClientIpAddress(), m_options.getClientIpPort());
		final InetSocketAddress serverAddress = new InetSocketAddress(m_options.getServerIpAddress(), m_options.getServerIpPort());

		m_lwM2mClient = new LeshanClient(clientAddress, serverAddress, coapServer, new LwM2mServerObjectDefinition(), deviceObject,
				new LwM2mPositionObjectDefinition(m_positionService));

		m_lwM2mClient.start();

		// Register to the server provided
		// UUID
		// final String endpointIdentifier =
		// "urn:uuuid:"+UUID.randomUUID().toString();
		// <OUI>-<Product Class>-<Serial Number>
		final String endpointIdentifier = "urn:dev:ops:" + m_systemService.getPrimaryMacAddress() + "-" + "Kura_" + m_systemService.getDeviceName() + "-"
				+ m_systemService.getSerialNumber();

		HashMap<String, String> client_props = new HashMap<String, String>();
		client_props.put("lt", "120000");
		final RegisterRequest registerRequest = new RegisterRequest(endpointIdentifier, client_props, 5000);
		final OperationResponse operationResponse = m_lwM2mClient.send(registerRequest);

		// Report registration response.
		s_logger.info("Device Registration (Success? " + operationResponse.isSuccess() + ")");
		if (operationResponse.isSuccess()) {
			s_logger.info("\tDevice: Registered Client Location '" + operationResponse.getClientIdentifier() + "'");
			clientIdentifier = operationResponse.getClientIdentifier();
		} else {
			s_logger.error("\tDevice Registration Error: " + operationResponse.getErrorMessage());
			s_logger.error("If you're having issues connecting to the LWM2M endpoint, try using the DTLS port instead");
		}

		if (updater_future != null) {
			updater_future.cancel(true);
		}

		updater_future = updater.scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() {

				try {
					final AbstractRegisteredLwM2mClientRequest updateRequest = new UpdateRequest(clientIdentifier, 5000, new HashMap<String, String>());
					final OperationResponse updateResponse = m_lwM2mClient.send(updateRequest);

					// Report update response.
					s_logger.info("Device Update (Success? " + updateResponse.isSuccess() + ")");
					if (updateResponse.isSuccess()) {
						s_logger.info("\tDevice: Client Updated '" + updateResponse.getClientIdentifier() + "'");
						clientIdentifier = operationResponse.getClientIdentifier();
					} else {
						s_logger.error("\tDevice Update Error: " + updateResponse.getErrorMessage());
						deregister();

						register();
					}
				} catch (Exception ex) {
					System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				}

			}
		}, m_options.getUpdateDelay(), m_options.getUpdateDelay(), TimeUnit.MINUTES);

	}

	private LwM2mClientObjectDefinition createDeviceDefinition() {
		// Create an object model
		final StringValueResource manufacturerResource = new StringValueResource(m_systemService.getDeviceName(), 0);
		final StringValueResource modelResource = new StringValueResource(m_systemService.getModelName(), 1);
		final StringValueResource serialNumberResource = new StringValueResource(m_systemService.getSerialNumber(), 2);
		final StringValueResource firmwareResource = new StringValueResource(m_systemService.getFirmwareVersion(), 3);
		final ExecutableResource rebootResource = new ExecutableResource(4);
		final ExecutableResource factoryResetResource = new ExecutableResource(5);
		final MultipleLwM2mResource powerAvailablePowerResource = new IntegerMultipleResource(new Integer[] { 0, 2, 6 });
		final MultipleLwM2mResource powerSourceVoltageResource = new IntegerMultipleResource(new Integer[] { 12000, 5000, 240000 });
		final MultipleLwM2mResource powerSourceCurrentResource = new IntegerMultipleResource(new Integer[] { 216, 75, 0 });
		final IntegerValueResource batteryLevelResource = new IntegerValueResource(92, 9);
		final MemoryFreeResource memoryFreeResource = new MemoryFreeResource();
		final IntegerMultipleResource errorCodeResource = new IntegerMultipleResource(new Integer[] { 0 });
		final TimeResource currentTimeResource = new TimeResource();
		final StringValueResource utcOffsetResource = new StringValueResource(new SimpleDateFormat("X").format(Calendar.getInstance().getTime()), 14);
		final StringValueResource timezoneResource = new StringValueResource(TimeZone.getDefault().getID(), 15);
		final StringValueResource bindingsResource = new StringValueResource("U", 16);

		final LwM2mClientObjectDefinition objectDevice = new LwM2mClientObjectDefinition(3, true, true, new SingleResourceDefinition(0, manufacturerResource,
				true), new SingleResourceDefinition(1, modelResource, true), new SingleResourceDefinition(2, serialNumberResource, true),
				new SingleResourceDefinition(3, firmwareResource, true), new SingleResourceDefinition(4, rebootResource, true), new SingleResourceDefinition(5,
						factoryResetResource, true), new SingleResourceDefinition(6, powerAvailablePowerResource, true), new SingleResourceDefinition(7,
						powerSourceVoltageResource, true), new SingleResourceDefinition(8, powerSourceCurrentResource, true), new SingleResourceDefinition(9,
						batteryLevelResource, true), new SingleResourceDefinition(10, memoryFreeResource, true), new SingleResourceDefinition(11,
						errorCodeResource, true), new SingleResourceDefinition(12, new ExecutableResource(12), true), new SingleResourceDefinition(13,
						currentTimeResource, true), new SingleResourceDefinition(14, utcOffsetResource, true), new SingleResourceDefinition(15,
						timezoneResource, true), new SingleResourceDefinition(16, bindingsResource, true));

		return objectDevice;
	}

	private CaliforniumBasedObject createBundles() {
		LwM2mClientResourceDefinition[] bundleObjectsResourceDefs = new LwM2mClientResourceDefinition[3];

		IntegerValueResource bundleId = new IntegerValueResource(0, 0);
		bundleObjectsResourceDefs[0] = new SingleResourceDefinition(0, bundleId, true);

		StringValueResource bundleName = new StringValueResource("Bundle-SymbolicName", 1);
		bundleObjectsResourceDefs[1] = new SingleResourceDefinition(0, bundleName, true);

		StringValueResource bundleVersion = new StringValueResource("Bundle-Version", 2);
		bundleObjectsResourceDefs[2] = new SingleResourceDefinition(0, bundleVersion, true);

		final LwM2mClientObjectDefinition objectBundlesDef = new LwM2mClientObjectDefinition(99, false, false, bundleObjectsResourceDefs);
		CaliforniumBasedObject calObjectBundles = new CaliforniumBasedObject(objectBundlesDef);
		LwM2mClientObject lwm2mObjectBundles = calObjectBundles.getLwM2mClientObject();

		// Create an object model for the list of bundles
		Bundle[] bundles = m_ctx.getBundleContext().getBundles();
		for (int i = 0; i < bundles.length; i++) {

			Bundle bundle = bundles[i];

			// create object instance
			LwM2mClientObjectInstance instance = new LwM2mClientObjectInstance(i, lwm2mObjectBundles, objectBundlesDef);

			// create resources
			instance.addResource(0, new IntegerValueResource((int) bundle.getBundleId(), 0));
			instance.addResource(1, new StringValueResource(bundle.getSymbolicName(), 1));
			instance.addResource(2, new StringValueResource(bundle.getVersion().toString(), 2));

			// add the object instance
			calObjectBundles.onSuccessfulCreate(instance);
		}

		return calObjectBundles;
	}

	public class TimeResource extends TimeLwM2mResource {
		@Override
		public void handleRead(final TimeLwM2mExchange exchange) {
			System.out.println("\tDevice: Reading Current Device Time.");
			exchange.respondContent(new Date());
		}
	}

	public class MemoryFreeResource extends IntegerLwM2mResource {
		@Override
		public void handleRead(final IntegerLwM2mExchange exchange) {
			System.out.println("\tDevice: Reading Memory Free Resource");
			final Random rand = new Random();
			exchange.respondContent(114 + rand.nextInt(50));
		}
	}

	private class IntegerMultipleResource extends MultipleLwM2mResource {

		private final Map<Integer, byte[]> values;

		public IntegerMultipleResource(Integer[] values) {
			this.values = new HashMap();
			for (int i = 0; i < values.length; i++) {
				this.values.put(i, ByteBuffer.allocate(4).putInt(values[i]).array());
			}
		}

		@Override
		public void handleRead(final MultipleLwM2mExchange exchange) {
			exchange.respondContent(values);
		}
	}

	public class StringValueResource extends StringLwM2mResource {

		private String value;
		private final int resourceId;

		public StringValueResource(final String initialValue, final int resourceId) {
			value = initialValue;
			this.resourceId = resourceId;
		}

		public void setValue(final String newValue) {
			value = newValue;
			notifyResourceUpdated();
		}

		public String getValue() {
			return value;
		}

		@Override
		public void handleWrite(final StringLwM2mExchange exchange) {
			System.out.println("\tDevice: Writing on Resource " + resourceId);
			setValue(exchange.getRequestPayload());

			exchange.respondSuccess();
		}

		@Override
		public void handleRead(final StringLwM2mExchange exchange) {
			System.out.println("\tDevice: Reading on Resource " + resourceId);
			exchange.respondContent(value);
		}

	}

	public class IntegerValueResource extends IntegerLwM2mResource {

		private Integer value;
		private final int resourceId;

		public IntegerValueResource(final int initialValue, final int resourceId) {
			value = initialValue;
			this.resourceId = resourceId;
		}

		public void setValue(final Integer newValue) {
			value = newValue;
			notifyResourceUpdated();
		}

		public Integer getValue() {
			return value;
		}

		@Override
		public void handleWrite(final IntegerLwM2mExchange exchange) {
			System.out.println("\tDevice: Writing on Integer Resource " + resourceId);
			setValue(exchange.getRequestPayload());

			exchange.respondSuccess();
		}

		@Override
		public void handleRead(final IntegerLwM2mExchange exchange) {
			System.out.println("\tDevice: Reading on IntegerResource " + resourceId);
			exchange.respondContent(value);
		}

	}

	public class ExecutableResource extends StringLwM2mResource {

		private final int resourceId;

		public ExecutableResource(final int resourceId) {
			this.resourceId = resourceId;
		}

		@Override
		public void handleExecute(final LwM2mExchange exchange) {
			System.out.println("Executing on Resource " + resourceId);

			exchange.respond(ExecuteResponse.success());
		}

		@Override
		protected void handleWrite(final StringLwM2mExchange exchange) {
			exchange.respondSuccess();
		}

	}

	public class BooleanValueResource extends BooleanLwM2mResource {
		private Boolean value;
		private final int resourceId;

		public BooleanValueResource(Boolean value, int resourceId) {
			this.value = value;
			this.resourceId = resourceId;
		}

		public void setValue(final Boolean newValue) {
			value = newValue;
			notifyResourceUpdated();
		}

		public Boolean getValue() {
			return value;
		}

		@Override
		public void handleWrite(final BooleanLwM2mExchange exchange) {
			System.out.println("\tDevice: Writing on Boolean Resource " + resourceId);
			setValue(exchange.getRequestPayload());

			exchange.respondSuccess();
		}

		@Override
		public void handleRead(final BooleanLwM2mExchange exchange) {
			System.out.println("\tDevice: Reading on BooleanResource " + resourceId);
			exchange.respondContent(value);
		}

	}
}
