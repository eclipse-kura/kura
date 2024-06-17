/*******************************************************************************
 * Copyright (c) 2022, 2024 Eurotech and/or its affiliates and others
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.kura.core.testutil.requesthandler.Transport.MethodSpec;
import org.eclipse.kura.core.testutil.requesthandler.Transport.Response;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public abstract class AbstractRequestHandlerTest {

    protected final Transport transport;
    protected Optional<Response> response = Optional.empty();
    protected final TransportType transportType;
    protected Map<String, HttpCookie> cookieSnapshot = new HashMap<>();

    protected AbstractRequestHandlerTest(final Transport transport) {
        this.transport = transport;

        if (this.transport instanceof MqttTransport) {
            this.transportType = TransportType.MQTT;
        } else if (this.transport instanceof RestTransport) {
            this.transportType = TransportType.REST;
        } else
            throw new IllegalArgumentException("Transport type must be a REST transport or a MQTT transport");

        this.transport.init();
    }

    protected void givenSnapshotOfCurrentCookies() {
        final CookieManager cookieManager = expectCookieManager();

        this.cookieSnapshot = cookieManager.getCookieStore().getCookies().stream()
                .collect(Collectors.toMap(c -> c.getName(), c -> (HttpCookie) c.clone()));
    }

    protected void givenCookieInSnapshot(final String name) {
        assertNotNull("cookie " + name + " is not in captured snapshot", this.cookieSnapshot.get(name));
    }

    protected void whenRequestIsPerformed(final MethodSpec method, final String resource) {
        this.response = Optional.of(this.transport.runRequest(resource, method));
    }

    protected void whenRequestIsPerformed(final MethodSpec method, final String resource, final String body) {
        this.response = Optional.of(this.transport.runRequest(resource, method, body));
    }

    protected void whenCookieIsRestoredFromSnapshot(final String name) {
        final CookieManager cookieManager = expectCookieManager();
        final CookieStore cookieStore = cookieManager.getCookieStore();

        final HttpCookie cookie = Optional.ofNullable(this.cookieSnapshot.get(name))
                .orElseThrow(() -> new IllegalStateException("cookie " + name + " is not in the captured snapshot"));

        Optional<URI> targetURI = Optional.empty();

        for (final URI uri : cookieStore.getURIs()) {
            for (final HttpCookie storedCookie : cookieStore.get(uri)) {
                if (Objects.equals(storedCookie.getName(), name)) {
                    targetURI = Optional.of(uri);
                    break;
                }
            }
        }

        cookieStore.add(targetURI.orElseThrow(() -> new IllegalStateException("Cannot determine cookie URI")), cookie);
    }

    protected void thenCurrentCookieDiffersFromPreviousSnapshot(final String name) {
        final Optional<HttpCookie> cookieInSnapshot = Optional.ofNullable(this.cookieSnapshot.get(name));
        final Optional<HttpCookie> currentCookie = expectCookieManager().getCookieStore().getCookies().stream()
                .filter(c -> name.equals(c.getName())).findAny();

        assertNotEquals(cookieInSnapshot.map(HttpCookie::getValue), currentCookie.map(HttpCookie::getValue));
    }

    protected void thenRequestSucceeds() {
        final Response currentResponse = expectResponse();

        if ((currentResponse.getStatus() / 200) != 1) {
            fail("expected success status, but was: " + currentResponse.getStatus() + " body: "
                    + currentResponse.getBody());
        }
    }

    protected void thenResponseCodeIs(final int expectedResponseCode) {
        final Response currentResponse = expectResponse();

        if (currentResponse.getStatus() != expectedResponseCode) {
            fail("expected status: " + expectedResponseCode + " but was: " + currentResponse.getStatus() + " body: "
                    + currentResponse.getBody());
        }
    }

    protected void thenResponseBodyEqualsJson(final String value) {
        assertEquals(Json.parse(value), Json.parse(
                expectResponse().getBody().orElseThrow(() -> new IllegalStateException("expected response body"))));
    }

    protected void thenResponseBodyIsEmpty() {
        assertEquals(Optional.empty(), expectResponse().getBody());
    }

    protected void thenResponseBodyIsNotEmpty() {
        assertTrue(expectResponse().getBody().isPresent());
    }

    protected void thenResponseHasCookie(final String name) {
        final CookieManager cookieManager = expectCookieManager();

        assertTrue(cookieManager.getCookieStore().getCookies().stream().anyMatch(c -> c.getName().equals(name)));
    }

    protected void thenResponseDoesNotHaveCookie(final String name) {
        final CookieManager cookieManager = expectCookieManager();

        assertTrue(cookieManager.getCookieStore().getCookies().stream().noneMatch(c -> c.getName().equals(name)));
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
        return Json.parse(
                expectResponse().getBody().orElseThrow(() -> new IllegalStateException("response body is empty")))
                .asObject();
    }

    public TransportType getTransportType() {
        return this.transportType;
    }

}
