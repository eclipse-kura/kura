/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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

import java.util.Optional;

public interface Transport {

    public void init();

    public Response runRequest(final String resource, final MethodSpec method);

    public Response runRequest(final String resource, final MethodSpec method, final String body);

    public static class Response {

        public final int status;
        public final Optional<String> body;

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

        public final String restMethod;
        public final String requestHandlerMethod;

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
