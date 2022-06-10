package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TritonServerServiceOptionsTest {

    private Map<String, Object> properties = new HashMap<>();

    @Test
    public void portOptionsShouldWork() {
        // Given
        properties.put("server.address", "localhost");
        properties.put("server.ports", new Integer[] { 4000, 4001, 4002 });
        properties.put("enable.local", Boolean.FALSE);

        TritonServerServiceOptions options = new TritonServerServiceOptions(properties);

        // Then
        assertEquals(4000, options.getHttpPort());
        assertEquals(4001, options.getGrpcPort());
        assertEquals(4002, options.getMetricsPort());
    }

    @Test
    public void portOptionsShouldWorkWithNullPorts() {
        // Given
        properties.put("server.address", "localhost");
        properties.put("server.ports", null);
        properties.put("enable.local", Boolean.FALSE);

        TritonServerServiceOptions options = new TritonServerServiceOptions(properties);

        // Then
        assertEquals(5000, options.getHttpPort());
        assertEquals(5001, options.getGrpcPort());
        assertEquals(5002, options.getMetricsPort());
    }

    @Test
    public void localOptionsShouldWorkWithLocal() {
        // Given
        properties.put("server.address", "localhost");
        properties.put("server.ports", new Integer[] { 4000, 4001, 4002 });
        properties.put("enable.local", Boolean.TRUE);

        TritonServerServiceOptions options = new TritonServerServiceOptions(properties);

        // Then
        assertTrue(options.isLocalEnabled());
    }

    @Test
    public void localOptionsShouldWorkWithRemote() {
        // Given
        properties.put("server.address", "localhost");
        properties.put("server.ports", new Integer[] { 4000, 4001, 4002 });
        properties.put("enable.local", Boolean.FALSE);

        TritonServerServiceOptions options = new TritonServerServiceOptions(properties);

        // Then
        assertFalse(options.isLocalEnabled());
    }

    @Test
    public void localOptionsShouldWorkWithNull() {
        // Given
        properties.put("server.address", "localhost");
        properties.put("server.ports", new Integer[] { 4000, 4001, 4002 });
        properties.put("enable.local", null);

        TritonServerServiceOptions options = new TritonServerServiceOptions(properties);

        // Then
        assertFalse(options.isLocalEnabled());
    }

    @Test
    public void timeoutOptionsShouldWork() {
        // Given
        properties.put("server.address", "localhost");
        properties.put("server.ports", new Integer[] { 4000, 4001, 4002 });
        properties.put("enable.local", Boolean.FALSE);
        properties.put("timeout", 5);

        TritonServerServiceOptions options = new TritonServerServiceOptions(properties);

        // Then
        assertEquals(5, options.getTimeout());
        assertEquals(500, options.getRetryInterval());
        assertEquals(10, options.getNRetries());
    }

    @Test
    public void timeoutOptionsShouldWorkWithNullTimeout() {
        // Given
        properties.put("server.address", "localhost");
        properties.put("server.ports", new Integer[] { 4000, 4001, 4002 });
        properties.put("enable.local", Boolean.FALSE);
        properties.put("timeout", null);

        TritonServerServiceOptions options = new TritonServerServiceOptions(properties);

        // Then
        assertEquals(3, options.getTimeout());
        assertEquals(500, options.getRetryInterval());
        assertEquals(6, options.getNRetries());
    }
}
