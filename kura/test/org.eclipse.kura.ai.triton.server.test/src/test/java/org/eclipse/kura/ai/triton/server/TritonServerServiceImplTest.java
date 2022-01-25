package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TritonServerServiceImplTest {

    private final String modelName = "iris";
    private final String modelRepositoryPath = "/opt/modelRepo";
    private final String backendRepoPath = "/opt/backends";
    private final String backendConfig = "myBackend,a=1;myOtherBackend,b=true";
    private final Integer[] serverPorts = new Integer[] { 8000, 8001, 8002 };
    private TritonServerServiceImpl engine;
    private byte[] result;
    private CommandExecutorService executorService;
    ArgumentCaptor<Command> commandCapture = ArgumentCaptor.forClass(Command.class);

    @Test
    public void shouldServerStart() {
        givenInferenceEngine();

        whenActivate();

        thenServerStarts();
    }

    @Test
    public void shouldServerStop() {
        givenInferenceEngine();
        givenServerActivated();

        whenDeactivate();

        thenServerStops();
    }

    @Test
    public void shouldLoadModel() {

    }

    @Test
    public void shouldDeleteModel() {

    }

    @Test
    public void shouldGetModelNames() {

    }

    @Test
    public void shouldGetModelInfo() {

    }

    @Test
    public void shouldInfer() {
        givenInferenceEngine();

        whenInfer();

        thenResultIsNotEmpty();
    }

    private void givenInferenceEngine() {
        this.engine = new TritonServerServiceImpl();

        this.executorService = mock(CommandExecutorService.class);
        doNothing().when(this.executorService).execute(commandCapture.capture(), anyObject());
        when(this.executorService.kill(getStartCommand().getCommandLine(), LinuxSignal.SIGINT)).thenReturn(true);

        this.engine.setCommandExecutorService(this.executorService);
    }

    private void givenServerActivated() {
        activateServer();
    }

    private void whenActivate() {
        activateServer();
    }

    private void whenDeactivate() {
        this.engine.deactivate();
    }

    private void whenInfer() {
        this.result = this.engine.infer(this.modelName);
    }

    private void thenServerStarts() {
        assertEquals(getStartCommand(), commandCapture.getValue());
    }

    private void thenServerStops() {
        verify(this.executorService, times(1)).kill(new String[] { "tritonserver" }, LinuxSignal.SIGINT);
    }

    private void thenResultIsNotEmpty() {
        assertTrue(this.result.length > 0);
    }

    private void activateServer() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("enable", true);
        properties.put("server.ports", new Integer[] { 8000, 8001, 8002 });
        properties.put("model.repository.path", this.modelRepositoryPath);
        properties.put("backends.path", this.backendRepoPath);
        properties.put("backends.config", this.backendConfig);
        this.engine.activate(properties);
    }

    private Command getStartCommand() {
        List<String> commandString = new ArrayList<>();
        commandString.add("tritonserver");
        commandString.add("--model-repository=" + this.modelRepositoryPath);
        commandString.add("--backend-directory=" + this.backendRepoPath);
        String[] configs = this.backendConfig.split(";");
        commandString.add("--backend-config=" + configs[0]);
        commandString.add("--backend-config=" + configs[1]);
        commandString.add("--http-port=" + serverPorts[0]);
        commandString.add("--grpc-port=" + serverPorts[1]);
        commandString.add("--metrics-port=" + serverPorts[2]);
        commandString.add("2>&1");
        commandString.add("|");
        commandString.add("systemd-cat");
        commandString.add("-t tritonserver");
        commandString.add("-p info");
        Command command = new Command(commandString.toArray(new String[0]));
        command.setExecuteInAShell(true);
        return command;
    }

}
