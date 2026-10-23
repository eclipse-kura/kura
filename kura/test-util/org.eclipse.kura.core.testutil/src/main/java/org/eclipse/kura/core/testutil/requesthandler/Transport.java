/*******************************************************************************
 * Copyright (c) 2021, 2026 Eurotech and/or its affiliates and others
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Transport {

    public void init();

    public Response runRequest(final String resource, final MethodSpec method);

    public Response runRequest(final String resource, final MethodSpec method, final String body);

    public static class Response {

        private final int status;
        private final Optional<String> body;
        private final Map<String, List<String>> headers;

        public Response(int status, Optional<String> body) {
            this(status, body, Collections.emptyMap());
        }

        public Response(int status, Optional<String> body, Map<String, List<String>> headers) {
            this.status = status;
            this.body = body;
            this.headers = headers;
        }

        public int getStatus() {
            return status;
        }

        public Optional<String> getBody() {
            return body;
        }

        /**
         * Returns the values of a response header, matching its name case-insensitively.
         */
        public List<String> getHeader(final String name) {
            return this.headers.entrySet().stream() //
                    .filter(e -> name.equalsIgnoreCase(e.getKey())) //
                    .map(Map.Entry::getValue) //
                    .findFirst() //
                    .orElse(Collections.emptyList());
        }
    }

    public class MethodSpec {

        private final String restMethod;
        private final String requestHandlerMethod;

        public MethodSpec(final String method) {
            this.requestHandlerMethod = method;
            this.restMethod = method;

            if (this.requestHandlerMethod.equalsIgnoreCase("DELETE")) {
                throw new IllegalArgumentException(
                        "Method " + this.requestHandlerMethod + " is not allowed for RequestHandler");
            }
        }

        public MethodSpec(final String restMethod, final String requestHandlerMethod) {
            this.restMethod = restMethod;
            this.requestHandlerMethod = requestHandlerMethod;

            if (this.requestHandlerMethod.equalsIgnoreCase("DELETE")) {
                throw new IllegalArgumentException(
                        "Method " + this.requestHandlerMethod + " is not allowed for RequestHandler");
            }
        }

        public String getRestMethod() {
            return this.restMethod;
        }

        public String getRequestHandlerMethod() {
            return this.requestHandlerMethod;
        }
    }
}
