package org.eclipse.kura.ai.triton.server;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.CommandExecutorService;
import org.junit.Test;

public class TritonServerLocalManagerTest {

    @Test
    public void killMethodShouldWork() {
        // Given option
        Map<String, Object> properties = new HashMap<>();
        properties.put("server.address", "localhost");
        properties.put("server.ports", new Integer[] { 4000, 4001, 4002 });
        properties.put("enable.local", Boolean.TRUE);

        TritonServerServiceOptions options = new TritonServerServiceOptions(properties);

        // Given command executor service
        CommandExecutorService ces = mock(CommandExecutorService.class);
        when(ces.isRunning(new String[] { "tritonserver" })).thenReturn(true);

        // Given TritonServerLocalManager built with
        TritonServerLocalManager manager = new TritonServerLocalManager(options, ces, "test");

        // When method is called
        manager.kill();

        // Then command execution kill method is called
        String[] cmd = new String[] { "tritonserver" };
        verify(ces, times(1)).kill(cmd, LinuxSignal.SIGKILL);
    }

}
