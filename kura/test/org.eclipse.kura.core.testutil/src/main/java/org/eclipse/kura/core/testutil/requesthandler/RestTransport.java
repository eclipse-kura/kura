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
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

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
    private Optional<SSLContext> sslContext = Optional.empty();
    private boolean isHostnameVerificationEnabled = false;
    private CookieManager cookieManager = new CookieManager();
    private final Map<String, String> headers = new HashMap<>();

    public RestTransport(final String servicePath) {
        this("http://localhost:8080/services/", servicePath);
    }

    public RestTransport(final String urlPrefix, final String servicePath) {
        this.baseURL = urlPrefix + servicePath;
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

            Thread.sleep(1000);

            initialized = true;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void waitPortOpen(final String host, final int port, final long timeout, final TimeUnit timeoutUnit)
            throws InterruptedException {
        final long now = System.nanoTime();
        int successCount = 0;

        while (System.nanoTime() - now < timeoutUnit.toNanos(timeout)) {
            try {
                new Socket(host, port).close();
                successCount++;
                if (successCount == 2) {
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

    public void setSslContext(final SSLContext sslContext) {
        this.sslContext = Optional.of(sslContext);
    }

    public void setHostnameVerificationEnabled(final boolean isHostnameVerificationEnabled) {
        this.isHostnameVerificationEnabled = isHostnameVerificationEnabled;
    }

    public void setHeader(final String key, final String value) {
        this.headers.put(key, value);
    }

    @Override
    public Response runRequest(String relativeUri, MethodSpec method) {
        return runRequest(relativeUri, method, null);
    }

    @Override
    public Response runRequest(String relativeUri, MethodSpec method, String requestBody) {
        return runRequest(this.baseURL, relativeUri, method, requestBody);
    }

    public Response runRequest(final String urlPrefix, final String relativeUri, final MethodSpec method,
            final String requestBody) {
        final HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) new URL(urlPrefix + relativeUri).openConnection();
            basicCredentials.ifPresent(c -> {
                final String encoded = ENCODER.encodeToString(c.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + encoded);
            });
            if (connection instanceof HttpsURLConnection && sslContext.isPresent()) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.get().getSocketFactory());
                if (!isHostnameVerificationEnabled) {
                    ((HttpsURLConnection) connection).setHostnameVerifier((h, s) -> true);
                }
            }

            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Connection", "close");
            setHeaders(connection);
            setCookies(urlPrefix, relativeUri, connection);

            connection.setRequestMethod(method.getRestMethod());

            if (requestBody != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                IOUtils.write(requestBody, connection.getOutputStream());
            }

            connection.connect();

            final int status = connection.getResponseCode();

            final String body = getBody(connection);
            storeCookies(urlPrefix, relativeUri, connection);

            connection.disconnect();

            return new Response(status, Optional.ofNullable(body).filter(b -> !b.isEmpty()));
        } catch (final Exception e) {
            throw new IllegalStateException("request failed", e);
        }
    }

    private void setHeaders(final HttpURLConnection connection) {
        for (final Entry<String, String> e : this.headers.entrySet()) {
            connection.setRequestProperty(e.getKey(), e.getValue());
        }
    }

    private void storeCookies(final String urlPrefix, final String relativeUri, final HttpURLConnection connection)
            throws IOException, URISyntaxException {
        this.cookieManager.put(new URI(urlPrefix + relativeUri), connection.getHeaderFields());
    }

    private void setCookies(final String urlPrefix, final String relativeUri, final HttpURLConnection connection)
            throws IOException, URISyntaxException {
        for (final Entry<String, List<String>> e : cookieManager.get(new URI(urlPrefix + relativeUri),
                connection.getRequestProperties()).entrySet()) {
            if (!e.getValue().isEmpty()) {
                connection.setRequestProperty(e.getKey(), e.getValue().stream().collect(Collectors.joining(",")));
            }
        }
    }

    public void setCookieManager(final CookieManager cookieManager) {
        this.cookieManager = cookieManager;
    }

    public CookieManager getCookieManager() {
        return cookieManager;
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
