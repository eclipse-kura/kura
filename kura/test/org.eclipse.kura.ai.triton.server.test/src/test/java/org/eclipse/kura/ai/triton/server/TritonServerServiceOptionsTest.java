package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TritonServerServiceOptionsTest {

    private Map<String, Object> properties = new HashMap<>();
    private TritonServerServiceOptions options = new TritonServerServiceOptions(properties);

    @Test
    public void portOptionsShouldWork() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenServiceOptionsBuiltWith(properties);

        thenHttpPortIsEqualTo(4000);
        thenGrpcPortIsEqualTo(4001);
        thenMetricsPortIsEqualTo(4002);
    }

    @Test
    public void portOptionsShouldWorkWithNullPorts() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", null);
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenServiceOptionsBuiltWith(properties);

        thenHttpPortIsEqualTo(5000);
        thenGrpcPortIsEqualTo(5001);
        thenMetricsPortIsEqualTo(5002);
    }

    @Test
    public void localOptionsShouldWorkWithLocal() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.TRUE);
        givenServiceOptionsBuiltWith(properties);

        thenLocalConfigIsEqualTo(true);
    }

    @Test
    public void localOptionsShouldWorkWithRemote() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenServiceOptionsBuiltWith(properties);

        thenLocalConfigIsEqualTo(false);
    }

    @Test
    public void localOptionsShouldWorkWithNull() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", null);
        givenServiceOptionsBuiltWith(properties);

        thenLocalConfigIsEqualTo(false);
    }

    @Test
    public void timeoutOptionsShouldWork() {
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
    public void timeoutOptionsShouldWorkWithNullTimeout() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("timeout", null);
        givenServiceOptionsBuiltWith(properties);

        thenTimeoutIsEqualTo(3);
        thenRetryIntervalIsEqualTo(500);
        thenNRetriesIsEqualTo(6);
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

    /*
     * When
     */
    // TODO

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
}
