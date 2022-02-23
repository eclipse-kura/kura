package org.eclipse.kura.ai.triton.server;

import java.io.IOException;

import org.junit.Test;

public class TritonServerServiceEngineTest extends TritonServerServiceStepDefinitions {

    @Test
    public void engineShouldBeReady() throws IOException {
        givenTritonServerServiceImpl(defaultProperties());

        whenAskingIfEngineIsReady();

        thenEngineIsReady();
    }
}
