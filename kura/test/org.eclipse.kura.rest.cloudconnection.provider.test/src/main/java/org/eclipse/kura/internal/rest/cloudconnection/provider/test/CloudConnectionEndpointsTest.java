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
package org.eclipse.kura.internal.rest.cloudconnection.provider.test;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CloudConnectionEndpointsTest extends AbstractRequestHandlerTest {

    private static final String CLOUD_ENDPOINT_INSTANCE_TEST = "org.eclipse.kura.cloud.CloudService-test";

    private static final String MQTT_APP_ID = "CC-V1";

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";
    private static final String METHOD_SPEC_DELETE = "DELETE";
    private static final String MQTT_METHOD_SPEC_DEL = "DEL";
    private static final String METHOD_SPEC_PUT = "PUT";
    private static final String REST_APP_ID = "identity/v1";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Transport> transports() {
        return Arrays.asList(new MqttTransport(MQTT_APP_ID), new RestTransport(REST_APP_ID));
    }

    public CloudConnectionEndpointsTest(Transport transport) {
        super(transport);

    }

    @BeforeClass
    public static void setup() throws InterruptedException, ExecutionException, TimeoutException, KuraException {
        final CloudConnectionFactory cloudConnectionFactory = ServiceUtil
                .trackService(CloudConnectionFactory.class, Optional.empty()).get(30, TimeUnit.SECONDS);

        cloudConnectionFactory.createConfiguration(CLOUD_ENDPOINT_INSTANCE_TEST);

        assertTrue("Unable to create the test CloudEndpoint",
                cloudConnectionFactory.getManagedCloudConnectionPids().contains(CLOUD_ENDPOINT_INSTANCE_TEST));
    }

}
