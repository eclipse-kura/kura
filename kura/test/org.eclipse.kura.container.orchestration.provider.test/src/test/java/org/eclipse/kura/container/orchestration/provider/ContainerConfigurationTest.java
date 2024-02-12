/*******************************************************************************
 * Copyright (c) 2022, 2024 Eurotech and/or its affiliates and others
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
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.ContainerConfiguration.ContainerConfigurationBuilder;
import org.eclipse.kura.container.orchestration.ContainerNetworkConfiguration;
import org.eclipse.kura.container.orchestration.ImageConfiguration;
import org.eclipse.kura.container.orchestration.PasswordRegistryCredentials;
import org.junit.Test;

public class ContainerConfigurationTest {

    private static final List<String> CONTAINER_ENV_VARS = Arrays.asList("test=test");
    private static final String CONTAINER_IMAGE_TAG = "tag1";
    private static final String CONTAINER_IMAGE = "image1";
    private static final String CONTAINER_NAME = "cont1";
    private static final String REGISTRY_URL = "https://test";
    private static final String REGISTRY_USERNAME = "test";
    private static final String REGISTRY_PASSWORD = "test1";
    private static final List<Integer> CONTAINER_PORTS_EXTERNAL = Arrays.asList(1521, 81);
    private static final List<Integer> CONTAINER_PORTS_INTERNAL = Arrays.asList(1521, 81);
    private static final Map<String, String> CONTAINER_VOLUMES = Collections.singletonMap("key1", "val1");
    private static final Map<String, String> CONTAINER_LOGGER_PARAMETERS = Collections.singletonMap("key2", "val2");
    private static final String CONTAINER_LOGGER_TYPE = "test2";
    private static final List<String> CONTAINER_DEVICE_LIST = Arrays.asList("/dev/gpio1", "/dev/gpio2");
    private static final Long MEMORY = 1000L;
    private static final Float CPUS = 1.3F;
    private static final String GPUS = "all";
    private static final String RUNTIME = "coolRuntime";
    private ContainerConfiguration firstContainerConfig;
    private ContainerConfiguration secondContainerConfig;
    private ContainerConfigurationBuilder containerConfigurationBuilder;
    private ImageConfiguration firstImageConfig;
    private ImageConfiguration seccondImageConfig;
    private boolean comparisonResult;
    private int hashResult;

    @Test(expected = NullPointerException.class)
    public void testRequiredContainerName() {
        givenContainerNoParams();
    }

    @Test(expected = NullPointerException.class)
    public void testRequiredContainerImage() {
        givenContainerNoImage();
    }

    @Test(expected = NullPointerException.class)
    public void testRequiredContainerCredentials() {
        givenContainerNoCredentials();
    }

    @Test
    public void testSupportOfParameters() {
        givenContainerBuilder();

        whenContainerConfigurationBuilt();

        thenContainerPropertiesMatchExpected();
    }

    @Test
    public void testContainerDoesntEquals() {
        givenContainerOne();
        givenContainerTwoDifferent();

        whenContainersCompared();

        thenFirstContainerDoesntEqualSecond();
    }

    @Test
    public void testContainerHashCode() {
        givenContainerOne();
        givenContainerTwoDifferent();

        whenContainerTwoHash();

        thenHashNoMatch();
    }

    /**
     * End Of Tests
     */

    // given
    private void givenContainerNoParams() {

        this.firstContainerConfig = ContainerConfiguration.builder().build();
    }

    private void givenContainerNoImage() {

        this.firstContainerConfig = ContainerConfiguration.builder().setContainerName(CONTAINER_NAME).build();
    }

    private void givenContainerNoCredentials() {
        this.firstImageConfig = new ImageConfiguration.ImageConfigurationBuilder().setImageName(CONTAINER_IMAGE)
                .setImageTag(CONTAINER_IMAGE_TAG).setImageDownloadTimeoutSeconds(0).build();

        this.firstContainerConfig = ContainerConfiguration.builder().setContainerName(CONTAINER_NAME)
                .setImageConfiguration(firstImageConfig).build();
    }

    private void givenContainerBuilder() {
        this.firstImageConfig = new ImageConfiguration.ImageConfigurationBuilder().setImageName(CONTAINER_IMAGE)
                .setImageTag(CONTAINER_IMAGE_TAG)
                .setRegistryCredentials(Optional.of(new PasswordRegistryCredentials(Optional.of(REGISTRY_URL),
                        REGISTRY_USERNAME, new Password(REGISTRY_PASSWORD))))
                .setImageDownloadTimeoutSeconds(0).build();

        this.containerConfigurationBuilder = ContainerConfiguration.builder().setContainerName(CONTAINER_NAME)
                .setImageConfiguration(firstImageConfig).setExternalPorts(CONTAINER_PORTS_EXTERNAL)
                .setInternalPorts(CONTAINER_PORTS_INTERNAL).setEnvVars(CONTAINER_ENV_VARS)
                .setDeviceList(CONTAINER_DEVICE_LIST).setVolumes(CONTAINER_VOLUMES).setPrivilegedMode(false)
                .setFrameworkManaged(false).setLoggerParameters(CONTAINER_LOGGER_PARAMETERS)
                .setLoggingType(CONTAINER_LOGGER_TYPE)
                .setContainerNetowrkConfiguration(
                        new ContainerNetworkConfiguration.ContainerNetworkConfigurationBuilder()
                                .setNetworkMode(Optional.of("bridge")).build())
                .setMemory(Optional.of(MEMORY)).setCpus(Optional.of(CPUS)).setGpus(Optional.of(GPUS))
                .setRuntime(Optional.of(RUNTIME));
    }

    private void givenContainerOne() {
        givenContainerBuilder();
        whenContainerConfigurationBuilt();
    }

    private void givenContainerTwoDifferent() {

        givenContainerBuilder();
        this.seccondImageConfig = new ImageConfiguration.ImageConfigurationBuilder().setImageName("different")
                .setImageTag("different")
                .setRegistryCredentials(Optional.of(new PasswordRegistryCredentials(Optional.of("different"),
                        REGISTRY_USERNAME, new Password("different"))))
                .setImageDownloadTimeoutSeconds(0).build();
        this.secondContainerConfig = this.containerConfigurationBuilder.setImageConfiguration(seccondImageConfig)
                .build();
    }

    private void whenContainerConfigurationBuilt() {
        this.firstContainerConfig = this.containerConfigurationBuilder.build();
    }

    private void whenContainersCompared() {
        this.comparisonResult = this.firstContainerConfig.equals(this.secondContainerConfig);
    }

    private void whenContainerTwoHash() {
        this.hashResult = this.secondContainerConfig.hashCode();
    }

    // then
    private void thenContainerPropertiesMatchExpected() {
        assertEquals(CONTAINER_NAME, this.firstContainerConfig.getContainerName());
        assertEquals(CONTAINER_IMAGE, this.firstContainerConfig.getImageConfiguration().getImageName());
        assertEquals(CONTAINER_IMAGE_TAG, this.firstContainerConfig.getImageConfiguration().getImageTag());

        assertEquals(CONTAINER_PORTS_EXTERNAL, this.firstContainerConfig.getContainerPortsExternal());
        assertEquals(CONTAINER_PORTS_INTERNAL, this.firstContainerConfig.getContainerPortsInternal());
        assertEquals(CONTAINER_VOLUMES, this.firstContainerConfig.getContainerVolumes());
        assertEquals(CONTAINER_LOGGER_PARAMETERS, this.firstContainerConfig.getLoggerParameters());
        assertEquals(CONTAINER_LOGGER_TYPE, this.firstContainerConfig.getContainerLoggingType());
        assertEquals(CONTAINER_ENV_VARS, this.firstContainerConfig.getContainerEnvVars());
        assertEquals(CONTAINER_DEVICE_LIST, this.firstContainerConfig.getContainerDevices());

        PasswordRegistryCredentials prc = (PasswordRegistryCredentials) this.firstContainerConfig
                .getImageConfiguration().getRegistryCredentials().get();
        assertEquals(REGISTRY_URL, prc.getUrl().get());
        assertEquals(REGISTRY_USERNAME, prc.getUsername());
        assertEquals(REGISTRY_PASSWORD, new String(prc.getPassword().getPassword()));

        assertFalse(this.firstContainerConfig.isContainerPrivileged());
        assertFalse(this.firstContainerConfig.isFrameworkManaged());
        assertEquals(0, this.firstContainerConfig.getImageConfiguration().getimageDownloadTimeoutSeconds());
        assertEquals(Optional.of(MEMORY), this.firstContainerConfig.getMemory());
        assertEquals(Optional.of(CPUS), this.firstContainerConfig.getCpus());
        assertEquals(Optional.of(GPUS), this.firstContainerConfig.getGpus());
    }

    private void thenFirstContainerDoesntEqualSecond() {
        assertFalse(this.comparisonResult);
    }

    private void thenHashNoMatch() {
        assertNotEquals(this.firstContainerConfig.hashCode(), this.hashResult);
    }
}
