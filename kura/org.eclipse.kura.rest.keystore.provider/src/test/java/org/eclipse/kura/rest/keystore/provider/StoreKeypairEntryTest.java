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
package org.eclipse.kura.rest.keystore.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.internal.rest.keystore.provider.KeystoreRestService;
import org.eclipse.kura.internal.rest.keystore.request.KeyPairWriteRequest;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import jakarta.ws.rs.WebApplicationException;

public class StoreKeypairEntryTest {

    private static final String KEYSTORE_PID = "MyKeystore";
    private static final String ALIAS = "MyAlias";
    private static final String ATTRIBUTES = "CN=Kura, OU=IoT, O=Eclipse, C=US";

    @Test
    public void shouldStoreKeyPair() throws Exception {
        givenKeystoreService();

        whenKeypairEntryIsStored();

        thenNoExceptionIsThrown();
        thenKeyPairIsCreated();
    }

    @Test
    public void shouldReturnBadRequestWhenKeystoreRejectsParameters() throws Exception {
        givenKeystoreService();
        givenKeystoreFailsWith(KuraErrorCode.BAD_REQUEST);

        whenKeypairEntryIsStored();

        thenResponseStatusIs(400);
    }

    @Test
    public void shouldReturnInternalServerErrorWhenKeystoreFails() throws Exception {
        givenKeystoreService();
        givenKeystoreFailsWith(KuraErrorCode.INTERNAL_ERROR);

        whenKeypairEntryIsStored();

        thenResponseStatusIs(500);
    }

    private final KeystoreService keystoreService = mock(KeystoreService.class);
    private KeystoreRestService restService;
    private Optional<Exception> exception = Optional.empty();

    private void givenKeystoreService() {
        this.restService = new KeystoreRestService() {

            @Override
            public void activate(ComponentContext componentContext) {
                this.keystoreServices.put(KEYSTORE_PID, StoreKeypairEntryTest.this.keystoreService);
            }
        };
        this.restService.activate(null);
    }

    private void givenKeystoreFailsWith(final KuraErrorCode code) throws KuraException {
        doThrow(new KuraException(code, "invalid key pair parameters")).when(this.keystoreService)
                .createKeyPair(ALIAS, "RSA", 1024, "SHA256WithRSA", ATTRIBUTES);
    }

    private void whenKeypairEntryIsStored() throws NoSuchFieldException {
        final KeyPairWriteRequest writeRequest = new KeyPairWriteRequest(KEYSTORE_PID, ALIAS);
        TestUtil.setFieldValue(writeRequest, "algorithm", "RSA");
        TestUtil.setFieldValue(writeRequest, "signatureAlgorithm", "SHA256WithRSA");
        TestUtil.setFieldValue(writeRequest, "size", 1024);
        TestUtil.setFieldValue(writeRequest, "attributes", ATTRIBUTES);

        try {
            this.restService.storeKeypairEntry(writeRequest);
        } catch (final Exception e) {
            this.exception = Optional.of(e);
        }
    }

    private void thenNoExceptionIsThrown() {
        if (this.exception.isPresent()) {
            fail("unexpected exception: " + this.exception.get());
        }
    }

    private void thenKeyPairIsCreated() throws KuraException {
        verify(this.keystoreService, times(1)).createKeyPair(ALIAS, "RSA", 1024, "SHA256WithRSA", ATTRIBUTES);
    }

    private void thenResponseStatusIs(final int expectedStatus) {
        assertTrue(this.exception.isPresent());
        assertTrue("unexpected exception: " + this.exception.get(),
                this.exception.get() instanceof WebApplicationException);
        assertEquals(expectedStatus, ((WebApplicationException) this.exception.get()).getResponse().getStatus());
    }
}
