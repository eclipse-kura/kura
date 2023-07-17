/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.command.provider.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.app.command.CommandCloudApp;
import org.eclipse.kura.cloud.app.command.KuraCommandRequestPayload;
import org.eclipse.kura.cloud.app.command.KuraCommandResponsePayload;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.internal.rest.command.CommandRestService;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.rest.command.api.RestCommandRequest;
import org.eclipse.kura.rest.command.api.RestCommandResponse;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class CommandRestServiceTest {

    private static final String ARGS_KEY = "args";

    CommandCloudApp commandCloudApp;
    CommandRestService commandRestService;
    Boolean hasExceptionOccurred = false;

    RestCommandRequest restCommandRequest = new RestCommandRequest();
    RestCommandResponse restCommandResponse = new RestCommandResponse();

    KuraCommandResponsePayload mockedKuraCommandResponsePayload;

    private ArgumentCaptor<KuraMessage> kuraPayloadArgumentCaptor = ArgumentCaptor.forClass(KuraMessage.class);
    KuraMessage capturedKuraMessage;
    KuraCommandRequestPayload captureKuraCommandRequestPayload;

    Response asyncResponse;

    @Test
    public void commandExecNull() throws KuraException {
        givenCommandCloudApp();
        givenCommandRestService();

        givenMockedResponseStdout("example stdout 1");
        givenMockedResponseStderr("example stderr 1");
        givenMockedResponseExitCode(0);
        givenMockedResponseTimedout(false);

        whenExecCommand();

        thenVerifyDoExecIsRun();
        thenCommandRequestIs(Arrays.asList("command"));
    }

    @Test
    public void commandExecWithCommandTest() throws KuraException {
        givenCommandCloudApp();
        givenCommandRestService();
        givenCommand("ls");

        givenMockedResponseStdout("example stdout 2");
        givenMockedResponseStderr("example stderr 2");
        givenMockedResponseExitCode(0);
        givenMockedResponseTimedout(false);

        whenExecCommand();

        thenVerifyDoExecIsRun();
        thenCommandRequestIs(Arrays.asList("command"));
        thenCommandIs("ls");

        thenVerifyKuraResponseStdoutIs("example stdout 2");
        thenVerifyKuraResponseStderrIs("example stderr 2");
        thenVerifyKuraResponseExitCodeIs(0);
        thenVerifyKuraResponseTimedoutIs(false);
    }

    @Test
    public void commandExecWithFullParamsTest() throws KuraException {
        givenCommandCloudApp();
        givenCommandRestService();

        givenCommand("ls");
        givenArguments(new String[] { "arg1", "arg2" });
        givenEnvironmentPairs(Collections.singletonMap("testvar", "testValue"));
        givenWorkingDirectory("/tmp");
        givenPassword("password");
        givenZipBytes("emlwQnl0ZXM=");

        givenMockedResponseStdout("example stdout 3");
        givenMockedResponseStderr("example stderr 3");
        givenMockedResponseExitCode(0);
        givenMockedResponseTimedout(false);

        whenExecCommand();

        thenVerifyDoExecIsRun();
        thenCommandRequestIs(Arrays.asList("command"));
        thenCommandIs("ls");
        thenArgumentsIs(new String[] { "arg1", "arg2" });
        thenEnvironmentPairsIs(Collections.singletonMap("testvar", "testValue"));
        thenWorkingDirectoryIs("/tmp");
        thenPasswordIs("password");
        thenIsRunAsyncIs(false);
        thenZipBytesIs(new byte[] { 'z', 'i', 'p', 'B', 'y', 't', 'e', 's' });

        thenVerifyKuraResponseStdoutIs("example stdout 3");
        thenVerifyKuraResponseStderrIs("example stderr 3");
        thenVerifyKuraResponseExitCodeIs(0);
        thenVerifyKuraResponseTimedoutIs(false);
    }

    @Test
    public void commandExecAsycWithFullParamsTest() throws KuraException {
        givenCommandCloudApp();
        givenCommandRestService();

        givenCommand("ls");
        givenArguments(new String[] { "arg1", "arg2" });
        givenEnvironmentPairs(Collections.singletonMap("testvar", "testValue"));
        givenWorkingDirectory("/tmp");
        givenPassword("password");
        givenZipBytes("emlwQnl0ZXM=");

        givenMockedResponseStdout("example stdout 3");
        givenMockedResponseStderr("example stderr 3");
        givenMockedResponseExitCode(0);
        givenMockedResponseTimedout(false);

        whenExecAsyncCommand();

        thenVerifyDoExecIsRun();
        thenCommandRequestIs(Arrays.asList("command"));
        thenCommandIs("ls");
        thenArgumentsIs(new String[] { "arg1", "arg2" });
        thenEnvironmentPairsIs(Collections.singletonMap("testvar", "testValue"));
        thenWorkingDirectoryIs("/tmp");
        thenPasswordIs("password");
        thenIsRunAsyncIs(true);
        thenZipBytesIs(new byte[] { 'z', 'i', 'p', 'B', 'y', 't', 'e', 's' });
    }

    private void givenCommandCloudApp() throws KuraException {
        commandCloudApp = mock(CommandCloudApp.class);

        mockedKuraCommandResponsePayload = new KuraCommandResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        KuraMessage fakeKuraMessage = new KuraMessage(mockedKuraCommandResponsePayload);

        when(commandCloudApp.doExec(any(), any())).thenReturn(fakeKuraMessage);
    }

    private void givenCommandRestService() {
        commandRestService = new CommandRestService();
        commandRestService.setCommandCloudApp(commandCloudApp);
    }

    private void givenMockedResponseStdout(String stdout) {
        mockedKuraCommandResponsePayload.setStdout(stdout);
    }

    private void givenMockedResponseStderr(String stderr) {
        mockedKuraCommandResponsePayload.setStderr(stderr);
    }

    private void givenMockedResponseExitCode(int exitCode) {
        mockedKuraCommandResponsePayload.setExitCode(exitCode);
    }

    private void givenMockedResponseTimedout(boolean isTimedout) {
        mockedKuraCommandResponsePayload.setTimedout(isTimedout);
    }

    private void givenCommand(String command) {
        this.restCommandRequest.setCommand(command);
    }

    private void givenArguments(String[] arguments) {
        this.restCommandRequest.setArguments(arguments);
    }

    private void givenEnvironmentPairs(Map<String, String> environmentPairs) {
        this.restCommandRequest.setEnvironmentPairs(environmentPairs);
    }

    private void givenWorkingDirectory(String workingDirectory) {
        this.restCommandRequest.setWorkingDirectory(workingDirectory);
    }

    private void givenPassword(String password) {
        this.restCommandRequest.setPassword(password);
    }

    private void givenZipBytes(String zipBytes) {
        this.restCommandRequest.setZipBytes(zipBytes);
    }

    private void whenExecCommand() {
        restCommandResponse = commandRestService.execCommand(this.restCommandRequest);
    }

    private void whenExecAsyncCommand() {
        asyncResponse = commandRestService.execAsyncCommand(this.restCommandRequest);
    }

    private void thenVerifyDoExecIsRun() throws KuraException {
        verify(commandCloudApp).doExec(any(), kuraPayloadArgumentCaptor.capture());
    }

    private void thenCommandRequestIs(List<String> expectedArgs) {
        capturedKuraMessage = kuraPayloadArgumentCaptor.getValue();
        captureKuraCommandRequestPayload = (KuraCommandRequestPayload) capturedKuraMessage.getPayload();

        assertEquals(expectedArgs, capturedKuraMessage.getProperties().get(ARGS_KEY));
    }

    private void thenCommandIs(String command) {
        assertEquals(command, captureKuraCommandRequestPayload.getCommand());
    }

    private void thenArgumentsIs(String[] arguments) {
        assertArrayEquals(arguments, captureKuraCommandRequestPayload.getArguments());
    }

    private void thenEnvironmentPairsIs(Map<String, String> environmentPairs) {
        this.restCommandRequest.setEnvironmentPairs(environmentPairs);
    }

    private void thenWorkingDirectoryIs(String workingDirectory) {
        assertEquals(workingDirectory, captureKuraCommandRequestPayload.getWorkingDir());
    }

    private void thenPasswordIs(String password) {
        assertEquals(password, captureKuraCommandRequestPayload.getMetric("command.password"));
    }

    private void thenIsRunAsyncIs(boolean isRunAsync) {
        assertEquals(isRunAsync, captureKuraCommandRequestPayload.isRunAsync());
    }

    private void thenZipBytesIs(byte[] zipBytes) {
        assertEquals(new String(zipBytes), new String(captureKuraCommandRequestPayload.getZipBytes()));
    }

    private void thenVerifyKuraResponseStdoutIs(String stdout) {
        assertEquals(stdout, restCommandResponse.getStdout());
    }

    private void thenVerifyKuraResponseStderrIs(String stderr) {
        assertEquals(stderr, restCommandResponse.getStderr());
    }

    private void thenVerifyKuraResponseExitCodeIs(int exitCode) {
        assertEquals(exitCode, restCommandResponse.getExitCode());
    }

    private void thenVerifyKuraResponseTimedoutIs(boolean isTimedout) {
        assertEquals(isTimedout, restCommandResponse.getIsTimeOut());
    }
}
