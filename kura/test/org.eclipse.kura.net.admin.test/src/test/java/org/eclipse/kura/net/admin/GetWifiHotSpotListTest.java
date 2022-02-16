/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/

package org.eclipse.kura.net.admin;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;
import org.eclipse.kura.core.net.WifiInterfaceAddressConfigImpl;
import org.eclipse.kura.core.net.WifiInterfaceConfigImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.eclipse.kura.linux.net.wifi.WpaSupplicantManager;
import org.eclipse.kura.net.admin.visitor.linux.WpaSupplicantConfigWriter;
import org.eclipse.kura.net.admin.visitor.linux.WpaSupplicantConfigWriterFactory;
import org.eclipse.kura.net.wifi.WifiInterfaceAddressConfig;
import org.eclipse.kura.net.wifi.WifiMode;
import org.junit.Test;

public class GetWifiHotSpotListTest {

    private NetworkAdminServiceImpl nas;
    private String interfaceName;

    @Test
    public void getHotSpotWhileInfMasterMode() {
        givenInterfaceName("test_iName");
        givenNetworkAdminService();

        whenWifiModeIs(WifiMode.MASTER);

        thenHotSpotListIsReturned();
    }

    private void thenHotSpotListIsReturned() {
        try {
            assertNotNull(this.nas.getWifiHotspotList(this.interfaceName));
        } catch (KuraException e) {
            fail();
        }
    }

    private void givenNetworkAdminService() {

        WpaSupplicantConfigWriter wpaSupplicantConfigWriter = mock(WpaSupplicantConfigWriter.class);

        WpaSupplicantConfigWriterFactory wpaSupplicantConfigWriterFactory = mock(
                WpaSupplicantConfigWriterFactory.class);

        when(wpaSupplicantConfigWriterFactory.getInstance()).thenReturn(wpaSupplicantConfigWriter);

        this.nas = new NetworkAdminServiceImpl(wpaSupplicantConfigWriterFactory);
        NetworkConfigurationService ncs = mock(NetworkConfigurationService.class);
        this.nas.setNetworkConfigurationService(ncs);

        LinuxNetworkUtil linuxNetworkUtil = mock(LinuxNetworkUtil.class);

        WpaSupplicantManager wpaSupplicantManager = mock(WpaSupplicantManager.class);

        try {
            when(linuxNetworkUtil.getInterfaceConfiguration(this.interfaceName)).thenReturn(null);
            when(linuxNetworkUtil.getWifiMode(this.interfaceName + "_ap")).thenReturn(WifiMode.INFRA);

            TestUtil.setFieldValue(this.nas, "linuxNetworkUtil", linuxNetworkUtil);
            TestUtil.setFieldValue(this.nas, "wpaSupplicantManager", wpaSupplicantManager);
        } catch (Exception e) {
            fail();
        }
    }

    private void givenInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    private void whenWifiModeIs(WifiMode master) {
        NetworkConfigurationService ncsMock = mock(NetworkConfigurationService.class);
        this.nas.setNetworkConfigurationService(ncsMock);

        NetworkConfiguration nc = new NetworkConfiguration();
        WifiInterfaceConfigImpl nic = new WifiInterfaceConfigImpl(this.interfaceName);
        List<WifiInterfaceAddressConfig> ias = new ArrayList<>();
        WifiInterfaceAddressConfigImpl wiac = new WifiInterfaceAddressConfigImpl();

        wiac.setMode(WifiMode.MASTER);
        ias.add(wiac);
        nic.setNetInterfaceAddresses(ias);
        nc.addNetInterfaceConfig(nic);

        try {
            when(ncsMock.getNetworkConfiguration()).thenReturn(nc);
        } catch (KuraException e) {
            fail();
        }
    }
}
