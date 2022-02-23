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
        givenTritonServerServiceImpl(defaultProperties());

        whenLoadModel("myModel2");

        thenExceptionIsCaught();
    }

    @Test
    public void shouldLoadModel() throws KuraException, IOException {
        givenTritonServerServiceImpl(defaultProperties());

        whenLoadModel("myModel");

        thenModelIsLoaded();
    }

    @Test
    public void shouldNotUnloadModel() throws KuraException, IOException {
        givenTritonServerServiceImpl(defaultProperties());

        whenUnloadModel("myModel2");

        thenExceptionIsCaught();
    }

    @Test
    public void shouldUnloadModel() throws KuraException, IOException {
        givenTritonServerServiceImpl(defaultProperties());

        whenUnloadModel("myModel");

        thenModelIsUnLoaded();
    }

    @Test
    public void shouldNotGetModelLoadState() throws KuraException, IOException {
        givenTritonServerServiceImpl(defaultProperties());

        whenGetModelLoadState("myModel");

        thenExceptionIsCaught();
    }

    @Test
    public void shouldGetModelNamesList() throws IOException {
        givenTritonServerServiceImpl(defaultProperties());

        whenGetModelNames();

        thenListIsNotEmpty();

    }

    @Test
    public void shouldGetModelInfo() throws IOException {
        givenTritonServerServiceImpl(defaultProperties());

        whenGetModelInfo("myModel");

        thenModelInfoExists();
    }

}
