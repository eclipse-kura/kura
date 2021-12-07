/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.rest.configuration.provider.test;

import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestTransport implements Transport {

    private static final Encoder ENCODER = Base64.getEncoder();
    private static final Logger logger = LoggerFactory.getLogger(RestTransport.class);

    private static final String BASE_URL = "http://localhost:8080/services/configuration/v2";

    private boolean initialized = false;

    @Override
    public void init() {
        if (initialized) {
            return;
        }

        try {
            waitPortOpen("localhost", 8080, 3, TimeUnit.MINUTES);

            ServiceUtil
                    .trackService(ConfigurableComponent.class,
                            Optional.of("(kura.service.pid=org.eclipse.kura.internal.rest.provider.RestService)"))
                    .get(1, TimeUnit.MINUTES);

            ServiceUtil.trackService("org.eclipse.kura.internal.rest.configuration.ConfigurationRestService",
                    Optional.empty()).get(1, TimeUnit.MINUTES);

            Thread.sleep(5000);

            initialized = true;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitPortOpen(final String url, final int port, final long timeout, final TimeUnit timeoutUnit)
            throws InterruptedException {
        final long now = System.nanoTime();
        int successCount = 0;

        while (System.nanoTime() - now < timeoutUnit.toNanos(timeout)) {
            try {
                new Socket(url, port).close();
                successCount++;
                if (successCount == 10) {
                    return;
                }
                logger.info("port open");
            } catch (final Exception e) {
                logger.warn("failed to connect");
                successCount = 0;
            }
            Thread.sleep(1000);
        }

        throw new IllegalStateException("Port " + port + "not open");
    }

    @Override
    public Response runRequest(String relativeUri, MethodSpec method) {
        return runRequest(relativeUri, method, null);
    }

    @Override
    public Response runRequest(String relativeUri, MethodSpec method, String requestBody) {
        final HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) new URL(BASE_URL + relativeUri).openConnection();
            final String encoded = ENCODER.encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + encoded);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod(method.getRestMethod());

            if (requestBody != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                IOUtils.write(requestBody, connection.getOutputStream());
            }

            connection.connect();

            final int status = connection.getResponseCode();

            final String body = IOUtils.toString(
                    status == 200 ? connection.getInputStream() : connection.getErrorStream(), StandardCharsets.UTF_8);

            return new Response(status, body == null || body.isEmpty() ? Optional.empty() : Optional.of(body));
        } catch (final Exception e) {
            throw new IllegalStateException("request failed", e);
        }
    }

}
