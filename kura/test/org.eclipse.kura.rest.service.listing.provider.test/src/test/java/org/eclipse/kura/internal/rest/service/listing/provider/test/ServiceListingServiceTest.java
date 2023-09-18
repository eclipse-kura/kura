/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.service.listing.provider.test;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.internal.rest.service.listing.provider.RestServiceListingProvider;
import org.junit.Test;

public class ServiceListingServiceTest {

    private final RestServiceListingProvider service = new RestServiceListingProvider();
    private RequestHandlerRegistry requestHandlerRegistry;
    private Exception occurredException;

    /*
     * Scenarios
     */

    @Test
    public void shouldCatchExceptionsOnRequestHandlerBind() throws KuraException {
        givenFailingMockRequestHandlerRegistry();

        whenBindRequestHandlerRegistry();

        thenNoExceptionOccurred();
    }

    @Test
    public void shouldCatchExceptionsOnRequestHandlerUnbind() throws KuraException {
        givenFailingMockRequestHandlerRegistry();

        whenUnbindRequestHandlerRegistry();

        thenNoExceptionOccurred();
    }

    /*
     * Given
     */

    private void givenFailingMockRequestHandlerRegistry() throws KuraException {
        this.requestHandlerRegistry = mock(RequestHandlerRegistry.class);
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(this.requestHandlerRegistry)
                .registerRequestHandler(any(), any());
        doThrow(new KuraException(KuraErrorCode.BAD_REQUEST)).when(this.requestHandlerRegistry).unregister(any());
    }

    /*
     * When
     */

    private void whenBindRequestHandlerRegistry() {
        try {
            this.service.bindRequestHandlerRegistry(this.requestHandlerRegistry);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenUnbindRequestHandlerRegistry() {
        try {
            this.service.unbindRequestHandlerRegistry(this.requestHandlerRegistry);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenNoExceptionOccurred() {
        String errorMessage = "Empty message";
        if (Objects.nonNull(this.occurredException)) {
            StringWriter sw = new StringWriter();
            this.occurredException.printStackTrace(new PrintWriter(sw));

            errorMessage = String.format("No exception expected, \"%s\" found. Caused by: %s",
                    this.occurredException.getClass().getName(), sw.toString());
        }

        assertNull(errorMessage, this.occurredException);
    }

}
