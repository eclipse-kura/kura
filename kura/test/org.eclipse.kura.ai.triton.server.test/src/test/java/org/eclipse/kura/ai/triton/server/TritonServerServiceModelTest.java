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

import org.eclipse.kura.KuraException;
import org.junit.Test;

public class TritonServerServiceModelTest extends TritonServerServiceStepDefinitions {

    @Test
    public void shouldNotLoadModel() throws KuraException, IOException {
        givenTritonServerServiceNativeImpl(defaultProperties(), false);

        whenLoadModel("myModel2");

        thenExceptionIsCaught();
    }

    @Test
    public void shouldLoadModel() throws KuraException, IOException {
        givenTritonServerServiceNativeImpl(defaultProperties(), false);

        whenLoadModel("myModel");

        thenModelIsLoaded();
    }

    @Test
    public void shouldNotUnloadModel() throws KuraException, IOException {
        givenTritonServerServiceNativeImpl(defaultProperties(), false);

        whenUnloadModel("myModel2");

        thenExceptionIsCaught();
    }

    @Test
    public void shouldUnloadModel() throws KuraException, IOException {
        givenTritonServerServiceNativeImpl(defaultProperties(), false);

        whenUnloadModel("myModel");

        thenModelIsUnLoaded();
    }

    @Test
    public void shouldNotGetModelLoadState() throws KuraException, IOException {
        givenTritonServerServiceNativeImpl(defaultProperties(), false);

        whenGetModelLoadState("myModel");

        thenExceptionIsCaught();
    }

    @Test
    public void shouldGetModelNamesList() throws IOException {
        givenTritonServerServiceNativeImpl(defaultProperties(), false);

        whenGetModelNames();

        thenListIsNotEmpty();

    }

    @Test
    public void shouldGetModelInfo() throws IOException {
        givenTritonServerServiceNativeImpl(defaultProperties(), false);

        whenGetModelInfo("myModel");

        thenModelInfoExists();
    }

}
