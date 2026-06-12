/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.cloud;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.message.KuraDeviceProfile;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.system.SystemService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.util.measurement.Measurement;
import org.osgi.util.measurement.Unit;
import org.osgi.util.position.Position;

public class LifeCyclePayloadBuilderTest {

    private SystemService systemService;
    private SystemAdminService sysAdminService;
    private Optional<NetworkService> networkService;
    private Optional<PositionService> positionService;
    private CloudServiceImpl cloudServiceImpl;
    private LifeCyclePayloadBuilder lifeCyclePayloadBuilder;
    private KuraDeviceProfile kuraDeviceProfile;

    private final List<NetInterface<? extends NetInterfaceAddress>> emptyNetInterfacesList = new ArrayList<NetInterface<? extends NetInterfaceAddress>>();

    @Test
    public void shoudBuildDeviceProfileWithPositionIfPositionServiceIsAvaiable() throws KuraException {
        givenSystemService();
        givenSystemAdminService();
        givenNetworkService(emptyNetInterfacesList);
        givenPositionService();
        givenCloudServiceImpl();
        givenLifeCyclePayloadBuilder();

        whenBuildDeviceProfile();

        thenDeviceProfileHasPosition();
    }

    @Test
    public void shoudBuildDeviceProfileWithoutPositionIfPositionServiceIsNotAvaiable() throws KuraException {
        givenSystemService();
        givenSystemAdminService();
        givenNetworkService(emptyNetInterfacesList);
        givenCloudServiceImpl();
        givenLifeCyclePayloadBuilder();

        whenBuildDeviceProfile();

        thenDeviceProfileHasNotPosition();
    }

    private void givenCloudServiceImpl() {
        this.cloudServiceImpl = mock(CloudServiceImpl.class);
        when(this.cloudServiceImpl.getSystemService()).thenReturn(this.systemService);
        when(this.cloudServiceImpl.getSystemAdminService()).thenReturn(this.sysAdminService);
        when(this.cloudServiceImpl.getNetworkService()).thenReturn(this.networkService);
        when(this.cloudServiceImpl.getPositionService()).thenReturn(this.positionService);
    }

    private void givenLifeCyclePayloadBuilder() {
        this.lifeCyclePayloadBuilder = new LifeCyclePayloadBuilder(this.cloudServiceImpl);
    }

    private void givenSystemService() {
        this.systemService = mock(SystemService.class);
        when(this.systemService.getDeviceName()).thenReturn("deviceName");
        when(this.systemService.getModelName()).thenReturn("modelName");
        when(this.systemService.getModelId()).thenReturn("modelId");
        when(this.systemService.getPartNumber()).thenReturn("partNumber");
        when(this.systemService.getSerialNumber()).thenReturn("serialNumber");
        when(this.systemService.getFirmwareVersion()).thenReturn("firmwareVersion");
        when(this.systemService.getCpuVersion()).thenReturn("cpuVersion");
        when(this.systemService.getBiosVersion()).thenReturn("biosVersion");
        when(this.systemService.getOsName()).thenReturn("osName");
        when(this.systemService.getOsVersion()).thenReturn("osVersion");
        when(this.systemService.getJavaVmName()).thenReturn("javaVmName");
        when(this.systemService.getJavaVmVersion()).thenReturn("javaVmVersion");
        when(this.systemService.getJavaVmInfo()).thenReturn("javaVmInfo");
        when(this.systemService.getJavaVendor()).thenReturn("javaVendor");
        when(this.systemService.getJavaVersion()).thenReturn("javaVersion");
        when(this.systemService.getKuraVersion()).thenReturn("kuraVersion");
        when(this.systemService.getNumberOfProcessors()).thenReturn(1);
        when(this.systemService.getTotalMemory()).thenReturn(1L);
        when(this.systemService.getOsArch()).thenReturn("osArch");
        when(this.systemService.getOsgiFwName()).thenReturn("osgiFwName");
        when(this.systemService.getOsgiFwVersion()).thenReturn("osgiFwVersion");
        when(this.systemService.getJavaVmVendor()).thenReturn("javaVmVendor");
        when(this.systemService.getJdkVendorVersion()).thenReturn("jdkVendorVersion");
    }

    private void givenSystemAdminService() {
        this.sysAdminService = mock(SystemAdminService.class);
        when(this.sysAdminService.getUptime()).thenReturn("0");
    }

    private void givenNetworkService(List<NetInterface<? extends NetInterfaceAddress>> netInterfaces)
            throws KuraException {
        this.networkService = Optional.of(mock(NetworkService.class));
        when(this.networkService.get().getActiveNetworkInterfaces()).thenReturn(netInterfaces);
    }

    private void givenPositionService() {
        this.positionService = Optional.of(mock(PositionService.class));
        Position position = new Position(new Measurement(Math.toRadians(1.0), Unit.rad),
                new Measurement(Math.toRadians(2.0), Unit.rad), new Measurement(3.0, Unit.m),
                new Measurement(4.0, Unit.m_s), new Measurement(Math.toRadians(5.0), Unit.rad));
        when(this.positionService.get().getPosition()).thenReturn(position);
    }

    private void whenBuildDeviceProfile() {
        this.kuraDeviceProfile = this.lifeCyclePayloadBuilder.buildDeviceProfile();
    }

    private void thenDeviceProfileHasPosition() {
        assertEquals(Double.valueOf(1.0), kuraDeviceProfile.getLatitude());
        assertEquals(Double.valueOf(2.0), kuraDeviceProfile.getLongitude());
        assertEquals(Double.valueOf(3.0), kuraDeviceProfile.getAltitude());
    }

    private void thenDeviceProfileHasNotPosition() {
        assertEquals(Double.valueOf(0.0), kuraDeviceProfile.getLatitude());
        assertEquals(Double.valueOf(0.0), kuraDeviceProfile.getLongitude());
        assertEquals(Double.valueOf(0.0), kuraDeviceProfile.getAltitude());
    }

    @Before
    public void clear() {
        this.positionService = Optional.empty();
    }
}
