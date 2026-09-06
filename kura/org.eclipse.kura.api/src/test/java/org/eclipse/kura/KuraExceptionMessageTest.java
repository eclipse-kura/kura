/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

public class KuraExceptionMessageTest {

    private String message;

    @Test
    public void shouldFormatTheMessageOfTheCodeWithItsArguments() {
        givenMessageOf(new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "missing pid"));

        thenMessageIs("Configuration Error: missing pid");
    }

    @Test
    public void shouldJoinTheArgumentsOfAnInternalError() {
        givenMessageOf(new KuraException(KuraErrorCode.INTERNAL_ERROR, "first", "second"));

        thenMessageIs("An internal error occurred.  - first - second");
    }

    @Test
    public void shouldFallBackToTheGenericMessageForACodeWithoutMessage() {
        givenMessageOf(new KuraException(KuraErrorCode.IO_ERROR, "disk", "full"));

        thenMessageIs("Generic Error - IO_ERROR: full");
    }

    @Test
    public void shouldFallBackToTheGenericMessageWithoutCode() {
        givenMessageOf(new KuraException(null, "first", "second"));

        thenMessageIs("Generic Error - Unknown: {1}");
    }

    @Test
    public void shouldUseTheSameMessagesForRuntimeExceptions() {
        givenMessageOf(new KuraRuntimeException(KuraErrorCode.BAD_REQUEST, "no body"));

        thenMessageIs("Bad request. no body");
    }

    @Test
    public void shouldFallBackToTheGenericMessageForARuntimeExceptionWithoutMessage() {
        givenMessageOf(new KuraRuntimeException(KuraErrorCode.UNSUPPORTED_MODEM));

        thenMessageIs("Generic Error - UNSUPPORTED_MODEM: {1}");
    }

    @Test
    public void shouldLocalizeTheMessageWithTheDefaultLocale() {
        givenLocalizedMessageOf(new KuraException(KuraErrorCode.NOT_FOUND));

        thenMessageIs("Not found.");
    }

    private void givenMessageOf(final Exception exception) {
        this.message = exception.getMessage();
    }

    private void givenLocalizedMessageOf(final Exception exception) {
        final Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ITALY);
            this.message = exception.getLocalizedMessage();
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    private void thenMessageIs(final String expected) {
        assertEquals(expected, this.message);
    }
}
