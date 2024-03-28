/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.identity.provider.test;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.requesthandler.AbstractRequestHandlerTest;
import org.eclipse.kura.core.testutil.requesthandler.MqttTransport;
import org.eclipse.kura.core.testutil.requesthandler.RestTransport;
import org.eclipse.kura.core.testutil.requesthandler.Transport;
import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;

@RunWith(Parameterized.class)
public class IdentityV2EndpointsTest extends AbstractRequestHandlerTest {

    private static final String MQTT_APP_ID = "IDN-V2";
    private static final String REST_APP_ID = "identity/v2";

    private static final String METHOD_SPEC_GET = "GET";
    private static final String METHOD_SPEC_POST = "POST";
    private static final String METHOD_SPEC_DELETE = "DELETE";
    private static final String MQTT_METHOD_SPEC_DEL = "DEL";
    private static final String METHOD_SPEC_PUT = "PUT";

    private Gson gson = new Gson();

    private IdentityDTO identity;
    private String testUserName;

    @Parameterized.Parameters(name = "{0} - {1}")
    public static Collection<?> transports() {
        return Arrays.asList(new Object[][] { //
                { new MqttTransport(MQTT_APP_ID), "test-user-for-mqtt" }, //
                { new RestTransport(REST_APP_ID), "test-user-for-rest" } });
    }

    public IdentityV2EndpointsTest(Transport transport, String testUserName) {
        super(transport);
        this.testUserName = testUserName;
    }

    @Test
    public void shouldInvokeCreateIdentitySuccessfully() throws KuraException {
        givenIdentity(new IdentityDTO(this.testUserName));

        whenRequestIsPerformed(new MethodSpec(METHOD_SPEC_POST), "/identities", gson.toJson(this.identity));

        thenRequestSucceeds();
        thenResponseBodyIsEmpty();
    }

    private void givenIdentity(IdentityDTO identity) {
        this.identity = identity;

    }

}
