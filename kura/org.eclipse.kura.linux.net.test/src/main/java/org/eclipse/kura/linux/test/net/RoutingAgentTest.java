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
package org.eclipse.kura.linux.test.net;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingAgentTest extends TestCase {
	
	private static final Logger s_logger = LoggerFactory.getLogger(RoutingAgentTest.class);

	/*
	private static IRoutingAgent s_routingAgentService = null;

	
	public void setRoutingAgent(IRoutingAgent ras) {
		s_routingAgentService = ras;
	}
	
	public void unsetRoutingAgent(IRoutingAgent ras) {
		s_routingAgentService = null;
	}*/

	/*
	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void testServiceExists() {
		assertNotNull(s_routingAgentService);
	}

	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void addEthernetWifiInterface() {
		String iface = "eth0";
		
		s_logger.info("Adding EthernetWifi interface");
		//s_routingAgentService.addInterface(new NetworkInterfaceStatus(iface, iface, 0, false, false, true, false, false, null, null, null));
		
		assertTrue("Has interface " + iface, s_routingAgentService.hasInterface(iface));
	}

	@TestTarget(targetPlatforms={TestTarget.PLATFORM_ALL})
	@Test
	public void addCellularInterface() {
		String iface = "eth1";
		
		s_logger.info("Adding interfaces");
		//s_routingAgentService.addInterface(new NetworkInterfaceStatus(iface, true, true, false, true, true, null, null, null));
		
		assertTrue("Has interface " + iface, s_routingAgentService.hasInterface(iface));
	}	
	*/
}
