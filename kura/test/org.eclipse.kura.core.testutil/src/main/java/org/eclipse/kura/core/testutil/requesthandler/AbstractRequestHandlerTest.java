/*******************************************************************************
 * Copyright (c) 2022, 2023 Eurotech and/or its affiliates and others
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.CookieManager;
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
        final Response currentResponse = expectResponse();

        assertEquals(true, (currentResponse.getStatus() / 200) == 1);
    }

    protected void thenResponseCodeIs(final int expectedResponseCode) {
        final Response currentResponse = expectResponse();

        if (currentResponse.getStatus() != expectedResponseCode) {
            fail("expected status: " + expectedResponseCode + " but was: " + currentResponse.getStatus() + " body: "
                    + currentResponse.getBody());
        }
    }

    protected void thenResponseBodyEqualsJson(final String value) {
        assertEquals(Json.parse(value), Json
                .parse(expectResponse().getBody()
                        .orElseThrow(() -> new IllegalStateException("expected response body"))));
    }

    protected void thenResponseBodyIsEmpty() {
        assertEquals(Optional.empty(), expectResponse().getBody());
    }

    protected void thenResponseHasCookie(final String name) {
        final CookieManager cookieManager = expectCookieManager();

        assertTrue(cookieManager.getCookieStore().getCookies().stream()
                .anyMatch(c -> c.getName().equals(name)));
    }

    protected void thenResponseDoesNotHaveCookie(final String name) {
        final CookieManager cookieManager = expectCookieManager();

        assertTrue(cookieManager.getCookieStore().getCookies().stream()
                .noneMatch(c -> c.getName().equals(name)));
    }

    protected CookieManager expectCookieManager() {
        if (!(this.transport instanceof RestTransport)) {
            fail("cookies are available only with rest transport");
        }

        return ((RestTransport) this.transport).getCookieManager();
    }

    protected Response expectResponse() {
        return response.orElseThrow(() -> new IllegalStateException("response not available"));
    }

    protected JsonObject expectJsonResponse() {
        return Json
                .parse(expectResponse().getBody()
                        .orElseThrow(() -> new IllegalStateException("response body is empty")))
                .asObject();
    }
}
