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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import leshan.client.exchange.LwM2mExchange;
import leshan.client.register.RegisterUplink;
import leshan.client.resource.LwM2mClientObjectDefinition;
import leshan.client.resource.LwM2mClientResourceDefinition;
import leshan.client.resource.SingleResourceDefinition;
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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.net.NetworkService;
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


public class LwM2mClient implements ConfigurableComponent, EventHandler
{	
	private static final Logger s_logger = LoggerFactory.getLogger(LwM2mClient.class);
	
	private static final int TIMEOUT_MS = 5000;
	
	private ComponentContext          m_ctx;
	private LwM2mClientOptions        m_options;

	private leshan.client.LwM2mClient m_lwM2mClient;
	private RegisterUplink            m_registerUplink;
	private String 				      m_deviceLocation;
	
	private SystemService             m_systemService;
	private SystemAdminService        m_systemAdminService;
	private NetworkService            m_networkService;
	private EventAdmin                m_eventAdmin;

	public LwM2mClient() {
	}
	
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

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
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("activate...");
		
		//
		// save the bundle context and the properties
		m_ctx = componentContext;
		m_options = new LwM2mClientOptions(properties, m_systemService, m_networkService);
		
		//
		// connect and register
		try {
			register();
		}
		catch (Exception e) {
			s_logger.error("Error during registration", e);
			throw new ComponentException(e);
		}
	}
		
	public void updated(Map<String,Object> properties)
	{
		s_logger.info("updated...: " + properties);

		// Update properties and re-publish Birth certificate
		m_options = new LwM2mClientOptions(properties, m_systemService, m_networkService);
	}
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("deactivate...");

		if (m_deviceLocation != null) {
            System.out.println("\tDevice: Deregistering Client '" + m_deviceLocation + "'");
            m_registerUplink.deregister(m_deviceLocation, TIMEOUT_MS);
        }

		m_systemService      = null;
		m_systemAdminService = null;
		m_networkService     = null;
		m_eventAdmin         = null;
	}
	
	
	public void handleEvent(Event event) 
	{
	}
	
	
	
    private void register()
    	throws KuraException, UnknownHostException
    {
        LwM2mClientObjectDefinition deviceObject = createDeviceDefinition(); 
        
        CoapServer coapServer = new CoapServer();
        m_lwM2mClient = new leshan.client.LwM2mClient(coapServer, deviceObject);

        coapServer.add()
        // Connect to the server provided
        s_logger.info("Connecting from: "+m_options.getClientIpAddress()+":"+m_options.getClientIpPort()+" to "+m_options.getServerIpAddress()+":"+m_options.getServerIpPort());
        final InetSocketAddress clientAddress = new InetSocketAddress(m_options.getClientIpAddress(), m_options.getClientIpPort());
        final InetSocketAddress serverAddress = new InetSocketAddress(m_options.getServerIpAddress(), m_options.getServerIpPort());
        
        m_registerUplink = m_lwM2mClient.startRegistration(clientAddress, serverAddress);
        final OperationResponse operationResponse = m_registerUplink.register(UUID.randomUUID().toString(),
                															  new HashMap<String, String>(), 
                															  TIMEOUT_MS);

        // Report registration response.
        System.out.println("Device Registration (Success? " + operationResponse.isSuccess() + ")");
        if (operationResponse.isSuccess()) {
            System.out.println("\tDevice: Registered Client Location '" + operationResponse.getLocation() + "'");
            String deviceLocation = operationResponse.getLocation();
        } else {
            System.err.println("\tDevice: " + operationResponse.getErrorMessage());
            System.err
                    .println("If you're having issues connecting to the LWM2M endpoint, try using the DTLS port instead");
        }

        // Deregister on shutdown.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
            }
        });
    }

    private LwM2mClientObjectDefinition createDeviceDefinition() 
    {
        // Create an object model
        final StringValueResource manufacturerResource = new StringValueResource("Leshan Example Device", 0);
        final StringValueResource modelResource = new StringValueResource("Model 500", 1);
        final StringValueResource serialNumberResource = new StringValueResource("LT-500-000-0001", 2);
        final StringValueResource firmwareResource = new StringValueResource("1.0.0", 3);
        final ExecutableResource rebootResource = new ExecutableResource(4);
        final ExecutableResource factoryResetResource = new ExecutableResource(5);
        final MultipleLwM2mResource powerAvailablePowerResource = new IntegerMultipleResource(new Integer[] { 0, 4 });
        final MultipleLwM2mResource powerSourceVoltageResource = new IntegerMultipleResource(new Integer[] { 12000,
                                5000 });
        final MultipleLwM2mResource powerSourceCurrentResource = new IntegerMultipleResource(new Integer[] { 150, 75 });
        final IntegerValueResource batteryLevelResource = new IntegerValueResource(92, 9);
        final MemoryFreeResource memoryFreeResource = new MemoryFreeResource();
        final IntegerMultipleResource errorCodeResource = new IntegerMultipleResource(new Integer[] { 0 });
        final TimeResource currentTimeResource = new TimeResource();
        final StringValueResource utcOffsetResource = new StringValueResource(new SimpleDateFormat("X").format(Calendar
                .getInstance().getTime()), 14);
        final StringValueResource timezoneResource = new StringValueResource(TimeZone.getDefault().getID(), 15);
        final StringValueResource bindingsResource = new StringValueResource("U", 16);

        final LwM2mClientObjectDefinition objectDevice = new LwM2mClientObjectDefinition(3, true, true,
                new SingleResourceDefinition(0, manufacturerResource, true), 
                new SingleResourceDefinition(1, modelResource, true), 
                new SingleResourceDefinition(2, serialNumberResource, true),
                new SingleResourceDefinition(3, firmwareResource, true), 
                new SingleResourceDefinition(4, rebootResource, true), 
                new SingleResourceDefinition(5, factoryResetResource, true),
                new SingleResourceDefinition(6, powerAvailablePowerResource, true), 
                new SingleResourceDefinition(7, powerSourceVoltageResource, true), 
                new SingleResourceDefinition(8, powerSourceCurrentResource, true), 
                new SingleResourceDefinition(9, batteryLevelResource, true),
                new SingleResourceDefinition(10, memoryFreeResource, true), 
                new SingleResourceDefinition(11, errorCodeResource, true), 
                new SingleResourceDefinition(12, new ExecutableResource(12), true),
                new SingleResourceDefinition(13, currentTimeResource, true), 
                new SingleResourceDefinition(14, utcOffsetResource, true), 
                new SingleResourceDefinition(15, timezoneResource, true),
                new SingleResourceDefinition(16, bindingsResource, true));
        return objectDevice;
    }

    
    private LwM2mClientObjectDefinition createBundlesDefinition() 
    {
        final LwM2mClientObjectDefinition objectBundles = new LwM2mClientObjectDefinition(99, false, false);

        // Create an object model for the list of bundles
        List<LwM2mClientResourceDefinition> resourceDefinitions = new ArrayList<LwM2mClientResourceDefinition>();
        Bundle[] bundles = m_ctx.getBundleContext().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            
            Bundle bundle = bundles[i];

            final LwM2mClientObjectDefinition objectBundles = new LwM2mClientObjectDefinition(99, false, false);

            StringValueResource resource = new StringValueResource(bundle.getSymbolicName(), i);
            resourceDefinitions.add( new SingleResourceDefinition(i, resource, true));
        }

        final LwM2mClientObjectDefinition objectBundles = new LwM2mClientObjectDefinition(99, true, true, 
                resourceDefinitions.toArray( new LwM2mClientResourceDefinition[]{}));
        return objectBundles;
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
}
