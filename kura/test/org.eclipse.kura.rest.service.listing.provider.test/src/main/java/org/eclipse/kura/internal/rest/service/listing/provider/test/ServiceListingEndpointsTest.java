/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.service.listing.provider.test;

import java.util.Arrays;
import java.util.Collection;

import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.internal.rest.service.listing.provider.test.responses.ServiceListeningTestConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ServiceListingEndpointsTest extends AbstractRequestHandlerTest {

    private static final String MQTT_APP_ID = "SERLIST-V1";

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";
    private static final String REST_APP_ID = "serviceListing/v1";

    @Parameterized.Parameters
    public static Collection<Transport> transports() {
        return Arrays.asList(new RestTransport(REST_APP_ID), new MqttTransport(MQTT_APP_ID));
    }

    public ServiceListingEndpointsTest(Transport transport) {
        super(transport);
    }

    @Test
    public void shouldReturnListOfAllServices() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_GET), ServiceListeningTestConstants.GET_ENDPOINT);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.ALL_SERVICES_RESPONSE);
    }

    @Test
    public void shouldReturnListOfFilteredServices() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), ServiceListeningTestConstants.POST_ENDPOINT,
                ServiceListeningTestConstants.COMPLETE_POST_BODY);

        thenRequestSucceeds();
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.FILTERED_SERVICES_RESPONSE);
    }

    @Test
    public void shouldReturnErrorMessageWhenRequestBodyIsNull() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), ServiceListeningTestConstants.POST_ENDPOINT,
                ServiceListeningTestConstants.NULL_POST_BODY);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.NULL_BODY_RESPONSE);
    }

    @Test
    public void shouldReturnErrorMessageWhenRequestBodyIsEmpty() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), ServiceListeningTestConstants.POST_ENDPOINT,
                ServiceListeningTestConstants.EMPTY_POST_BODY);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.EMPTY_BODY_RESPONSE);
    }

    @Test
    public void shouldReturnErrorMessageWhenRequestBodyHasNullField() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), ServiceListeningTestConstants.POST_ENDPOINT,
                ServiceListeningTestConstants.NULL_FIELD_POST_BODY);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.NULL_FIELD_BODY_RESPONSE);
    }

    @Test
    public void shouldReturnErrorMessageWhenRequestBodyHasEmptyField() {

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), ServiceListeningTestConstants.POST_ENDPOINT,
                ServiceListeningTestConstants.EMPTY_FIELD_POST_BODY);

        thenResponseCodeIs(Status.BAD_REQUEST.getStatusCode());
        thenResponseBodyEqualsJson(ServiceListeningTestConstants.EMPTY_FIELD_BODY_RESPONSE);
    }

}
