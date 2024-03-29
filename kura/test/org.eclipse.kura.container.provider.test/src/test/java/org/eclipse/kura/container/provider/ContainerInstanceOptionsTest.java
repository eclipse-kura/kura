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
package org.eclipse.kura.container.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.container.orchestration.ContainerConfiguration;
import org.eclipse.kura.container.orchestration.PasswordRegistryCredentials;
import org.junit.Test;

public class ContainerInstanceOptionsTest {

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
    private static final boolean DEFAULT_CCONTAINER_PRIVILEGED = false;
    private static final int DEFAULT_CONTAINER_IMAGE_DOWNLOAD_RETRIES = 5;
    private static final int DEFAULT_CONTAINER_IMAGE_DOWNLOAD_RETRY_INTERVAL = 30000;
    private static final String DEFAULT_CONTAINER_LOGGER_PARAMETERS = "";
    private static final String DEFAULT_CONTAINER_LOGGING_TYPE = "default";
    private static final String DEFAULT_REGISTRY_URL = "";
    private static final String DEFAULT_REGISTRY_USERNAME = "";
    private static final String DEFAULT_REGISTRY_PASSWORD = "";
    private static final int DEFAULT_IMAGES_DOWNLOAD_TIMEOUT = 500;
    private static final String DEFAULT_CONTAINER_NETWORKING_MODE = "";
    private static final String DEFAULT_CONTAINER_ENTRY_POINT = "";
    private static final String DEFAULT_CONTAINER_MEMORY = "";
    private static final String DEFAULT_CONTAINER_CPUS = "";
    private static final String DEFAULT_CONTAINER_GPUS = "";
    private static final String DEFAULT_CONTAINER_RUNTIME = "";
    private static final String DEFAULT_ENFORCEMENT_DIGEST = "";

    private static final String CONTAINER_ENV = "container.env";
    private static final String CONTAINER_PORTS_INTERNAL = "container.ports.internal";
    private static final String CONTAINER_PORTS_EXTERNAL = "container.ports.external";
    private static final String CONTAINER_NAME = "kura.service.pid";
    private static final String CONTAINER_IMAGE_TAG = "container.image.tag";
    private static final String CONTAINER_IMAGE = "container.image";
    private static final String CONTAINER_ENABLED = "container.enabled";
    private static final String CONTAINER_DEVICE = "container.device";
    private static final String CONTAINER_VOLUME = "container.volume";
    private static final String CONTAINER_PRIVILEGED = "container.privileged";
    private static final String CONTAINER_IMAGE_DOWNLOAD_RETRIES = "container.image.download.retries";
    private static final String CONTAINER_IMAGE_DOWNLOAD_RETRY_INTERVAL = "container.image.download.interval";
    private static final String CONTAINER_LOGGER_PARAMETERS = "container.loggerParameters";
    private static final String CONTAINER_LOGGING_TYPE = "container.loggingType";
    private static final String REGISTRY_URL = "registry.hostname";
    private static final String REGISTRY_USERNAME = "registry.username";
    private static final String REGISTRY_PASSWORD = "registry.password";
    private static final String IMAGES_DOWNLOAD_TIMEOUT = "container.image.download.timeout";
    private static final String CONTAINER_NETWORKING_MODE = "container.networkMode";
    private static final String CONTAINER_ENTRY_POINT = "container.entrypoint";
    private static final String CONTAINER_MEMORY = "container.memory";
    private static final String CONTAINER_CPUS = "container.cpus";
    private static final String CONTAINER_GPUS = "container.gpus";
    private static final String CONTAINER_RUNTIME = "container.runtime";
    private static final String ENFORCEMENT_DIGEST = "enforcement.digest";

    private Map<String, Object> properties;

    private ContainerInstanceOptions cgdso;

    private boolean enabled;
    private String image;
    private String imageTag;
    private String containerName;
    private List<String> containerEnv;
    private Map<String, String> containerVolumes;
    private List<String> containerDevice;
    private List<Integer> portsAvailable;
    private boolean equals;
    private int hashCode;
    private boolean privilegedMode;

    private ContainerConfiguration containerDescriptor;

    private Map<String, Object> newProperties;
    private int imageDownloadRetries;
    private int imageDownloadRetryInterval;
    private boolean unlimitedRetries;

    private Optional<String> registryURL;
    private String registryUsername;
    private String registryPassword;
    private int imageDownloadTimeout;

    @Test(expected = IllegalArgumentException.class)
    public void testNullProperties() {
        givenNullProperties();

        whenConfigurableGenericDockerServiceOptionsCreated();
    }

    @Test
    public void testEnabledDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenIsEnabled();

        thenEnabledStateIs(false);

    }

    @Test
    public void testEnabled() {

        givenDefaultProperties();
        givenEnabled(true);
        givenConfigurableGenericDockerServiceOptions();

        whenIsEnabled();

        thenEnabledStateIs(true);

    }

    @Test
    public void testImageDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetImage();

        thenImage(DEFAULT_IMAGE);

    }

    @Test
    public void testImage() {

        givenDefaultProperties();
        givenImage("test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetImage();

        thenImage("test");
    }

    @Test
    public void testImageTagDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetImageTag();

        thenImageTag(DEFAULT_IMAGE_TAG);

    }

    @Test
    public void testImageTag() {

        givenDefaultProperties();
        givenImageTag("test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetImageTag();

        thenImageTag("test");
    }

    @Test
    public void testContainerNameDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerName();

        thenContainerName(DEFAULT_CONTAINER_NAME);

    }

    @Test
    public void testContainerName() {

        givenDefaultProperties();
        givenContainerName("test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerName();

        thenContainerName("test");
    }

    @Test
    public void testContainerEnvDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerEnv();

        thenContainerEnvIsEmpty();

    }

    @Test
    public void testContainerEnv() {

        givenDefaultProperties();
        givenContainerEnv("test=123");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerEnv();

        thenContainerEnv("test=123");
    }

    @Test
    public void testContainerVolumeDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerVolume();

        thenContainerVolumeIsEmpty();

    }

    @Test
    public void testContainerVolume() {

        givenDefaultProperties();
        givenContainerVolume("test", "test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerVolume();

        thenContainerVolume("test", "test");
    }

    @Test
    public void testContainerDeviceDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDevice();

        thenContainerDeviceIsEmpty();

    }

    @Test
    public void testContainerDevice() {

        givenDefaultProperties();
        givenContainerDevice("test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDevice();

        thenContainerDevice("test");
    }

    @Test
    public void testPrivilegedModeDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetPrivilegedMode();

        thenPrivilegedMode(DEFAULT_CCONTAINER_PRIVILEGED);

    }

    @Test
    public void testPrivilegedMode() {

        givenDefaultProperties();
        givenPrivilegedMode(true);
        givenConfigurableGenericDockerServiceOptions();

        whenGetPrivilegedMode();

        thenPrivilegedMode(true);
    }

    @Test
    public void testImageDownloadRetriesDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetImageDownloadRetries();

        thenImageDownloadRetries(DEFAULT_CONTAINER_IMAGE_DOWNLOAD_RETRIES);

    }

    @Test
    public void testImageDownloadRetries() {

        givenDefaultProperties();
        givenImageDownloadRetries(100);
        givenConfigurableGenericDockerServiceOptions();

        whenGetImageDownloadRetries();

        thenImageDownloadRetries(100);
    }

    @Test
    public void testRegistryURLDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetRegistryURL();

        thenRegistryURL(DEFAULT_REGISTRY_URL);

    }

    @Test
    public void testRegistryURL() {

        givenDefaultProperties();
        givenRegistryURL("https://test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetRegistryURL();

        thenRegistryURL("https://test");
    }

    @Test
    public void testImageDownloadUnlimitedRetriesDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetIsUnlimitedRetries();

        thenIsFalse(this.unlimitedRetries);

    }

    @Test
    public void testImageDownloadUnlimitedRetries() {

        givenDefaultProperties();
        givenImageDownloadRetries(0);
        givenConfigurableGenericDockerServiceOptions();

        whenGetIsUnlimitedRetries();

        thenIsTrue(this.unlimitedRetries);
    }

    @Test
    public void testImageDownloadRetryIntervalDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetImageDownloadRetryInterval();

        thenImageDownloadRetryInterval(DEFAULT_CONTAINER_IMAGE_DOWNLOAD_RETRY_INTERVAL);

    }

    @Test
    public void testImageDownloadRetryInterval() {

        givenDefaultProperties();
        givenImageDownloadRetryInterval(100);
        givenConfigurableGenericDockerServiceOptions();

        whenGetImageDownloadRetryInterval();

        thenImageDownloadRetryInterval(100);
    }

    @Test
    public void testRegistryUsernameDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetRegistryUsername();

        thenRegistryUsername(DEFAULT_REGISTRY_USERNAME);

    }

    @Test
    public void testRegistryUsername() {

        givenDefaultProperties();
        givenRegistryUsername("test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetRegistryUsername();

        thenRegistryUsername("test");
    }

    @Test
    public void testRegistryPasswordDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetRegistryPassword();

        thenRegistryPassword(DEFAULT_REGISTRY_PASSWORD);

    }

    @Test
    public void testRegistryPassword() {

        givenDefaultProperties();
        givenRegistryPassword("test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetRegistryPassword();

        thenRegistryPassword("test");
    }

    @Test
    public void testImageDownloadTimeoutDefault() {

        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetImageDownloadTimeout();

        thenImageDownloadTimeout(DEFAULT_IMAGES_DOWNLOAD_TIMEOUT);

    }

    @Test
    public void testImageDownloadTimeout() {

        givenDefaultProperties();
        givenImageDownloadTimeout(100);
        givenConfigurableGenericDockerServiceOptions();

        whenGetImageDownloadTimeout();

        thenImageDownloadTimeout(100);
    }

    @Test
    public void shouldSupportMultipleExternalPortsInOneString() {

        String ports = "22, 56, 77, 567, 4455";
        List<Integer> portResult = new ArrayList<>(Arrays.asList(22, 56, 77, 567, 4455));

        givenDefaultProperties();
        givenPortsConfiguration(CONTAINER_PORTS_EXTERNAL, ports);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerPortsExternal();

        thenPortResult(portResult);

    }

    @Test
    public void shouldSupportSingleExternalPortsInOneString() {

        String ports = "22";
        List<Integer> portResult = new ArrayList<>(Arrays.asList(22));

        givenDefaultProperties();
        givenPortsConfiguration(CONTAINER_PORTS_EXTERNAL, ports);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerPortsExternal();

        thenPortResult(portResult);

    }

    @Test
    public void shouldSupportSingleBrokenExternalPortsInOneString() {

        String ports = "22,";
        List<Integer> portResult = new ArrayList<>(Arrays.asList(22));

        givenDefaultProperties();
        givenPortsConfiguration(CONTAINER_PORTS_EXTERNAL, ports);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerPortsExternal();

        thenPortResult(portResult);

    }

    @Test
    public void testMultipleInternalPortsInOneString() {
        String ports = "22, 56, 77, 567, 4455";
        List<Integer> portResult = new ArrayList<>(Arrays.asList(22, 56, 77, 567, 4455));

        givenDefaultProperties();
        givenPortsConfiguration(CONTAINER_PORTS_INTERNAL, ports);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerPortsInternal();

        thenPortResult(portResult);
    }

    @Test
    public void testMultipleBrokenInternalPortsInOneString() {
        String ports = "56 ,";
        List<Integer> portResult = new ArrayList<>(Arrays.asList(56));

        givenDefaultProperties();
        givenPortsConfiguration(CONTAINER_PORTS_INTERNAL, ports);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerPortsInternal();

        thenPortResult(portResult);
    }

    @Test
    public void testSingleInternalPortsInOneString() {
        String ports = "56";
        List<Integer> portResult = new ArrayList<>(Arrays.asList(56));

        givenDefaultProperties();
        givenPortsConfiguration(CONTAINER_PORTS_INTERNAL, ports);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerPortsInternal();

        thenPortResult(portResult);
    }

    @Test
    public void testOptionsEqualsSameObject() {
        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetEquals(this.cgdso);
        whenGetHashCode(this.cgdso);

        thenIsEqual();
        thenIsSameHashCode();
    }

    @Test
    public void testOptionsEqualsNewSameObject() {
        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetEquals(new ContainerInstanceOptions(this.properties));

        thenIsEqual();
        thenIsNotSameHashCode();
    }

    @Test
    public void testOptionsEqualsDifferentObject() {
        givenDefaultProperties();
        givenDifferentProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetEquals(new ContainerInstanceOptions(this.newProperties));

        thenIsNotEqual();
        thenIsNotSameHashCode();
    }

    @Test
    public void testOptionsDifferEmptyProperties() {
        givenDefaultProperties();
        givenEmptyNewProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetEquals(new ContainerInstanceOptions(this.newProperties));

        thenIsNotEqual();
        thenIsNotSameHashCode();
    }

    @Test
    public void testGetContainerDescriptor() {
        givenDefaultProperties();
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDescriptor();

        thenIsNotNullContainerDescriptor();
    }

    @Test
    public void testExtraCommaInEntryPointFeild() {
        givenDifferentProperties();
        givenDiffrentConfigurableGenericDockerServiceOptions();

        thenCheckIfExtraCommasAreIgnored();
    }

    @Test
    public void testMemoryOptionEmpty() {
        givenDefaultProperties();
        givenMemoryProperty("");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDescriptor();

        thenContainerMemoryIsEmpty();
    }

    @Test
    public void testMemoryOptionNoSuffix() {
        testMemoryOption("1234", 1234L);
    }

    @Test
    public void testMemoryOptionByteSuffix() {
        testMemoryOption("12345b", 12345L);
    }

    @Test
    public void testMemoryOptionKiloSuffix() {
        testMemoryOption("1111k", 1137664L);
    }

    @Test
    public void testMemoryOptionMegaSuffix() {
        testMemoryOption("2222m", 2329935872L);
    }

    @Test
    public void testMemoryOptionGigaSuffix() {
        testMemoryOption("7g", 7516192768L);
    }

    @Test
    public void testCpusOptionEmpty() {
        givenDefaultProperties();
        givenCpusProperty(null);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDescriptor();

        thenContainerCpusIsEmpty();
    }

    @Test
    public void testCpusOption() {
        givenDefaultProperties();
        givenCpusProperty(1.78F);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDescriptor();

        thenContainerCpusIsNotEmpty();
        thenContainerCpusIs(1.78F);
    }

    @Test
    public void testGpusOptionEmpty() {
        givenDefaultProperties();
        givenGpusProperty(null);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDescriptor();

        thenContainerGpusIsEmpty();
    }

    @Test
    public void testCpusOptionWithNumber() {
        givenDefaultProperties();
        givenGpusProperty("2");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDescriptor();

        thenContainerGpusIsNotEmpty();
        thenContainerGpusIs("2");
    }

    @Test
    public void testCpusOptionAll() {
        givenDefaultProperties();
        givenGpusProperty("all");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDescriptor();

        thenContainerGpusIsNotEmpty();
        thenContainerGpusIs("all");
    }

    @Test
    public void testRuntimeOptionEmpty() {
        givenDefaultProperties();
        givenRuntimeProperty(null);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDescriptor();

        thenContainerRuntimeIsEmpty();
    }

    @Test
    public void testRuntimeOptionIsSet() {
        givenDefaultProperties();
        givenRuntimeProperty("coolRuntime");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDescriptor();

        thenContainerRuntimeIsNotEmpty();
        thenContainerRuntimeIs("coolRuntime");
    }

    @Test
    public void testEnforcementDigest() {
        givenDefaultProperties();
        givenEnforcementDigestProperty("sha256:test");
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDescriptor();

        thenEnforcementDigestIsNotEmpty();
        thenEnforcementDigestIs("sha256:test");
    }

    private void testMemoryOption(String stringValue, Long longValue) {
        givenDefaultProperties();
        givenMemoryProperty(stringValue);
        givenConfigurableGenericDockerServiceOptions();

        whenGetContainerDescriptor();

        thenContainerMemoryIsNotEmpty();
        thenContainerMemoryIs(longValue);
    }

    private void givenNullProperties() {
        this.properties = null;
    }

    private void givenEmptyNewProperties() {
        this.newProperties = new HashMap<>();
    }

    private void givenDefaultProperties() {
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
        this.properties.put(REGISTRY_URL, DEFAULT_REGISTRY_URL);
        this.properties.put(REGISTRY_USERNAME, DEFAULT_REGISTRY_USERNAME);
        this.properties.put(REGISTRY_PASSWORD, DEFAULT_REGISTRY_PASSWORD);
        this.properties.put(IMAGES_DOWNLOAD_TIMEOUT, DEFAULT_IMAGES_DOWNLOAD_TIMEOUT);
        this.properties.put(CONTAINER_NETWORKING_MODE, DEFAULT_CONTAINER_NETWORKING_MODE);
        this.properties.put(CONTAINER_ENTRY_POINT, DEFAULT_CONTAINER_ENTRY_POINT);
        this.properties.put(CONTAINER_MEMORY, DEFAULT_CONTAINER_MEMORY);
        this.properties.put(CONTAINER_CPUS, DEFAULT_CONTAINER_CPUS);
        this.properties.put(CONTAINER_GPUS, DEFAULT_CONTAINER_GPUS);
        this.properties.put(CONTAINER_RUNTIME, DEFAULT_CONTAINER_RUNTIME);
        this.properties.put(ENFORCEMENT_DIGEST, DEFAULT_ENFORCEMENT_DIGEST);
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
        this.newProperties.put(CONTAINER_LOGGING_TYPE, "JOURNALD");
        this.newProperties.put(CONTAINER_LOGGER_PARAMETERS, "label=true");

        this.newProperties.put(REGISTRY_URL, "https://something");
        this.newProperties.put(REGISTRY_USERNAME, "test");
        this.newProperties.put(REGISTRY_PASSWORD, "test");
        this.newProperties.put(IMAGES_DOWNLOAD_TIMEOUT, 100);
        this.newProperties.put(CONTAINER_NETWORKING_MODE, "none");
        this.newProperties.put(CONTAINER_ENTRY_POINT, "./test.py,-v,-m,--human-readable,,,");
        this.newProperties.put(CONTAINER_MEMORY, "100m");
        this.newProperties.put(CONTAINER_CPUS, "1.5");
        this.newProperties.put(CONTAINER_RUNTIME, "myRuntime");
        this.newProperties.put(ENFORCEMENT_DIGEST, "");
    }

    private void givenMemoryProperty(String memory) {
        if (this.properties != null) {
            this.properties.put(CONTAINER_MEMORY, memory);
        }
    }

    private void givenCpusProperty(Float cpus) {
        if (this.properties != null) {
            this.properties.put(CONTAINER_CPUS, cpus);
        }
    }

    private void givenGpusProperty(String gpus) {
        if (this.properties != null) {
            this.properties.put(CONTAINER_GPUS, gpus);
        }
    }

    private void givenRuntimeProperty(String runtime) {
        if (this.properties != null) {
            this.properties.put(CONTAINER_RUNTIME, runtime);
        }
    }

    private void givenEnforcementDigestProperty(String digest) {
        if (this.properties != null) {
            this.properties.put(ENFORCEMENT_DIGEST, digest);
        }
    }

    private void givenConfigurableGenericDockerServiceOptions() {
        this.cgdso = new ContainerInstanceOptions(this.properties);
    }

    private void givenDiffrentConfigurableGenericDockerServiceOptions() {
        this.cgdso = new ContainerInstanceOptions(this.newProperties);
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

    private void givenPrivilegedMode(boolean value) {
        this.properties.put(CONTAINER_PRIVILEGED, value);
    }

    private void givenImageDownloadRetries(int value) {
        this.properties.put(CONTAINER_IMAGE_DOWNLOAD_RETRIES, value);
    }

    private void givenImageDownloadRetryInterval(int value) {
        this.properties.put(CONTAINER_IMAGE_DOWNLOAD_RETRY_INTERVAL, value);
    }

    private void givenContainerVolume(String source, String dest) {
        this.properties.put(CONTAINER_VOLUME, source + ":" + dest);
    }

    private void givenContainerDevice(String value) {
        this.properties.put(CONTAINER_DEVICE, value);
    }

    private void givenRegistryURL(String value) {
        this.properties.put(REGISTRY_URL, value);
    }

    private void givenRegistryUsername(String username) {
        this.properties.put(REGISTRY_USERNAME, username);
    }

    private void givenRegistryPassword(String password) {
        this.properties.put(REGISTRY_PASSWORD, password);
    }

    private void givenImageDownloadTimeout(int value) {
        this.properties.put(IMAGES_DOWNLOAD_TIMEOUT, value);
    }

    private void givenPortsConfiguration(String portProperty, String Ports) {
        this.properties.put(portProperty, Ports);
    }

    private void whenConfigurableGenericDockerServiceOptionsCreated() {
        this.cgdso = new ContainerInstanceOptions(this.properties);
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

    private void whenGetPrivilegedMode() {
        this.privilegedMode = this.cgdso.getPrivilegedMode();
    }

    private void whenGetImageDownloadRetries() {
        this.imageDownloadRetries = this.cgdso.getMaxDownloadRetries();
    }

    private void whenGetIsUnlimitedRetries() {
        this.unlimitedRetries = this.cgdso.isUnlimitedRetries();
    }

    private void whenGetImageDownloadRetryInterval() {
        this.imageDownloadRetryInterval = this.cgdso.getRetryInterval();
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

    private void whenGetRegistryURL() {
        PasswordRegistryCredentials registryCredentials = (PasswordRegistryCredentials) this.cgdso
                .getRegistryCredentials().get();
        this.registryURL = registryCredentials.getUrl();
    }

    private void whenGetRegistryUsername() {
        PasswordRegistryCredentials registryCredentials = (PasswordRegistryCredentials) this.cgdso
                .getRegistryCredentials().get();
        this.registryUsername = registryCredentials.getUsername();
    }

    private void whenGetRegistryPassword() {
        PasswordRegistryCredentials registryCredentials = (PasswordRegistryCredentials) this.cgdso
                .getRegistryCredentials().get();
        this.registryPassword = new String(registryCredentials.getPassword().getPassword());
    }

    private void whenGetImageDownloadTimeout() {
        this.imageDownloadTimeout = this.cgdso.getImageDownloadTimeout();
    }

    private void whenGetEquals(ContainerInstanceOptions options) {
        this.equals = this.cgdso.equals(options);
    }

    private void whenGetHashCode(ContainerInstanceOptions options) {
        this.hashCode = options.hashCode();
    }

    private void whenGetContainerDescriptor() {
        this.containerDescriptor = this.cgdso.getContainerConfiguration();
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

    private void thenPrivilegedMode(boolean expectedValue) {
        assertEquals(expectedValue, this.privilegedMode);
    }

    private void thenImageDownloadRetries(int expectedValue) {
        assertEquals(expectedValue, this.imageDownloadRetries);
    }

    private void thenRegistryURL(String expectedValue) {
        assertEquals(expectedValue, this.registryURL.get());
    }

    private void thenRegistryUsername(String expectedValue) {
        assertEquals(expectedValue, this.registryUsername);
    }

    private void thenRegistryPassword(String expectedValue) {
        assertEquals(expectedValue, this.registryPassword);
    }

    private void thenImageDownloadTimeout(int expectedValue) {
        assertEquals(expectedValue, this.imageDownloadTimeout);
    }

    private void thenImageDownloadRetryInterval(int expectedValue) {
        assertEquals(expectedValue, this.imageDownloadRetryInterval);
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

    private void thenPortResult(List<Integer> portResult) {
        assertEquals(portResult, this.portsAvailable);
    }

    private void thenIsEqual() {
        assertTrue(this.equals);
    }

    private void thenIsNotEqual() {
        assertFalse(this.equals);
    }

    private void thenIsTrue(boolean expectedValue) {
        assertTrue(expectedValue);
    }

    private void thenIsFalse(boolean expectedValue) {
        assertFalse(expectedValue);
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

    private void thenCheckIfExtraCommasAreIgnored() {
        assertEquals(Arrays.asList("./test.py", "-v", "-m", "--human-readable"), this.cgdso.getEntryPoint());
    }

    private void thenContainerMemoryIsEmpty() {
        assertFalse(this.containerDescriptor.getMemory().isPresent());
    }

    private void thenContainerMemoryIsNotEmpty() {
        assertTrue(this.containerDescriptor.getMemory().isPresent());
    }

    private void thenContainerMemoryIs(Long value) {
        assertEquals(this.containerDescriptor.getMemory().get(), value);
    }

    private void thenContainerCpusIsEmpty() {
        assertFalse(this.containerDescriptor.getCpus().isPresent());
    }

    private void thenContainerCpusIsNotEmpty() {
        assertTrue(this.containerDescriptor.getCpus().isPresent());
    }

    private void thenContainerCpusIs(Float value) {
        assertEquals(this.containerDescriptor.getCpus().get(), value);
    }

    private void thenContainerGpusIsEmpty() {
        assertFalse(this.containerDescriptor.getGpus().isPresent());
    }

    private void thenContainerGpusIsNotEmpty() {
        assertTrue(this.containerDescriptor.getGpus().isPresent());
    }

    private void thenContainerGpusIs(String value) {
        assertEquals(this.containerDescriptor.getGpus().get(), value);
    }

    private void thenContainerRuntimeIsEmpty() {
        assertFalse(this.containerDescriptor.getRuntime().isPresent());
    }

    private void thenContainerRuntimeIsNotEmpty() {
        assertTrue(this.containerDescriptor.getRuntime().isPresent());
    }

    private void thenContainerRuntimeIs(String value) {
        assertEquals(this.containerDescriptor.getRuntime().get(), value);
    }

    private void thenEnforcementDigestIsNotEmpty() {
        assertTrue(this.containerDescriptor.getEnforcementDigest().isPresent());
    }

    private void thenEnforcementDigestIs(String value) {
        assertEquals(this.containerDescriptor.getEnforcementDigest().get(), value);
    }
}
