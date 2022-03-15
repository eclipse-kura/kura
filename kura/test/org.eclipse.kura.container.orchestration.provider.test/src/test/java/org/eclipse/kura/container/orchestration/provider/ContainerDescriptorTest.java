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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.junit.Test;

public class ContainerDescriptorTest {

    private static final String H2_DB_NAME = "Kura_H2DB";
    private static final String H2_DB_IMAGE = "joedoe/h2db";
    private static final List<Integer> H2_DB_PORTS_EXTERNAL = new ArrayList<>(Arrays.asList(1521, 81));
    private static final List<Integer> H2_DB_PORTS_INTERNAL = new ArrayList<>(Arrays.asList(1521, 81));
    private ContainerConfiguration firstContainerConfig;
    private ContainerConfiguration secondContainerConfig;
    private ContainerConfiguration thirdContainerConfig;
    private List<ContainerConfiguration> testContainerList;
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

        this.firstContainerConfig = ContainerConfiguration.builder().setContainerImageTag(H2_DB_NAME)
                .setContainerName(H2_DB_NAME).setContainerImage(H2_DB_IMAGE).setExternalPorts(H2_DB_PORTS_EXTERNAL)
                .setInternalPorts(H2_DB_PORTS_INTERNAL).build();
    }

    private void givenContainerTwoDiffrent() {

        this.secondContainerConfig = ContainerConfiguration.builder().setContainerImageTag(H2_DB_NAME)
                .setContainerName(H2_DB_NAME).setContainerImage(H2_DB_IMAGE).setContainerImageTag("diffrent")
                .setExternalPorts(H2_DB_PORTS_EXTERNAL).setInternalPorts(H2_DB_PORTS_INTERNAL).build();
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

    private void whenContainersAreInList() {
        this.testContainerList = new LinkedList<>();
        this.testContainerList.add(this.firstContainerConfig);
        this.testContainerList.add(this.secondContainerConfig);
        this.testContainerList.add(ContainerConfiguration.builder().setContainerName("randomTestName")
                .setContainerImage(H2_DB_IMAGE).build());
    }

    private void whenContainerTrioIsCreated() {
        // Using Set
        this.firstContainerConfig = ContainerConfiguration.builder().setContainerName("abc")
                .setPrivilegedMode(this.privilegedMode).setContainerImage(H2_DB_IMAGE).setVolumes(this.volumes)
                .setEnvVars(this.envVar).setDeviceList(this.devices).build();

        // Using Add by item
        this.secondContainerConfig = ContainerConfiguration.builder().setContainerName("def")
                .setPrivilegedMode(this.privilegedMode).setContainerImage(H2_DB_IMAGE)
                .setVolumes(Collections.singletonMap("test", "~/test/test"))
                .setDeviceList(Arrays.asList("/dev/gpio1", "/dev/gpio2"))
                .setEnvVars(Arrays.asList("test=test", "test2=test2")).build();

        // Using add by list
        this.thirdContainerConfig = ContainerConfiguration.builder().setContainerName("ghi")
                .setPrivilegedMode(this.privilegedMode).setContainerImage(H2_DB_IMAGE).setVolumes(this.volumes)
                .setEnvVars(this.envVar).setDeviceList(this.devices).build();
    }

    // then
    private void thenCompareContainerOneToExpectedOutput() {
        assertEquals(H2_DB_NAME, this.firstContainerConfig.getContainerName());
        assertEquals(H2_DB_IMAGE, this.firstContainerConfig.getContainerImage());
        assertTrue(ArrayUtils.isEquals(H2_DB_PORTS_EXTERNAL, this.firstContainerConfig.getContainerPortsExternal()));
        assertTrue(ArrayUtils.isEquals(H2_DB_PORTS_INTERNAL, this.firstContainerConfig.getContainerPortsInternal()));
    }

    private void thenFirstContainerDoesntEqualSeccond() {
        assertNotEquals(this.firstContainerConfig, this.secondContainerConfig);
    }

    private void thenFindContainerInLists() {
        assertEquals(this.firstContainerConfig, this.testContainerList.stream()
                .filter(container -> H2_DB_NAME.equals(container.getContainerName())).findFirst().orElse(null));
    }

    private void thenTestExpectedPrivilegedFlagOnContainerTrio() {
        assertEquals(this.firstContainerConfig.getContainerPrivileged(), this.privilegedMode);
        assertEquals(this.secondContainerConfig.getContainerPrivileged(), this.privilegedMode);
        assertEquals(this.thirdContainerConfig.getContainerPrivileged(), this.privilegedMode);
    }

    private void thenTestExpectedContainerVolumesOnContainerTrio() {
        assertEquals(this.firstContainerConfig.getContainerVolumes(), this.volumes);
        assertEquals(this.secondContainerConfig.getContainerVolumes(), this.volumes);
        assertEquals(this.thirdContainerConfig.getContainerVolumes(), this.volumes);
    }

    private void thenTestExpectedContainerEnvVarsOnContainerTrio() {
        assertEquals(this.firstContainerConfig.getContainerEnvVars(), this.envVar);
        assertEquals(this.secondContainerConfig.getContainerEnvVars(), this.envVar);
        assertEquals(this.thirdContainerConfig.getContainerEnvVars(), this.envVar);
    }

    private void thenTestExpectedContainerDevicesOnContainerTrio() {
        assertEquals(this.firstContainerConfig.getContainerDevices(), this.devices);
        assertEquals(this.secondContainerConfig.getContainerDevices(), this.devices);
        assertEquals(this.thirdContainerConfig.getContainerDevices(), this.devices);
    }

}
