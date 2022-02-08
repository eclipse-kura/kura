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

package org.eclipse.kura.container.orchestration.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;

public class ContainerDescriptorTest {

    private static final String H2_DB_NAME = "Kura_H2DB";
    private static final String H2_DB_IMAGE = "joedoe/h2db";
    private static final int[] H2_DB_PORTS_EXTERNAL = new int[] { 1521, 81 };
    private static final int[] H2_DB_PORTS_INTERNAL = new int[] { 1521, 81 };
    private final String device = "testDevice";
    private final String volume = "TestVolume";
    private final String path = "~/path/test/";
    private final int[] ports = { 10, 20, 30 }; // plus 40
    private final int[] finalPorts = { 10, 20, 30, 40 };
    private ContainerDescriptor firstContainer;
    private ContainerDescriptor seccondContainer;
    private ContainerDescriptor thirdContainer;
    private List<ContainerDescriptor> testContainerList;
    private Map<String, String> volumes;
    private List<String> devices;
    private List<String> envVar;
    private boolean privilegedMode;

    @Test
    public void testSupportOfBasicParameters() {
        givenContainerOne();

        thenCompareContainerOneToExpectedOutput();
    }

    @Test
    public void testSupportOfAppendingParameters() {
        givenContainerOne();

        whenContainerOneAppened();

        thenCompareContainerOneToExpectedOutputAfterAppend();
    }

    @Test
    public void testContainerDoesntEquals() {
        givenContainerOne();
        givenContainerTwoDiffrent();

        thenFirstContainerDoesntEqualSeccond();
    }

    @Test
    public void testFindByStringInList() {
        givenContainerOne();
        givenContainerTwoDiffrent();

        whenContainersAreInList();

        thenFindContainerInLists();
    }

    @Test
    public void shouldSupportContainerPrivilegedFlag() {
        givenExtendedContainerParameters();

        whenContainerTrioIsCreated();

        thenTestExpectedPrivilegedFlagOnContainerTrio();
    }

    @Test
    public void shouldSupportContainerVolumes() {
        givenExtendedContainerParameters();

        whenContainerTrioIsCreated();

        thenTestExpectedContainerVolumesOnContainerTrio();
    }

    @Test
    public void shouldSupportContainerEnvVars() {
        givenExtendedContainerParameters();

        whenContainerTrioIsCreated();

        thenTestExpectedContainerEnvVarsOnContainerTrio();
    }

    @Test
    public void shouldSupportContainerDevices() {
        givenExtendedContainerParameters();

        whenContainerTrioIsCreated();

        thenTestExpectedContainerDevicesOnContainerTrio();
    }

    /**
     * End Of Tests
     */

    // given
    private void givenContainerOne() {

        this.firstContainer = ContainerDescriptor.builder().setContainerImageTag(H2_DB_NAME)
                .setContainerName(H2_DB_NAME).setContainerImage(H2_DB_IMAGE).setExternalPort(H2_DB_PORTS_EXTERNAL)
                .setInternalPort(H2_DB_PORTS_INTERNAL).setContainerID("1d4f4v4x").build();
    }

    private void givenContainerTwoDiffrent() {

        this.seccondContainer = ContainerDescriptor.builder().setContainerImageTag(H2_DB_NAME)
                .setContainerName(H2_DB_NAME).setContainerImage(H2_DB_IMAGE).setContainerImageTag("diffrent")
                .setExternalPort(H2_DB_PORTS_EXTERNAL).setInternalPort(H2_DB_PORTS_INTERNAL).setContainerID("4f3gh4ds4")
                .build();
    }

    private void givenExtendedContainerParameters() {
        this.privilegedMode = false;
        this.volumes = new HashMap<>();
        this.volumes.put("test", "~/test/test");
        this.devices = new LinkedList<>();
        this.devices.add("/dev/gpio1");
        this.devices.add("/dev/gpio2");
        this.envVar = new LinkedList<>();
        this.envVar.add("test=test");
        this.envVar.add("test2=test2");
    }

    // when
    private void whenContainerOneAppened() {
        this.firstContainer = ContainerDescriptor.builder().setContainerImageTag(H2_DB_NAME)
                .setContainerName(H2_DB_NAME).setContainerImage(H2_DB_IMAGE).setExternalPort(H2_DB_PORTS_EXTERNAL)
                .setInternalPort(H2_DB_PORTS_INTERNAL).addDevice(this.device).addVolume(this.volume, this.path)
                .setInternalPort(this.ports).addInternalPort(40).setExternalPort(this.ports).addExternalPort(40)
                .build();
    }

    private void whenContainersAreInList() {
        this.testContainerList = new LinkedList<>();
        this.testContainerList.add(this.firstContainer);
        this.testContainerList.add(this.seccondContainer);
        this.testContainerList.add(ContainerDescriptor.builder().setContainerName("randomTestName")
                .setContainerImage(H2_DB_IMAGE).build());
    }

    private void whenContainerTrioIsCreated() {
        // Using Set
        this.firstContainer = ContainerDescriptor.builder().setContainerName("abc")
                .setPrivilegedMode(this.privilegedMode).setContainerImage(H2_DB_IMAGE).setVolume(this.volumes)
                .setEnvVar(this.envVar).setDeviceList(this.devices).build();

        // Using Add by item
        this.seccondContainer = ContainerDescriptor.builder().setContainerName("def")
                .setPrivilegedMode(this.privilegedMode).setContainerImage(H2_DB_IMAGE).addVolume("test", "~/test/test")
                .addDevice("/dev/gpio1").addDevice("/dev/gpio2").addEnvVar("test=test").addEnvVar("test2=test2")
                .build();

        // Using add by list
        this.thirdContainer = ContainerDescriptor.builder().setContainerName("ghi")
                .setPrivilegedMode(this.privilegedMode).setContainerImage(H2_DB_IMAGE).addVolume(this.volumes)
                .addEnvVar(this.envVar).addDevice(this.devices).build();
    }

    // then
    private void thenCompareContainerOneToExpectedOutput() {
        assertEquals(H2_DB_NAME, this.firstContainer.getContainerName());
        assertEquals(H2_DB_IMAGE, this.firstContainer.getContainerImage());
        assertTrue(ArrayUtils.isEquals(H2_DB_PORTS_EXTERNAL, this.firstContainer.getContainerPortsExternal()));
        assertTrue(ArrayUtils.isEquals(H2_DB_PORTS_INTERNAL, this.firstContainer.getContainerPortsInternal()));
    }

    private void thenCompareContainerOneToExpectedOutputAfterAppend() {
        assertEquals(H2_DB_NAME, this.firstContainer.getContainerName());
        assertEquals(H2_DB_IMAGE, this.firstContainer.getContainerImage());
        assertTrue(ArrayUtils.isEquals(this.finalPorts, this.firstContainer.getContainerPortsExternal()));
        assertTrue(ArrayUtils.isEquals(this.finalPorts, this.firstContainer.getContainerPortsInternal()));
        assertEquals(this.device, this.firstContainer.getContainerDevices().get(0));
        assertEquals(this.path, this.firstContainer.getContainerVolumes().get(this.volume));
    }

    private void thenFirstContainerDoesntEqualSeccond() {
        assertFalse(ContainerDescriptor.equals(this.firstContainer, this.seccondContainer));
    }

    private void thenFindContainerInLists() {
        assertEquals(this.firstContainer, ContainerDescriptor.findByName(H2_DB_NAME, this.testContainerList));
    }

    private void thenTestExpectedPrivilegedFlagOnContainerTrio() {
        assertEquals(this.firstContainer.getContainerPrivileged(), this.privilegedMode);
        assertEquals(this.seccondContainer.getContainerPrivileged(), this.privilegedMode);
        assertEquals(this.thirdContainer.getContainerPrivileged(), this.privilegedMode);
    }

    private void thenTestExpectedContainerVolumesOnContainerTrio() {
        assertEquals(this.firstContainer.getContainerVolumes(), this.volumes);
        assertEquals(this.seccondContainer.getContainerVolumes(), this.volumes);
        assertEquals(this.thirdContainer.getContainerVolumes(), this.volumes);
    }

    private void thenTestExpectedContainerEnvVarsOnContainerTrio() {
        assertEquals(this.firstContainer.getContainerEnvVars(), this.envVar);
        assertEquals(this.seccondContainer.getContainerEnvVars(), this.envVar);
        assertEquals(this.thirdContainer.getContainerEnvVars(), this.envVar);
    }

    private void thenTestExpectedContainerDevicesOnContainerTrio() {
        assertEquals(this.firstContainer.getContainerDevices(), this.devices);
        assertEquals(this.seccondContainer.getContainerDevices(), this.devices);
        assertEquals(this.thirdContainer.getContainerDevices(), this.devices);
    }

}
