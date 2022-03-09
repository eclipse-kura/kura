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
package org.eclipse.kura.container.provider;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.container.orchestration.provider.ContainerDescriptor;
import org.junit.Test;

public class ConfigurableDockerGernericDockerServiceOptionsTest {

    private static final boolean DEFAULT_ENABLED = false;
    private static final String DEFAULT_PORTS_EXTERNAL = "";
    private static final String DEFAULT_CONTAINER_NAME = "kura_test_container";
    private static final String DEFAULT_IMAGE_TAG = "latest";
    private static final String DEFAULT_IMAGE = "hello-world";
    private static final String DEFAULT_PORTS_INTERNAL = "";
    private static final String DEFAULT_CONTAINER_ENV = "";
    private static final String DEFAULT_CONTAINER_PATH_DESTINATION = "";
    private static final String DEFAULT_CONTAINER_PATH_FILE_PATH = "";
    private static final String DEFAULT_CONTAINER_DEVICE = "";
    private static final String DEFAULT_CONTAINER_LOGGER_PARAMETERS = "";
    private static final String DEFAULT_CONTAINER_LOGGING_TYPE = "default";

    private static final String CONTAINER_ENV = "container.env";
    private static final String CONTAINER_PORTS_INTERNAL = "container.ports.internal";
    private static final String CONTAINER_PORTS_EXTERNAL = "container.ports.external";
    private static final String CONTAINER_NAME = "kura.service.pid";
    private static final String CONTAINER_IMAGE_TAG = "container.image.tag";
    private static final String CONTAINER_IMAGE = "container.image";
    private static final String CONTAINER_ENABLED = "container.enabled";
    private static final String CONTAINER_DEVICE = "container.device";
    private static final String CONTAINER_VOLUME = "container.volume";
    private static final String CONTAINER_LOGGER_PARAMETERS = "container.loggerParameters";
    private static final String CONTAINER_LOGGING_TYPE = "container.loggingType";

    private Map<String, Object> properties;

    private ConfigurableGenericDockerServiceOptions cgdso;

    private boolean enabled;
    private String image;
    private String imageTag;
    private String containerName;
    private List<String> containerEnv;
    private Map<String, String> containerVolumes;
    private List<String> containerDevice;
    private int[] portsAvailable;
    private boolean equals;
    private int hashCode;

    private ContainerDescriptor containerDescriptor;

    private Map<String, Object> newProperties;

    @Test
    public void testEnabledDefault() {

        givenEmptyProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenIsEnabled();

        thenEnabledStateIs(false);

    }

    @Test
    public void testEnabled() {

        givenEmptyProperties();
        givenEnabled(true);
        givenConfigurableGenericDockerServiceOptions();

        whenIsEnabled();

        thenEnabledStateIs(true);

    }

    @Test
    public void testImageDefault() {

        givenEmptyProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetImage();

        thenImage(DEFAULT_IMAGE);

    }

    @Test
    public void testImage() {

        givenEmptyProperties();
        givenImage("test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetImage();

        thenImage("test");
    }

    @Test
    public void testImageTagDefault() {

        givenEmptyProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetImageTag();

        thenImageTag(DEFAULT_IMAGE_TAG);

    }

    @Test
    public void testImageTag() {

        givenEmptyProperties();
        givenImageTag("test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetImageTag();

        thenImageTag("test");
    }

    @Test
    public void testContainerNameDefault() {

        givenEmptyProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerName();

        thenContainerName(DEFAULT_CONTAINER_NAME);

    }

    @Test
    public void testContainerName() {

        givenEmptyProperties();
        givenContainerName("test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerName();

        thenContainerName("test");
    }

    @Test
    public void testContainerEnvDefault() {

        givenEmptyProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerEnv();

        thenContainerEnvIsEmpty();

    }

    @Test
    public void testContainerEnv() {

        givenEmptyProperties();
        givenContainerEnv("test=123");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerEnv();

        thenContainerEnv("test=123");
    }

    @Test
    public void testContainerVolumeDefault() {

        givenEmptyProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerVolume();

        thenContainerVolumeIsEmpty();

    }

    @Test
    public void testContainerVolume() {

        givenEmptyProperties();
        givenContainerVolume("test", "test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerVolume();

        thenContainerVolume("test", "test");
    }

    @Test
    public void testContainerDeviceDefault() {

        givenEmptyProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDevice();

        thenContainerDeviceIsEmpty();

    }

    @Test
    public void testContainerDevice() {

        givenEmptyProperties();
        givenContainerDevice("test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDevice();

        thenContainerDevice("test");
    }

    @Test
    public void shouldSupportMultipleExternalPortsInOneString() {

        String ports = "22, 56, 77, 567, 4455";
        int[] portResult = { 22, 56, 77, 567, 4455 };

        givenEmptyProperties();
        givenPortsConfiguration(CONTAINER_PORTS_EXTERNAL, ports);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerPortsExternal();

        thenPortResult(portResult);

    }

    @Test
    public void shouldSupportSingleExternalPortsInOneString() {

        String ports = "22";
        int[] portResult = { 22 };

        givenEmptyProperties();
        givenPortsConfiguration(CONTAINER_PORTS_EXTERNAL, ports);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerPortsExternal();

        thenPortResult(portResult);

    }

    @Test
    public void shouldSupportSingleBrokenExternalPortsInOneString() {

        String ports = "22,";
        int[] portResult = { 22 };

        givenEmptyProperties();
        givenPortsConfiguration(CONTAINER_PORTS_EXTERNAL, ports);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerPortsExternal();

        thenPortResult(portResult);

    }

    @Test
    public void testMultipleInternalPortsInOneString() {
        String ports = "22, 56, 77, 567, 4455";
        int[] portResult = { 22, 56, 77, 567, 4455 };

        givenEmptyProperties();
        givenPortsConfiguration(CONTAINER_PORTS_INTERNAL, ports);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerPortsInternal();

        thenPortResult(portResult);
    }

    @Test
    public void testMultipleBrokenInternalPortsInOneString() {
        String ports = "56 ,";
        int[] portResult = { 56 };

        givenEmptyProperties();
        givenPortsConfiguration(CONTAINER_PORTS_INTERNAL, ports);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerPortsInternal();

        thenPortResult(portResult);
    }

    @Test
    public void testSingleInternalPortsInOneString() {
        String ports = "56";
        int[] portResult = { 56 };

        givenEmptyProperties();
        givenPortsConfiguration(CONTAINER_PORTS_INTERNAL, ports);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerPortsInternal();

        thenPortResult(portResult);
    }

    @Test
    public void testOptionsEqualsSameObject() {
        givenEmptyProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetEquals(this.cgdso);
        whenGetHashCode(this.cgdso);

        thenIsEqual();
        thenIsSameHashCode();
    }

    @Test
    public void testOptionsEqualsNewSameObject() {
        givenEmptyProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetEquals(new ConfigurableGenericDockerServiceOptions(this.properties));

        thenIsEqual();
        thenIsNotSameHashCode();
    }

    @Test
    public void testOptionsEqualsDifferentObject() {
        givenEmptyProperties();
        givenDifferentProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetEquals(new ConfigurableGenericDockerServiceOptions(this.newProperties));

        thenIsNotEqual();
        thenIsNotSameHashCode();
    }

    @Test
    public void testGetContainerDescriptor() {
        givenEmptyProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDescriptor();

        thenIsNotNullContainerDescriptor();
    }

    private void givenEmptyProperties() {
        this.properties = new HashMap<>();
        this.properties.put(CONTAINER_ENABLED, DEFAULT_ENABLED);
        this.properties.put(CONTAINER_IMAGE, DEFAULT_IMAGE);
        this.properties.put(CONTAINER_IMAGE_TAG, DEFAULT_IMAGE_TAG);
        this.properties.put(CONTAINER_NAME, DEFAULT_CONTAINER_NAME);
        this.properties.put(CONTAINER_PORTS_EXTERNAL, DEFAULT_PORTS_EXTERNAL);
        this.properties.put(CONTAINER_PORTS_INTERNAL, DEFAULT_PORTS_INTERNAL);
        this.properties.put(CONTAINER_ENV, DEFAULT_CONTAINER_ENV);
        this.properties.put(CONTAINER_VOLUME,
                DEFAULT_CONTAINER_PATH_FILE_PATH + ":" + DEFAULT_CONTAINER_PATH_DESTINATION);
        this.properties.put(CONTAINER_DEVICE, DEFAULT_CONTAINER_DEVICE);
        this.properties.put(CONTAINER_LOGGING_TYPE, DEFAULT_CONTAINER_LOGGING_TYPE);
        this.properties.put(CONTAINER_LOGGER_PARAMETERS, DEFAULT_CONTAINER_LOGGER_PARAMETERS);
    }

    private void givenDifferentProperties() {
        this.newProperties = new HashMap<>();
        this.newProperties.put(CONTAINER_ENABLED, true);
        this.newProperties.put(CONTAINER_IMAGE, "myimage");
        this.newProperties.put(CONTAINER_IMAGE_TAG, "mytag");
        this.newProperties.put(CONTAINER_NAME, "myname");
        this.newProperties.put(CONTAINER_PORTS_EXTERNAL, "");
        this.newProperties.put(CONTAINER_PORTS_INTERNAL, "");
        this.newProperties.put(CONTAINER_ENV, "");
        this.newProperties.put(CONTAINER_VOLUME, "diffrent:diffrent");
        this.newProperties.put(CONTAINER_DEVICE, "");
        this.properties.put(CONTAINER_LOGGING_TYPE, "journald");
        this.properties.put(CONTAINER_LOGGER_PARAMETERS, "label=true");
    }

    private void givenConfigurableGenericDockerServiceOptions() {
        this.cgdso = new ConfigurableGenericDockerServiceOptions(this.properties);
    }

    private void givenEnabled(boolean b) {
        this.properties.put(CONTAINER_ENABLED, b);
    }

    private void givenImage(String value) {
        this.properties.put(CONTAINER_IMAGE, value);
    }

    private void givenImageTag(String value) {
        this.properties.put(CONTAINER_IMAGE_TAG, value);
    }

    private void givenContainerName(String value) {
        this.properties.put(CONTAINER_NAME, value);
    }

    private void givenContainerEnv(String value) {
        this.properties.put(CONTAINER_ENV, value);
    }

    private void givenContainerVolume(String source, String dest) {
        this.properties.put(CONTAINER_VOLUME, source + ":" + dest);
    }

    private void givenContainerDevice(String value) {
        this.properties.put(CONTAINER_DEVICE, value);
    }

    private void givenPortsConfiguration(String portProperty, String Ports) {
        this.properties.put(portProperty, Ports);
    }

    private void whenIsEnabled() {
        this.enabled = this.cgdso.isEnabled();
    }

    private void whenGetImage() {
        this.image = this.cgdso.getContainerImage();
    }

    private void whenGetImageTag() {
        this.imageTag = this.cgdso.getContainerImageTag();
    }

    private void whenGetContainerName() {
        this.containerName = this.cgdso.getContainerName();
    }

    private void whenGetContainerEnv() {
        this.containerEnv = this.cgdso.getContainerEnvList();
    }

    private void whenGetContainerVolume() {
        this.containerVolumes = this.cgdso.getContainerVolumeList();
    }

    private void whenGetContainerDevice() {
        this.containerDevice = this.cgdso.getContainerDeviceList();
    }

    private void whenGetContainerPortsExternal() {
        this.portsAvailable = this.cgdso.getContainerPortsExternal();
    }

    private void whenGetContainerPortsInternal() {
        this.portsAvailable = this.cgdso.getContainerPortsInternal();
    }

    private void whenGetEquals(ConfigurableGenericDockerServiceOptions options) {
        this.equals = this.cgdso.equals(options);
    }

    private void whenGetHashCode(ConfigurableGenericDockerServiceOptions options) {
        this.hashCode = options.hashCode();
    }

    private void whenGetContainerDescriptor() {
        this.containerDescriptor = this.cgdso.getContainerDescriptor();
    }

    private void thenEnabledStateIs(boolean b) {
        assertEquals(b, this.enabled);
    }

    private void thenImage(String expectedValue) {
        assertEquals(expectedValue, this.image);
    }

    private void thenImageTag(String expectedValue) {
        assertEquals(expectedValue, this.imageTag);
    }

    private void thenContainerName(String expectedValue) {
        assertEquals(expectedValue, this.containerName);
    }

    private void thenContainerEnv(String expectedValue) {
        assertTrue(this.containerEnv.contains(expectedValue));
    }

    private void thenContainerEnvIsEmpty() {
        assertTrue(this.containerEnv.isEmpty());
    }

    private void thenContainerVolume(String expectedSourceValue, String expectedDestinationValue) {
        assertTrue(this.containerVolumes.containsKey(expectedSourceValue));
        assertTrue(this.containerVolumes.containsValue(expectedDestinationValue));
    }

    private void thenContainerVolumeIsEmpty() {
        assertTrue(this.containerVolumes.isEmpty());
    }

    private void thenContainerDevice(String expectedValue) {
        assertTrue(this.containerDevice.contains(expectedValue));
    }

    private void thenContainerDeviceIsEmpty() {
        assertTrue(this.containerDevice.isEmpty());
    }

    private void thenPortResult(int[] portResult) {
        assertArrayEquals(portResult, this.portsAvailable);
    }

    private void thenIsEqual() {
        assertTrue(this.equals);
    }

    private void thenIsNotEqual() {
        assertFalse(this.equals);
    }

    private void thenIsSameHashCode() {
        assertEquals(this.cgdso.hashCode(), this.hashCode);
    }

    private void thenIsNotSameHashCode() {
        assertNotEquals(this.cgdso.hashCode(), this.hashCode);
    }

    private void thenIsNotNullContainerDescriptor() {
        assertNotNull(this.containerDescriptor);
    }
}
