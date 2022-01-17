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
package org.eclipse.kura.core.testutil.requesthandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Optional;

import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.requesthandler.Transport.Response;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public abstract class AbstractRequestHandlerTest {

    protected final Transport transport;
    protected Optional<Response> response = Optional.empty();

    protected AbstractRequestHandlerTest(final Transport transport) {
        this.transport = transport;
        this.transport.init();
    }

    protected void whenRequestIsPerformed(final MethodSpec method, final String resource) {
        this.response = Optional.of(this.transport.runRequest(resource, method));
    }

    protected void whenRequestIsPerformed(final MethodSpec method, final String resource, final String body) {
        this.response = Optional.of(this.transport.runRequest(resource, method, body));
    }

    protected void thenRequestSucceeds() {
        thenResponseCodeIs(200);
    }

    protected void thenResponseCodeIs(final int expectedResponseCode) {
        final Response currentResponse = expectResponse();

        if (currentResponse.status != expectedResponseCode) {
            fail("expected status: " + expectedResponseCode + " but was: " + currentResponse.status + " body: "
                    + currentResponse.body);
        }
    }

    protected void thenResponseBodyEqualsJson(final String value) {
        assertEquals(Json.parse(value), Json
                .parse(expectResponse().body.orElseThrow(() -> new IllegalStateException("expected response body"))));
    }

    protected void thenResponseBodyIsEmpty() {
        assertEquals(Optional.empty(), expectResponse().body);
    }

    protected Response expectResponse() {
        return response.orElseThrow(() -> new IllegalStateException("response not available"));
    }

    protected JsonObject expectJsonResponse() {
        return Json.parse(expectResponse().body.orElseThrow(() -> new IllegalStateException("response body is empty")))
                .asObject();
    }
}
