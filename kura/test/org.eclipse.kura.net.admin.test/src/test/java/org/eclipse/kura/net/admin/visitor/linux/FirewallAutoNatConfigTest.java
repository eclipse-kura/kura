/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.core.net.EthernetInterfaceConfigImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.linux.net.iptables.NATRule;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.junit.Test;

public class FirewallAutoNatConfigTest {

    @Test
    public void testWriterVisit() throws KuraException {
        String intfName = "testinterface";
        String destinationInterface = "destIntf";
        boolean[] visited = { false, false };

        FirewallAutoNatConfigWriter writer = new FirewallAutoNatConfigWriter() {

            @Override
            protected void applyNatConfig(NetworkConfiguration networkConfig) throws KuraException {
                visited[0] = true;
            }
        };

        NetworkConfiguration config = prepareNetworkConfiguration(intfName, destinationInterface, false, false);

        CommandExecutorService esMock = mock(CommandExecutorService.class);
        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));
        when(esMock.execute(anyObject())).thenReturn(status);

        writer.setExecutorService(esMock);
        writer.visit(config);

        assertTrue("Configuration expected to be applied.", visited[0]);
    }

    private NetworkConfiguration prepareNetworkConfiguration(String intfName, String destinationInterface,
            boolean second, boolean omitNetConfigs) {
        NetworkConfiguration config = new NetworkConfiguration();

        WifiInterfaceConfigImpl netInterfaceConfig = new WifiInterfaceConfigImpl(intfName);
        config.addNetInterfaceConfig(netInterfaceConfig);

        List<WifiInterfaceAddressConfig> interfaceAddressConfigs = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wifiInterfaceAddressConfig = new WifiInterfaceAddressConfigImpl();

        if (!omitNetConfigs) {
            List<NetConfig> netConfigs = new ArrayList<>();
            FirewallAutoNatConfig natConfig = new FirewallAutoNatConfig();
            natConfig.setDestinationInterface(destinationInterface);
            natConfig.setSourceInterface(intfName);
            natConfig.setMasquerade(true);
            netConfigs.add(natConfig);
            NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
            netConfigs.add(netConfig);
            FirewallNatConfig natConfig2 = new FirewallNatConfig(intfName, destinationInterface, "TCP", "", "", true);
            netConfigs.add(natConfig2);
            wifiInterfaceAddressConfig.setNetConfigs(netConfigs);
        }
        interfaceAddressConfigs.add(wifiInterfaceAddressConfig);
        netInterfaceConfig.setNetInterfaceAddresses(interfaceAddressConfigs);

        if (second) {
            EthernetInterfaceConfigImpl netInterfaceConfig2 = new EthernetInterfaceConfigImpl(destinationInterface);
            config.addNetInterfaceConfig(netInterfaceConfig2);

            List<NetInterfaceAddressConfig> interfaceAddressConfigs2 = new ArrayList<>();
            NetInterfaceAddressConfigImpl ethInterfaceAddressConfig = new NetInterfaceAddressConfigImpl();

            if (!omitNetConfigs) {
                List<NetConfig> netConfigs = new ArrayList<>();
                FirewallAutoNatConfig natConfig = new FirewallAutoNatConfig();
                natConfig.setDestinationInterface(destinationInterface);
                natConfig.setSourceInterface(intfName);
                natConfig.setMasquerade(true);
                netConfigs.add(natConfig);
                NetConfigIP4 netConfig = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledWAN, true);
                netConfigs.add(netConfig);
                ethInterfaceAddressConfig.setNetConfigs(netConfigs);
            }
            interfaceAddressConfigs2.add(ethInterfaceAddressConfig);
            netInterfaceConfig2.setNetInterfaceAddresses(interfaceAddressConfigs2);
        }

        return config;
    }

    @Test
    public void testWriterNatConfigs() throws Throwable {
        String intfName = "testinterface";
        String destinationInterface = "destIntf";

        FirewallAutoNatConfigWriter writer = new FirewallAutoNatConfigWriter();

        NetworkConfiguration config = prepareNetworkConfiguration(intfName, destinationInterface, true, false);

        LinkedHashSet<NATRule> natConfigs = (LinkedHashSet<NATRule>) TestUtil.invokePrivate(writer, "getNatConfigs",
                config);

        assertNotNull(natConfigs);
        assertEquals(1, natConfigs.size());
        NATRule natRule = natConfigs.iterator().next();
        assertEquals(destinationInterface, natRule.getDestinationInterface());
        assertEquals(intfName, natRule.getSourceInterface());
    }

}
