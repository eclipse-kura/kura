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

package org.eclipse.kura.event.publisher.test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.cloudconnection.CloudConnectionConstants;
import org.eclipse.kura.event.publisher.EventPublisherOptions;
import org.junit.Before;
import org.junit.Test;

public class EventPublisherOptionsTest {
    
    private EventPublisherOptions options;
    private Map<String, Object> properties = new HashMap<>();
    private Object returnedValue;

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnTopic() {
        givenProperty(EventPublisherOptions.TOPIC_PROP_NAME, "ex.topic");
        givenEventPublisherOptions();

        whenGetTopic();

        thenReturnedValueIs("ex.topic");
    }

    @Test
    public void shouldReturnTopicWithoutSeparators() {
        givenProperty(EventPublisherOptions.TOPIC_PROP_NAME, "/example.topic/");
        givenEventPublisherOptions();
        
        whenGetTopic();
        
        thenReturnedValueIs("example.topic");
    }

    @Test
    public void shouldReturnDefaultTopic() {
        givenEventPublisherOptions();

        whenGetTopic();

        thenReturnedValueIs(EventPublisherOptions.DEFAULT_TOPIC);
    }

    @Test
    public void shouldReturnTopicPrefix() {
        givenProperty(EventPublisherOptions.TOPIC_PREFIX_PROP_NAME, "top.prefix");
        givenEventPublisherOptions();

        whenGetTopicPrefix();

        thenReturnedValueIs(Optional.of("top.prefix"));
    }

    @Test
    public void shouldReturnDefaultTopicPrefix() {
        givenEventPublisherOptions();

        whenGetTopicPrefix();

        thenReturnedValueIs(Optional.empty());
    }

    @Test
    public void shouldNotReturnTopicPrefix() {
        givenProperty(EventPublisherOptions.TOPIC_PREFIX_PROP_NAME, "");
        givenEventPublisherOptions();

        whenGetTopicPrefix();

        thenReturnedValueIs(Optional.empty());
    }

    @Test
    public void shouldReturnTopicPrefixWithoutSeparators() {
        givenProperty(EventPublisherOptions.TOPIC_PREFIX_PROP_NAME, "/prefix/");
        givenEventPublisherOptions();

        whenGetTopicPrefix();

        thenReturnedValueIs(Optional.of("prefix"));
    }

    @Test
    public void shouldReturnEndpointPid() {
        givenProperty(CloudConnectionConstants.CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME.value(), "example.endpoint-1");
        givenEventPublisherOptions();

        whenGetCloudEndpointPid();

        thenReturnedValueIs("example.endpoint-1");
    }

    @Test
    public void shouldReturnDefaultEndpointPid() {
        givenEventPublisherOptions();

        whenGetCloudEndpointPid();

        thenReturnedValueIs(EventPublisherOptions.DEFAULT_ENDPOINT_PID);
    }

    @Test
    public void shouldReturnQos() {
        givenProperty(EventPublisherOptions.QOS_PROP_NAME, (int) 1);
        givenEventPublisherOptions();

        whenGetQos();

        thenReturnedValueIs((int) 1);
    }

    @Test
    public void shouldReturnDefaultQos() {
        givenEventPublisherOptions();

        whenGetQos();

        thenReturnedValueIs(EventPublisherOptions.DEFAULT_QOS);
    }

    @Test
    public void shouldReturnRetain() {
        givenProperty(EventPublisherOptions.RETAIN_PROP_NAME, true);
        givenEventPublisherOptions();

        whenIsRetain();

        thenReturnedValueIs(true);
    }

    @Test
    public void shouldReturnDefaultRetain() {
        givenEventPublisherOptions();

        whenIsRetain();

        thenReturnedValueIs(EventPublisherOptions.DEFAULT_RETAIN);
    }

    @Test
    public void shouldReturnPriority() {
        givenProperty(EventPublisherOptions.PRIORITY_PROP_NAME, 5);
        givenEventPublisherOptions();

        whenGetPriority();

        thenReturnedValueIs((int) 5);
    }

    @Test
    public void shouldReturnDefaultPriority() {
        givenEventPublisherOptions();

        whenGetPriority();

        thenReturnedValueIs(EventPublisherOptions.DEFAULT_PRIORITY);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    private void givenEventPublisherOptions() {
        this.options = new EventPublisherOptions(this.properties);
    }

    /*
     * When
     */

    private void whenGetTopicPrefix() {
        this.returnedValue = this.options.getTopicPrefix();
    }

    private void whenGetTopic() {
        this.returnedValue = this.options.getTopic();
    }

    private void whenGetPriority() {
        this.returnedValue = this.options.getPriority();
    }

    private void whenGetCloudEndpointPid() {
        this.returnedValue = this.options.getCloudEndpointPid();
    }

    private void whenIsRetain() {
        this.returnedValue = this.options.isRetain();
    }

    private void whenGetQos() {
        this.returnedValue = this.options.getQos();
    }

    /*
     * Then
     */

    @SuppressWarnings("unchecked")
    private <T> void thenReturnedValueIs(T expectedValue) {
        assertEquals(expectedValue, (T) this.returnedValue);
    }

    /*
     * Utilities
     */

    @Before
    public void cleanup() {
        this.properties.clear();
    }

}
