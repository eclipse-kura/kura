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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.lwm2m.objects.LwM2mDeviceObjectDefinition;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.leshan.ResponseCode;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
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

	private LeshanClient m_lwM2mClient;
	private String m_registrationId;

	private SystemService m_systemService;
	private SystemAdminService m_systemAdminService;
	private NetworkService m_networkService;
	private EventAdmin m_eventAdmin;
	private PositionService m_positionService;
	private ConfigurationService m_configurationService;

	private ScheduledExecutorService m_updater;
	private ScheduledFuture<?> m_updater_future;

	public LwM2mClientKura() {
		m_updater = Executors.newSingleThreadScheduledExecutor();
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
		if (m_registrationId != null) {
			s_logger.info("\tDevice: Deregistering Client '" + m_registrationId + "'");
			final DeregisterRequest deregisterRequest = new DeregisterRequest(m_registrationId);
			final LwM2mResponse deregisterResponse = m_lwM2mClient.send(deregisterRequest);

			if (deregisterResponse.getCode() == ResponseCode.DELETED) {
				s_logger.info("\tDevice: Client Location Deregistered'" + deregisterResponse.getCode() + "'");
				m_registrationId = null;
			} else {
				s_logger.error("\tDevice Deregistration Error: " + deregisterResponse.getCode());
			}
			m_lwM2mClient.stop();
		}
	}

	private void register() throws KuraException, UnknownHostException, ComponentException {

		CoapServer coapServer = new CoapServer();
		
		LwM2mDeviceObjectDefinition device = new LwM2mDeviceObjectDefinition();
		device.setSystemService(m_systemService);
		ObjectsInitializer initializer = new ObjectsInitializer();
		initializer.setInstanceForObject(3, device);
		List<ObjectEnabler> enablers = initializer.createMandatory();
		
		

		//LWM2mConfigurableComponentsFactory.getDefault().setConfigurationService(m_configurationService);
		//CaliforniumBasedObject[] comps = LWM2mConfigurableComponentsFactory.getDefault().createComponentObjects();
		//coapServer.add(comps);

		// Connect to the server provided
		s_logger.info("Connecting from: " + m_options.getClientIpAddress() + ":" + m_options.getClientIpPort() + " to " + m_options.getServerIpAddress() + ":"
				+ m_options.getServerIpPort());
		final InetSocketAddress clientAddress = new InetSocketAddress(m_options.getClientIpAddress(), m_options.getClientIpPort());
		final InetSocketAddress serverAddress = new InetSocketAddress(m_options.getServerIpAddress(), m_options.getServerIpPort());

		m_lwM2mClient = new  LeshanClient(clientAddress, serverAddress, coapServer, new ArrayList<LwM2mObjectEnabler>(enablers));

		m_lwM2mClient.start();

		// Register to the server provided
		// UUID
		// final String endpointIdentifier =
		// "urn:uuuid:"+UUID.randomUUID().toString();
		// <OUI>-<Product Class>-<Serial Number>
		final String endpointIdentifier = "urn:dev:ops:" + m_systemService.getPrimaryMacAddress() + "-" + "Kura_" + m_systemService.getDeviceName() + "-"
				+ m_systemService.getSerialNumber();

		final RegisterRequest registerRequest = new RegisterRequest(endpointIdentifier);
		final RegisterResponse registerResponse = m_lwM2mClient.send(registerRequest);

		// Report registration response.
		s_logger.info("Device Registration (Success? " + registerResponse.getCode() + ")");
		if (registerResponse.getCode() == ResponseCode.CREATED) {
			s_logger.info("\tDevice: Registered Client Location '" + registerResponse.getRegistrationID() + "'");
			m_registrationId = registerResponse.getRegistrationID();
		} else {
			s_logger.error("\tDevice Registration Error: " + registerResponse.getCode());
			s_logger.error("If you're having issues connecting to the LWM2M endpoint, try using the DTLS port instead");
		}

		if (m_updater_future != null) {
			m_updater_future.cancel(true);
		}

		m_updater_future = m_updater.scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() {

				try {
					final UpdateRequest updateRequest = new UpdateRequest(m_registrationId, InetAddress.getByName(m_options.getServerIpAddress()), m_options.getServerIpPort());
					final LwM2mResponse updateResponse = m_lwM2mClient.send(updateRequest);

					// Report update response.
					s_logger.info("Device Update (Success? " + updateResponse.getCode() + ")");
					if (updateResponse.getCode() == ResponseCode.CHANGED) {
						s_logger.info("\tDevice: Client Updated '" + updateResponse.getCode() + "'");
						//clientIdentifier = operationResponse.getClientIdentifier();
					} else {
						s_logger.error("\tDevice Update Error: " + updateResponse.getCode());
						deregister();

						register();
					}
				} catch (Exception ex) {
					System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				}

			}
		}, m_options.getUpdateDelay(), m_options.getUpdateDelay(), TimeUnit.MINUTES);

	}

}
