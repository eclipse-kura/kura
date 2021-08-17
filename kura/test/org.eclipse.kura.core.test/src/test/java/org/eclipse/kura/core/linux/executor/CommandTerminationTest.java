package org.eclipse.kura.core.linux.executor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.kura.core.internal.linux.executor.ExecutorUtil;
import org.eclipse.kura.core.linux.executor.LinuxPid;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.core.linux.executor.privileged.PrivilegedExecutorServiceImpl;
import org.eclipse.kura.core.linux.executor.unprivileged.UnprivilegedExecutorServiceImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.Pid;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CommandTerminationTest {

    private static CommandExecutorService executor;
    private static Pid pid = new LinuxPid(1234);
    private static String[] commandLine = { "find", "/", "-name", "foo" };
    private boolean isStopped;
    private boolean isKilled;

    public CommandTerminationTest(CommandExecutorService executor) {
        CommandTerminationTest.executor = executor;
    }

    @Parameterized.Parameters
    public static Collection<CommandExecutorService> getExecutors() {
        return Arrays.asList(new UnprivilegedExecutorServiceImpl(), new PrivilegedExecutorServiceImpl());
    }

    @Test
    public void shouldStopCommand() {
        givenCommandExecutor();

        whenStopCommand(pid, null);

        thenCommandIsStopped();
    }

    @Test
    public void shouldNotStopCommandWithSignal() {
        givenCommandExecutor();

        whenStopCommand(pid, LinuxSignal.SIGHUP);

        thenCommandIsNotStopped();
    }

    @Test
    public void shouldKillCommand() {
        givenCommandExecutor();

        whenKillCommand(commandLine, null);

        thenCommandIsKilled();
    }

    @Test
    public void shouldNotKillCommandWithSignal() {
        givenCommandExecutor();

        whenKillCommand(commandLine, LinuxSignal.SIGHUP);

        thenCommandIsNotKilled();
    }

    private void givenCommandExecutor() {
        ExecutorUtil euMock = mock(ExecutorUtil.class);
        when(euMock.stopPrivileged(pid, LinuxSignal.SIGTERM)).thenReturn(true);
        when(euMock.stopPrivileged(pid, LinuxSignal.SIGHUP)).thenReturn(false);
        when(euMock.killPrivileged(commandLine, LinuxSignal.SIGTERM)).thenReturn(true);
        when(euMock.killPrivileged(commandLine, LinuxSignal.SIGHUP)).thenReturn(false);
        when(euMock.stopUnprivileged(pid, LinuxSignal.SIGTERM)).thenReturn(true);
        when(euMock.stopUnprivileged(pid, LinuxSignal.SIGHUP)).thenReturn(false);
        when(euMock.killUnprivileged(commandLine, LinuxSignal.SIGTERM)).thenReturn(true);
        when(euMock.killUnprivileged(commandLine, LinuxSignal.SIGHUP)).thenReturn(false);
        try {
            TestUtil.setFieldValue(executor, "executorUtil", euMock);
        } catch (NoSuchFieldException e) {
            // Do nothing...
        }
    }

    private void whenStopCommand(Pid pid, LinuxSignal signal) {
        this.isStopped = executor.stop(pid, signal);
    }

    private void whenKillCommand(String[] commandLine, LinuxSignal signal) {
        this.isKilled = executor.kill(commandLine, signal);
    }

    private void thenCommandIsStopped() {
        assertTrue(this.isStopped);
    }

    private void thenCommandIsNotStopped() {
        assertFalse(this.isStopped);
    }

    private void thenCommandIsKilled() {
        assertTrue(this.isKilled);
    }

    private void thenCommandIsNotKilled() {
        assertFalse(this.isKilled);
    }
}
