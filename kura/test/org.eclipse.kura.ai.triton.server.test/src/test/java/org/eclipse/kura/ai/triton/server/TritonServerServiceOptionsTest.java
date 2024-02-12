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
 ******************************************************************************/

package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TritonServerServiceOptionsTest {

    private Map<String, Object> properties = new HashMap<>();
    private TritonServerServiceOptions options = new TritonServerServiceOptions(properties);
    private TritonServerServiceOptions otherOptions = new TritonServerServiceOptions(properties);

    private boolean equalsResult = false;
    private int hashCode;
    private int otherHashCode;

    @Test
    public void portGettersShouldWork() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenServiceOptionsBuiltWith(properties);

        thenHttpPortIsEqualTo(4000);
        thenGrpcPortIsEqualTo(4001);
        thenMetricsPortIsEqualTo(4002);
    }

    @Test
    public void portGettersShouldWorkWithNullProperty() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", null);
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenServiceOptionsBuiltWith(properties);

        thenHttpPortIsEqualTo(5000);
        thenGrpcPortIsEqualTo(5001);
        thenMetricsPortIsEqualTo(5002);
    }

    @Test
    public void isLocalGetterShouldWorkWithLocal() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.TRUE);
        givenServiceOptionsBuiltWith(properties);

        thenLocalConfigIsEqualTo(true);
    }

    @Test
    public void isLocalGetterShouldWorkWithRemote() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenServiceOptionsBuiltWith(properties);

        thenLocalConfigIsEqualTo(false);
    }

    @Test
    public void isLocalGetterShouldWorkWithNullProperty() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", null);
        givenServiceOptionsBuiltWith(properties);

        thenLocalConfigIsEqualTo(false);
    }

    @Test
    public void timeoutGettersShouldWork() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("timeout", 5);
        givenServiceOptionsBuiltWith(properties);

        thenTimeoutIsEqualTo(5);
        thenRetryIntervalIsEqualTo(500);
        thenNRetriesIsEqualTo(10);
    }

    @Test
    public void timeoutGettersShouldWorkWithNullProperty() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("timeout", null);
        givenServiceOptionsBuiltWith(properties);

        thenTimeoutIsEqualTo(3);
        thenRetryIntervalIsEqualTo(500);
        thenNRetriesIsEqualTo(6);
    }

    @Test
    public void equalsMethodShouldWorkWithSameOptions() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("timeout", null);
        givenServiceOptionsBuiltWith(this.properties);

        whenEqualsIsCalledWith(this.options, this.options);

        thenEqualsMethodShouldReturn(true);
    }

    @Test
    public void equalsMethodShouldWorkWithNullArgument() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("timeout", null);
        givenServiceOptionsBuiltWith(this.properties);

        whenEqualsIsCalledWith(this.options, null);

        thenEqualsMethodShouldReturn(false);
    }

    @Test
    public void equalsMethodShouldWorkWithOptionsBuiltWithSameProperties() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("timeout", null);
        givenServiceOptionsBuiltWith(this.properties);
        givenOtherServiceOptionsBuiltWith(this.properties);

        whenEqualsIsCalledWith(this.options, this.otherOptions);

        thenEqualsMethodShouldReturn(true);
    }

    @Test
    public void equalsMethodShouldWorkWithOptionsBuiltWithDifferentProperties() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("timeout", null);
        givenServiceOptionsBuiltWith(this.properties);

        givenPropertyWith("server.address", "192.168.1.66");
        givenPropertyWith("server.ports", new Integer[] { 5000, 5001, 5002 });
        givenPropertyWith("enable.local", Boolean.TRUE);
        givenPropertyWith("timeout", 60);
        givenOtherServiceOptionsBuiltWith(this.properties);

        whenEqualsIsCalledWith(this.options, this.otherOptions);

        thenEqualsMethodShouldReturn(false);
    }

    @Test
    public void hashCodeMethodShouldWorkWithOptionsBuiltWithSameProperties() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("timeout", null);
        givenServiceOptionsBuiltWith(this.properties);
        givenOtherServiceOptionsBuiltWith(this.properties);

        whenHashCodeIsCalledWith(this.options);
        whenOtherHashCodeIsCalledWith(this.otherOptions);

        thenHashCodesShouldMatch();
    }

    @Test
    public void hashCodeMethodShouldWorkWithOptionsBuiltWithDifferentProperties() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("timeout", null);
        givenServiceOptionsBuiltWith(this.properties);

        givenPropertyWith("server.address", "192.168.1.66");
        givenPropertyWith("server.ports", new Integer[] { 5000, 5001, 5002 });
        givenPropertyWith("enable.local", Boolean.TRUE);
        givenPropertyWith("timeout", 60);
        givenOtherServiceOptionsBuiltWith(this.properties);

        whenHashCodeIsCalledWith(this.options);
        whenOtherHashCodeIsCalledWith(this.otherOptions);

        thenHashCodesShouldNotMatch();
    }

    @Test
    public void shouldReturnInputGrpcMaxMessageSize() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("grpc.max.size", 2000);
        givenServiceOptionsBuiltWith(this.properties);

        thenGrpcMaxMessageSizeIsEqualTo(2000);
    }

    @Test
    public void shouldReturnDefaultGrpcMaxMessageSize() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("grpc.max.size", null);
        givenServiceOptionsBuiltWith(this.properties);

        thenGrpcMaxMessageSizeIsEqualTo(4194304);
    }

    @Test
    public void shouldReturnNotPresentForOptionalParametersIfNotSet() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("container.cpus", null);
        givenServiceOptionsBuiltWith(properties);

        thenContainerMemoryIsPresent(false);
        thenContainerCpusIsPresent(false);
        thenContainerGpusIsPresent(false);
    }

    @Test
    public void containerCpusPropertyShouldWork() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("container.cpus", 1.78F);
        givenServiceOptionsBuiltWith(properties);

        thenContainerCpusIsPresent(true);
        thenContainerCpusIsEqualTo(1.78F);
    }

    @Test
    public void containerGpusPropertyShouldWork() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("container.gpus", "2");
        givenServiceOptionsBuiltWith(properties);

        thenContainerGpusIsPresent(true);
        thenContainerGpusIsEqualTo("2");
    }

    @Test
    public void containerGpusPropertyShouldWorkWithAll() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("container.gpus", "all");
        givenServiceOptionsBuiltWith(properties);

        thenContainerGpusIsPresent(true);
        thenContainerGpusIsEqualTo("all");
    }

    @Test
    public void containerMemoryPropertyShouldWork() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("container.memory", "1234");
        givenServiceOptionsBuiltWith(properties);

        thenContainerMemoryIsPresent(true);
        thenContainerMemoryIsEqualTo(1234L);
    }

    @Test
    public void containerMemoryPropertyShouldWorkWithByteSuffix() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("container.memory", "12345b");
        givenServiceOptionsBuiltWith(properties);

        thenContainerMemoryIsPresent(true);
        thenContainerMemoryIsEqualTo(12345L);
    }

    @Test
    public void containerMemoryPropertyShouldWorkWithKiloSuffix() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("container.memory", "1111k");
        givenServiceOptionsBuiltWith(properties);

        thenContainerMemoryIsPresent(true);
        thenContainerMemoryIsEqualTo(1137664L);
    }

    @Test
    public void containerMemoryPropertyShouldWorkWithMegaSuffix() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("container.memory", "2222m");
        givenServiceOptionsBuiltWith(properties);

        thenContainerMemoryIsPresent(true);
        thenContainerMemoryIsEqualTo(2329935872L);
    }

    @Test
    public void containerMemoryPropertyShouldWorkWithGigaSuffix() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("container.memory", "7g");
        givenServiceOptionsBuiltWith(properties);

        thenContainerMemoryIsPresent(true);
        thenContainerMemoryIsEqualTo(7516192768L);
    }

    @Test
    public void containerRuntimePropertyShouldWork() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("container.runtime", "myCoolRuntime");
        givenServiceOptionsBuiltWith(properties);

        thenContainerRuntimeIsPresent(true);
        thenContainerRuntimeIsEqualTo("myCoolRuntime");
    }

    @Test
    public void containerRuntimePropertyShouldNotBePresent() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenServiceOptionsBuiltWith(properties);

        thenContainerRuntimeIsPresent(false);
    }

    @Test
    public void containerDevicesPropertyShouldWork() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("devices", "/dev/tty1,/dev/video0");
        givenServiceOptionsBuiltWith(properties);

        thenDeviceListIsFilled(true);
        thenDeviceListContains(Arrays.asList(new String[] { "/dev/tty1", "/dev/video0" }));
    }

    @Test
    public void containerDevicesPropertyShouldBeEmpty() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenServiceOptionsBuiltWith(properties);

        thenDeviceListIsFilled(false);
    }

    /*
     * Given
     */
    private void givenPropertyWith(String name, Object value) {
        this.properties.put(name, value);
    }

    private void givenServiceOptionsBuiltWith(Map<String, Object> properties) {
        this.options = new TritonServerServiceOptions(properties);
    }

    private void givenOtherServiceOptionsBuiltWith(Map<String, Object> properties) {
        this.otherOptions = new TritonServerServiceOptions(properties);
    }

    /*
     * When
     */
    private void whenEqualsIsCalledWith(TritonServerServiceOptions lhs, TritonServerServiceOptions rhs) {
        this.equalsResult = lhs.equals(rhs);
    }

    private void whenHashCodeIsCalledWith(TritonServerServiceOptions options) {
        this.hashCode = options.hashCode();
    }

    private void whenOtherHashCodeIsCalledWith(TritonServerServiceOptions options) {
        this.otherHashCode = options.hashCode();
    }

    /*
     * Then
     */
    private void thenTimeoutIsEqualTo(int value) {
        assertEquals(value, this.options.getTimeout());
    }

    private void thenRetryIntervalIsEqualTo(int value) {
        assertEquals(value, this.options.getRetryInterval());
    }

    private void thenNRetriesIsEqualTo(int value) {
        assertEquals(value, this.options.getNRetries());
    }

    private void thenHttpPortIsEqualTo(int value) {
        assertEquals(value, this.options.getHttpPort());
    }

    private void thenGrpcPortIsEqualTo(int value) {
        assertEquals(value, this.options.getGrpcPort());
    }

    private void thenMetricsPortIsEqualTo(int value) {
        assertEquals(value, this.options.getMetricsPort());
    }

    private void thenLocalConfigIsEqualTo(boolean value) {
        assertEquals(value, this.options.isLocalEnabled());
    }

    private void thenGrpcMaxMessageSizeIsEqualTo(int expectedValue) {
        assertEquals(expectedValue, this.options.getGrpcMaxMessageSize());
    }

    private void thenEqualsMethodShouldReturn(boolean value) {
        assertEquals(value, this.equalsResult);
    }

    private void thenHashCodesShouldMatch() {
        assertEquals(this.hashCode, this.otherHashCode);
    }

    private void thenHashCodesShouldNotMatch() {
        assertNotEquals(this.hashCode, this.otherHashCode);
    }

    private void thenContainerMemoryIsPresent(boolean expectedResult) {
        assertEquals(expectedResult, this.options.getContainerMemory().isPresent());
    }

    private void thenContainerMemoryIsEqualTo(Long expectedResult) {
        assertEquals(expectedResult, this.options.getContainerMemory().get());
    }

    private void thenContainerCpusIsPresent(boolean expectedResult) {
        assertEquals(expectedResult, this.options.getContainerCpus().isPresent());
    }

    private void thenContainerCpusIsEqualTo(Float expectedResult) {
        assertEquals(expectedResult, this.options.getContainerCpus().get());
    }

    private void thenContainerGpusIsPresent(boolean expectedResult) {
        assertEquals(expectedResult, this.options.getContainerGpus().isPresent());
    }

    private void thenContainerGpusIsEqualTo(String expectedResult) {
        assertEquals(expectedResult, this.options.getContainerGpus().get());
    }

    private void thenContainerRuntimeIsPresent(boolean expectedResult) {
        assertEquals(expectedResult, this.options.getContainerRuntime().isPresent());
    }

    private void thenContainerRuntimeIsEqualTo(String expectedResult) {
        assertEquals(expectedResult, this.options.getContainerRuntime().get());
    }

    private void thenDeviceListIsFilled(boolean expectedResult) {
        assertEquals(expectedResult, !this.options.getDevices().isEmpty());
    }

    private void thenDeviceListContains(List<String> expectedResult) {
        assertEquals(expectedResult, this.options.getDevices());
    }

}
