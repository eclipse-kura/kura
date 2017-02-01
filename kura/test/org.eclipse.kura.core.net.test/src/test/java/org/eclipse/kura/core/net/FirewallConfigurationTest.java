/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.kura.core.net;

import static org.junit.Assert.*;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.net.wifi.WifiConfig;
import org.junit.Test;

public class FirewallConfigurationTest {

	@Test
	public void testFirewallConfiguration() {
		FirewallConfiguration conf = new FirewallConfiguration();
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());
	}

	@Test
	public void testFirewallConfigurationEmptyProperties() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.OPEN_PORTS_PROP_NAME, "");
		properties.put(FirewallConfiguration.PORT_FORWARDING_PROP_NAME, "");
		properties.put(FirewallConfiguration.NAT_PROP_NAME, "");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());
	}
	
	@Test
	public void testFirewallConfigurationFromOpenPortConfig() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.OPEN_PORTS_PROP_NAME,
				"42,tcp,10.0.0.1/24,PIN,UIN,PM,SPR,#;"
				+ "42:100,udp,,,,,,#");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());

		assertEquals(2, conf.getOpenPortConfigs().size());
		
		try {
			FirewallOpenPortConfigIP4 expected1 = new FirewallOpenPortConfigIP4(
					42,
					NetProtocol.tcp,
					new NetworkPair<IP4Address>((IP4Address) IP4Address.parseHostAddress("10.0.0.1"), (short)24),
					"PIN",
					"UIN",
					"PM",
					"SPR");
			assertEquals(expected1, conf.getOpenPortConfigs().get(0));

			FirewallOpenPortConfigIP4 expected2 = new FirewallOpenPortConfigIP4(
					"42:100",
					NetProtocol.udp,
					new NetworkPair<IP4Address>((IP4Address) IP4Address.parseHostAddress("127.0.0.1"), (short)0),
					null,
					null,
					null,
					null);
			assertEquals(expected2, conf.getOpenPortConfigs().get(1));
		} catch (UnknownHostException e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testFirewallConfigurationFromOpenPortConfigInvalid1() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.OPEN_PORTS_PROP_NAME, "42,udp,,,,,#");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());
	}

	@Test
	public void testFirewallConfigurationFromOpenPortConfigInvalid2() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.OPEN_PORTS_PROP_NAME, "42,udp,,,,,,,#;,,asd/xyz,,,,,#");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());
	}

	@Test
	public void testFirewallConfigurationFromOpenPortConfigInvalid3() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.OPEN_PORTS_PROP_NAME, "42,udp,,,,,,$");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());
	}
	
	@Test
	public void testFirewallConfigurationFromPortForwardConfig() {
		
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.PORT_FORWARDING_PROP_NAME,
				"IF,OF,10.0.1.1,udp,42,100,true,10.0.0.1/24,PM,SPR,#;"
				+ ",,,tcp,0,0,false,,,,#");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());

		assertEquals(2, conf.getPortForwardConfigs().size());
		
		try {
			FirewallPortForwardConfigIP4 expected1 = new FirewallPortForwardConfigIP4(
					"IF",
					"OF",
					(IP4Address) IP4Address.parseHostAddress("10.0.1.1"),
		            NetProtocol.udp,
		            42,
		            100,
		            true,
					new NetworkPair<IP4Address>((IP4Address) IP4Address.parseHostAddress("10.0.0.1"), (short)24),
		            "PM",
		            "SPR");
			assertEquals(expected1, conf.getPortForwardConfigs().get(0));
			
			FirewallPortForwardConfigIP4 expected2 = new FirewallPortForwardConfigIP4(
					null,
					null,
					(IP4Address) IP4Address.parseHostAddress("127.0.0.1"),
		            NetProtocol.tcp,
		            0,
		            0,
		            false,
					new NetworkPair<IP4Address>((IP4Address) IP4Address.parseHostAddress("127.0.0.1"), (short)0),
		            null,
		            null);
			assertEquals(expected2, conf.getPortForwardConfigs().get(1));
		} catch (UnknownHostException e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testFirewallConfigurationFromPortForwardConfigInvalid1() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.PORT_FORWARDING_PROP_NAME, ",,,tcp,0,0,false,,,#");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());
	}

	@Test
	public void testFirewallConfigurationFromPortForwardConfigInvalid2() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.PORT_FORWARDING_PROP_NAME,
				",,,tcp,0,0,false,,,,,#;,,,tcp,0,0,false,asd/xyz,,,#");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());
	}

	@Test
	public void testFirewallConfigurationFromPortForwardConfigInvalid3() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.PORT_FORWARDING_PROP_NAME, ",,,tcp,0,0,false,,,,$");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());
	}
	
	@Test
	public void testFirewallConfigurationFromNatConfig() {
		
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.NAT_PROP_NAME,
				"SI,DI,P,S,D,true,#;,,,,,false,#");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());

		assertEquals(2, conf.getNatConfigs().size());
		
		FirewallNatConfig expected1 = new FirewallNatConfig("SI", "DI", "P", "S", "D", true);
		assertEquals(expected1, conf.getNatConfigs().get(0));
		
		FirewallNatConfig expected2 = new FirewallNatConfig(null, null, null, null, null, false);
		assertEquals(expected2, conf.getNatConfigs().get(1));
	}

	@Test
	public void testFirewallConfigurationFromNatConfigInvalid1() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.NAT_PROP_NAME, ",,,,false,#");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());
	}

	@Test
	public void testFirewallConfigurationFromNatConfigInvalid2() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.NAT_PROP_NAME, ",,,,,false,,#");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());
	}

	@Test
	public void testFirewallConfigurationFromNatConfigInvalid3() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.NAT_PROP_NAME, ",,,,,false,$");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());
	}
	
	@Test
	public void testFirewallConfigurationFromAllConfig() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(FirewallConfiguration.OPEN_PORTS_PROP_NAME,
				"42,tcp,10.0.0.1/24,PIN,UIN,PM,SPR,#");
		properties.put(FirewallConfiguration.PORT_FORWARDING_PROP_NAME,
				"IF,OF,10.0.1.1,udp,42,100,true,10.0.0.1/24,PM,SPR,#");
		properties.put(FirewallConfiguration.NAT_PROP_NAME,
				"SI,DI,P,S,D,true,#");
		
		FirewallConfiguration conf = new FirewallConfiguration(properties);
		
		assertEquals(1, conf.getOpenPortConfigs().size());
		assertEquals(1, conf.getPortForwardConfigs().size());
		assertEquals(1, conf.getNatConfigs().size());
		assertTrue(conf.getAutoNatConfigs().isEmpty());
		
		try {
			FirewallOpenPortConfigIP4 expected1 = new FirewallOpenPortConfigIP4(
					42,
					NetProtocol.tcp,
					new NetworkPair<IP4Address>((IP4Address) IP4Address.parseHostAddress("10.0.0.1"), (short)24),
					"PIN",
					"UIN",
					"PM",
					"SPR");
			assertEquals(expected1, conf.getOpenPortConfigs().get(0));

			FirewallPortForwardConfigIP4 expected2 = new FirewallPortForwardConfigIP4(
					"IF",
					"OF",
					(IP4Address) IP4Address.parseHostAddress("10.0.1.1"),
		            NetProtocol.udp,
		            42,
		            100,
		            true,
					new NetworkPair<IP4Address>((IP4Address) IP4Address.parseHostAddress("10.0.0.1"), (short)24),
		            "PM",
		            "SPR");
			assertEquals(expected2, conf.getPortForwardConfigs().get(0));

			FirewallNatConfig expected3 = new FirewallNatConfig("SI", "DI", "P", "S", "D", true);
			assertEquals(expected3, conf.getNatConfigs().get(0));
		} catch (UnknownHostException e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testAddConfig() {
		FirewallConfiguration conf = new FirewallConfiguration();
		
		assertTrue(conf.getOpenPortConfigs().isEmpty());
		assertTrue(conf.getPortForwardConfigs().isEmpty());
		assertTrue(conf.getNatConfigs().isEmpty());
		assertTrue(conf.getAutoNatConfigs().isEmpty());

		conf.addConfig(new FirewallOpenPortConfigIP4());
		assertEquals(1, conf.getOpenPortConfigs().size());

		conf.addConfig(new FirewallPortForwardConfigIP4());
		assertEquals(1, conf.getPortForwardConfigs().size());
		
		conf.addConfig(new FirewallNatConfig("", "", "", "", "", false));
		assertEquals(1, conf.getNatConfigs().size());

		conf.addConfig(new FirewallAutoNatConfig());
		assertEquals(1, conf.getAutoNatConfigs().size());

		conf.addConfig(new WifiConfig());
		assertEquals(1, conf.getOpenPortConfigs().size());
		assertEquals(1, conf.getPortForwardConfigs().size());
		assertEquals(1, conf.getNatConfigs().size());
		assertEquals(1, conf.getAutoNatConfigs().size());
	}

	@Test
	public void testGetConfigs() {
		FirewallConfiguration conf = new FirewallConfiguration();
		List<NetConfig> netConfigs = new ArrayList<NetConfig>();
		
		conf.addConfig(new FirewallOpenPortConfigIP4());
		netConfigs.add(new FirewallOpenPortConfigIP4());
		
		conf.addConfig(new FirewallPortForwardConfigIP4());
		netConfigs.add(new FirewallPortForwardConfigIP4());
		
		conf.addConfig(new FirewallNatConfig("", "", "", "", "", false));
		netConfigs.add(new FirewallNatConfig("", "", "", "", "", false));
		
		conf.addConfig(new FirewallAutoNatConfig());
		netConfigs.add(new FirewallAutoNatConfig());
				
		assertArrayEquals(netConfigs.toArray(), conf.getConfigs().toArray());
	}

	@Test
	public void testGetConfigurationPropertiesEmpty() {
		FirewallConfiguration conf = new FirewallConfiguration();

		Map<String, Object> expected = new HashMap<String, Object>();
		expected.put(FirewallConfiguration.OPEN_PORTS_PROP_NAME, "");
		expected.put(FirewallConfiguration.PORT_FORWARDING_PROP_NAME, "");
		expected.put(FirewallConfiguration.NAT_PROP_NAME, "");

		assertEquals(expected, conf.getConfigurationProperties());
	}
	
	@Test
	public void testGetConfigurationPropertiesFormOpenPortConfig() {
		try {
			FirewallConfiguration conf = new FirewallConfiguration();
		
			conf.addConfig(new FirewallOpenPortConfigIP4(
					42,
					NetProtocol.tcp,
					new NetworkPair<IP4Address>((IP4Address) IP4Address.parseHostAddress("10.0.0.1"), (short)24),
					"PIN",
					"UIN",
					"PM",
					"SPR"));
			conf.addConfig(new FirewallOpenPortConfigIP4("42:100", null, null, null, null, null, null));
			
			Map<String, Object> expected = new HashMap<String, Object>();
			expected.put(FirewallConfiguration.OPEN_PORTS_PROP_NAME,
					"42,tcp,10.0.0.1/24,PIN,UIN,PM,SPR,#;"
					+ "42:100,,,,,,,#");
			expected.put(FirewallConfiguration.PORT_FORWARDING_PROP_NAME, "");
			expected.put(FirewallConfiguration.NAT_PROP_NAME, "");
			
			assertEquals(expected, conf.getConfigurationProperties());
		} catch (UnknownHostException e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testGetConfigurationPropertiesFormPortForwardConfig() {
		try {
			FirewallConfiguration conf = new FirewallConfiguration();
			
			conf.addConfig(new FirewallPortForwardConfigIP4(
					"IF",
					"OF",
					(IP4Address) IP4Address.parseHostAddress("10.0.1.1"),
		            NetProtocol.udp,
		            42,
		            100,
		            true,
					new NetworkPair<IP4Address>((IP4Address) IP4Address.parseHostAddress("10.0.0.1"), (short)24),
		            "PM",
		            "SPR"));
			conf.addConfig(new FirewallPortForwardConfigIP4());
			
			Map<String, Object> expected = new HashMap<String, Object>();
			expected.put(FirewallConfiguration.OPEN_PORTS_PROP_NAME, "");
			expected.put(FirewallConfiguration.PORT_FORWARDING_PROP_NAME,
					"IF,OF,10.0.1.1,udp,42,100,true,10.0.0.1/24,PM,SPR,#;"
					+ ",,,,0,0,false,,,,#");
			expected.put(FirewallConfiguration.NAT_PROP_NAME, "");
			
			assertEquals(expected, conf.getConfigurationProperties());
		} catch (UnknownHostException e) {
			fail("unexpected exception");
		}
	}

	@Test
	public void testGetConfigurationPropertiesFormNatConfig() {
		FirewallConfiguration conf = new FirewallConfiguration();
	
		conf.addConfig(new FirewallNatConfig("SI", "DI", "P", "S", "D", true));
		conf.addConfig(new FirewallNatConfig(null, null, null, null, null, false));
		
		Map<String, Object> expected = new HashMap<String, Object>();
		expected.put(FirewallConfiguration.OPEN_PORTS_PROP_NAME, "");
		expected.put(FirewallConfiguration.PORT_FORWARDING_PROP_NAME, "");
		expected.put(FirewallConfiguration.NAT_PROP_NAME,
				"SI,DI,P,S,D,true,#;,,,,,false,#");
		
		assertEquals(expected, conf.getConfigurationProperties());
	}
	
	
	
	
	
	
	
}
