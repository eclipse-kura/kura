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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.eclipse.kura.event.publisher.EventPublisher;
import org.eclipse.kura.event.publisher.EventPublisherConstants;
import org.eclipse.kura.event.publisher.helper.CloudEndpointServiceHelper;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventPublisherTest {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisherTest.class);

    private static EventPublisher eventPublisher;
    private static CloudEndpointServiceHelper endpoint;

    private static final String EVENT_PUBLISHER_FACTORY_PID = "org.eclipse.kura.event.publisher.EventPublisher";
    private static final String EVENT_PUBLISHER_PID = "eventPubTest";

    private Exception occurredException;
    private KuraMessage message;

    /*
     * Service tracking
     */

    @BeforeClass
    public static void setup()
            throws InterruptedException, ExecutionException, TimeoutException, KuraException, NoSuchFieldException,
            SecurityException, IllegalArgumentException, IllegalAccessException {
        ConfigurationService configurationService = ServiceUtil
                .trackService(ConfigurationService.class, Optional.empty()).get(30,
                TimeUnit.SECONDS);

        configurationService.createFactoryConfiguration(EVENT_PUBLISHER_FACTORY_PID, EVENT_PUBLISHER_PID,
                new HashMap<>(), false);

        eventPublisher = (EventPublisher) ServiceUtil
                .trackService(CloudPublisher.class,
                        Optional.of("(" + ConfigurationService.KURA_SERVICE_PID + "=" + EVENT_PUBLISHER_PID + ")"))
                .get(30, TimeUnit.SECONDS);

        endpoint = mock(CloudEndpointServiceHelper.class);
        
        // substitute the field with a mock to verify interactions
        Field helperField = eventPublisher.getClass().getDeclaredField("cloudHelper");
        helperField.setAccessible(true);
        helperField.set(eventPublisher, endpoint);

        logger.info("Dependencies satisfied.");
    }

    /*
     * Scenarios
     */

    @Test
    public void nullMessageShouldThrowException() throws InvalidSyntaxException {
        whenPublish(null);
        
        thenIllegalArgumentException();
    }

    @Test
    public void shouldPublishMessageCorrectly() throws KuraException {
        givenKuraMessage("test-body");

        whenPublish(this.message);

        thenNoExceptionsOccurred();
        thenMessageIsCorrectlyPublished("$EVT/#account-name/#client-id/EVENT_TOPIC", "test-body");
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenKuraMessage(String bodyContent) {
        KuraPayload payload = new KuraPayload();
        payload.setBody(bodyContent.getBytes());
        this.message = new KuraMessage(payload);
    }

    /*
     * When
     */

    private void whenPublish(KuraMessage message) {
        try {
            eventPublisher.publish(message);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenNoExceptionsOccurred() {
        assertNull(this.occurredException);
    }

    private void thenIllegalArgumentException() {
        assertTrue(this.occurredException instanceof IllegalArgumentException);
    }

    private void thenMessageIsCorrectlyPublished(String expectedFullTopic, String expectedBody) throws KuraException {
        ArgumentCaptor<KuraMessage> argument = ArgumentCaptor.forClass(KuraMessage.class);

        verify(endpoint, timeout(5000)).publish(argument.capture());
        
        String fullTopic = (String) argument.getValue().getProperties().get(EventPublisherConstants.FULL_TOPIC);
        boolean control = (Boolean) argument.getValue().getProperties().get(EventPublisherConstants.CONTROL);
        String body = new String(argument.getValue().getPayload().getBody());
        
        assertEquals(expectedFullTopic, fullTopic);
        assertTrue(control);
        assertEquals(expectedBody, body);
    }

    /*
     * Utilities
     */

    @Before
    public void cleanup() {
        this.occurredException = null;
    }

}
