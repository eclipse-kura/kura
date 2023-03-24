/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.testutil.requesthandler;

import java.io.IOException;
import java.io.InputStream;
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
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestTransport implements Transport {

    private static final Encoder ENCODER = Base64.getEncoder();
    private static final Logger logger = LoggerFactory.getLogger(RestTransport.class);

    private final String baseURL;

    private boolean initialized = false;

    private Optional<String> basicCredentials = Optional.of("admin:admin");

    public RestTransport(final String servicePath) {
        this.baseURL = "http://localhost:8080/services/" + servicePath;
    }

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
            connection = (HttpURLConnection) new URL(this.baseURL + relativeUri).openConnection();
            basicCredentials.ifPresent(c -> {
                final String encoded = ENCODER.encodeToString(c.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + encoded);
            });
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Connection", "close");
            connection.setRequestMethod(method.getRestMethod());

            if (requestBody != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                IOUtils.write(requestBody, connection.getOutputStream());
            }

            connection.connect();

            final int status = connection.getResponseCode();

            final String body = getBody(connection);

            connection.disconnect();

            return new Response(status, Optional.ofNullable(body).filter(b -> !b.isEmpty()));
        } catch (final Exception e) {
            throw new IllegalStateException("request failed", e);
        }
    }

    private String getBody(final HttpURLConnection connection) throws IOException {
        try (final InputStream in = ((connection.getResponseCode() / 200) == 1) ? connection.getInputStream()
                : connection.getErrorStream()) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }

    public void setBasicCredentials(final Optional<String> basicCredentials) {
        this.basicCredentials = basicCredentials;
    }

}
