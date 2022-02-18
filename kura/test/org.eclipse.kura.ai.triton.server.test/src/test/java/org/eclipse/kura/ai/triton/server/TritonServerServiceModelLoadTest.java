package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraIOException;
import org.junit.Test;

public class TritonServerServiceModelLoadTest {

    TritonServerServiceImpl tritonServer;
    boolean exceptionCaught = false;

    @Test
    public void shouldNotLoadModel() throws KuraIOException {
        givenTritonServerServiceImpl();

        whenLoadModel();

        thenModelExceptionIsCaught();
    }

    @Test
    public void shouldNotUnloadModel() throws KuraIOException {
        givenTritonServerServiceImpl();

        whenUnloadModel();

        thenModelExceptionIsCaught();
    }

    @Test
    public void shouldNotGetModelLoadState() throws KuraIOException {
        givenTritonServerServiceImpl();

        whenGetModelLoadState();

        thenModelExceptionIsCaught();
    }

    private void givenTritonServerServiceImpl() {
        this.exceptionCaught = false;
        this.tritonServer = new TritonServerServiceImpl();
        Map<String, Object> properties = new HashMap<>();
        properties.put("server.address", "localhost");
        properties.put("server.ports", "4000,4001,4002");
        properties.put("enable.local", "false");
        this.tritonServer.activate(properties);
    }

    private void whenLoadModel() throws KuraIOException {
        try {
            this.tritonServer.loadModel("myModel", Optional.empty());
        } catch (KuraIOException e) {
            this.exceptionCaught = true;
        }
    }

    private void whenGetModelLoadState() throws KuraIOException {
        try {
            this.tritonServer.isModelLoaded("myModel");
        } catch (KuraIOException e) {
            this.exceptionCaught = true;
        }
    }

    private void whenUnloadModel() throws KuraIOException {
        try {
            this.tritonServer.unloadModel("myModel");
        } catch (KuraIOException e) {
            this.exceptionCaught = true;
        }
    }

    private void thenModelExceptionIsCaught() {
        assertTrue(exceptionCaught);
    }

}
