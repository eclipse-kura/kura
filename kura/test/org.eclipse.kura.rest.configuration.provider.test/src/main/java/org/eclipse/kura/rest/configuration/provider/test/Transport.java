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

import java.util.Optional;

public interface Transport {

    public void init();

    public Response runRequest(final String resource, final MethodSpec method);

    public Response runRequest(final String resource, final MethodSpec method, final String body);

    public static class Response {

        final int status;
        final Optional<String> body;

        public Response(int status, Optional<String> body) {
            this.status = status;
            this.body = body;
        }

        public int getStatus() {
            return status;
        }

        public Optional<String> getBody() {
            return body;
        }
    }

    public class MethodSpec {

        final String restMethod;
        final String requestHandlerMethod;

        public MethodSpec(final String method) {
            this.requestHandlerMethod = method;
            this.restMethod = method;
        }

        public MethodSpec(final String restMethod, final String requestHandlerMethod) {
            this.restMethod = restMethod;
            this.requestHandlerMethod = requestHandlerMethod;
        }

        public String getRestMethod() {
            return restMethod;
        }

        public String getRequestHandlerMethod() {
            return requestHandlerMethod;
        }
    }
}
