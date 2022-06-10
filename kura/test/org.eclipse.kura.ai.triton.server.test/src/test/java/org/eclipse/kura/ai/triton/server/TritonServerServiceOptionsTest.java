package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashMap;
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

    @Test
    public void equalsMethodWorksWithSameOptions() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("timeout", null);
        givenServiceOptionsBuiltWith(this.properties);

        whenEqualsIsCalledWith(this.options, this.options);

        thenEqualsMethodShouldReturn(true);
    }

    @Test
    public void equalsMethodWorksWithDifferentType() {
        givenPropertyWith("server.address", "localhost");
        givenPropertyWith("server.ports", new Integer[] { 4000, 4001, 4002 });
        givenPropertyWith("enable.local", Boolean.FALSE);
        givenPropertyWith("timeout", null);
        givenServiceOptionsBuiltWith(this.properties);

        whenEqualsIsCalledWith(this.options, null);

        thenEqualsMethodShouldReturn(false);
    }

    @Test
    public void equalsMethodWorksWithOptionsBuiltWithSameProperties() {
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
    public void equalsMethodWorksWithOptionsBuiltWithDifferentProperties() {
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
    public void hashCodeMethodWorksWithOptionsBuiltWithSameProperties() {
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
    public void hashCodeMethodWorksWithOptionsBuiltWithDifferentProperties() {
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

    private void thenEqualsMethodShouldReturn(boolean value) {
        assertEquals(value, this.equalsResult);
    }

    private void thenHashCodesShouldMatch() {
        assertEquals(this.hashCode, this.otherHashCode);
    }

    private void thenHashCodesShouldNotMatch() {
        assertNotEquals(this.hashCode, this.otherHashCode);
    }
}
