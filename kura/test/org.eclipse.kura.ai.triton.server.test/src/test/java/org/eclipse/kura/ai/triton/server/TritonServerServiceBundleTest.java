/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.ai.triton.server;

import java.io.IOException;

import org.junit.Test;

public class TritonServerServiceBundleTest extends TritonServerServiceStepDefinitions {

    @Test
    public void shouldNotBeActivatedWithInvalidProperties() throws IOException {
        givenTritonServerServiceImplNotActive();

        whenTritonServerIsActivated(invalidProperties());

        thenExceptionIsCaught();

    }

    @Test
    public void shouldBeActivatedWithLocalManager() throws IOException, InterruptedException {
        givenTritonServerServiceImplNotActive();

        whenTritonServerIsActivated(enableLocalServerProperties());

        thenAfterWaiting(500);
        thenTritonStartServerCommandIsExecuted();

    }

    @Test
    public void shoulBeDeactivated() throws IOException {
        givenTritonServerServiceImpl(defaultProperties());

        whenDeactivateIsInvokedOnTritonServer();

        thenNoExceptionIsCaught();
    }

    @Test
    public void shouldBeUpdated() throws IOException {
        givenTritonServerServiceImpl(defaultProperties());

        whenUpdatedIsInvokedOnTritonServer(updatedProperties());

        thenNoExceptionIsCaught();
    }

}
