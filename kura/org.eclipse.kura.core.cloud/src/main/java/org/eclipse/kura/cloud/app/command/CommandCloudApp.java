/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud.app.command;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.command.PasswordCommandService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandCloudApp extends Cloudlet implements ConfigurableComponent, PasswordCommandService {

    private static final Logger logger = LoggerFactory.getLogger(CommandCloudApp.class);
    private static final String EDC_PASSWORD_METRIC_NAME = "command.password";
    private static final String COMMAND_ENABLED_ID = "command.enable";
    private static final String COMMAND_PASSWORD_ID = "command.password.value";
    private static final String COMMAND_WORKDIR_ID = "command.working.directory";
    private static final String COMMAND_TIMEOUT_ID = "command.timeout";
    private static final String COMMAND_ENVIRONMENT_ID = "command.environment";

    public static final String APP_ID = "CMD-V1";

    private Map<String, Object> properties;

    private ComponentContext compCtx;
    private CryptoService cryptoService;

    private boolean currentStatus;

    /* EXEC */
    public static final String RESOURCE_COMMAND = "command";

    public CommandCloudApp() {
        super(APP_ID);
    }

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    // This component inherits the required dependencies from the parent
    // class CloudApp.

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        this.cryptoService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    // This component inherits the activation methods from the parent
    // class CloudApp.
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        logger.info("Cloudlet {} has started with config!", APP_ID);
        this.compCtx = componentContext;
        this.currentStatus = (Boolean) properties.get(COMMAND_ENABLED_ID);
        if (this.currentStatus) {
            super.activate(this.compCtx);
        }
        updated(properties);
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updated...: {}", properties);

        this.properties = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.equals(COMMAND_PASSWORD_ID)) {
                try {
                    Password decryptedPassword = new Password(
                            this.cryptoService.decryptAes(value.toString().toCharArray()));
                    this.properties.put(key, decryptedPassword);
                } catch (Exception e) {
                    this.properties.put(key, new Password((String) value));
                }
            } else {
                this.properties.put(key, value);
            }
        }

        boolean newStatus = (Boolean) properties.get(COMMAND_ENABLED_ID);
        boolean stateChanged = this.currentStatus != newStatus;
        if (stateChanged) {
            this.currentStatus = newStatus;
            if (!this.currentStatus && getCloudApplicationClient() != null) {
                super.deactivate(this.compCtx);
            }
            if (this.currentStatus) {
                super.activate(this.compCtx);
            }
        }
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        logger.info("Cloudlet {} is deactivating!", APP_ID);
        if (getCloudApplicationClient() != null) {
            super.deactivate(this.compCtx);
        }
    }

    @Override
    protected void doExec(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
            throws KuraException {

        String[] resources = reqTopic.getResources();

        if (resources == null || resources.length != 1) {
            logger.error("Bad request topic: {}", reqTopic);
            logger.error("Expected one resource but found {}", resources != null ? resources.length : "none");
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        if (!resources[0].equals(RESOURCE_COMMAND)) {
            logger.error("Bad request topic: {}", reqTopic);
            logger.error("Cannot find resource with name: {}", resources[0]);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
            return;
        }

        logger.info("EXECuting resource: {}", RESOURCE_COMMAND);

        KuraCommandResponsePayload commandResp = execute(reqPayload);

        for (String name : commandResp.metricNames()) {
            Object value = commandResp.getMetric(name);
            respPayload.addMetric(name, value);
        }
        respPayload.setBody(commandResp.getBody());
    }

    @Override
    public KuraCommandResponsePayload execute(KuraRequestPayload reqPayload) {
        KuraCommandRequestPayload commandReq = new KuraCommandRequestPayload(reqPayload);

        String receivedPassword = (String) commandReq.getMetric(EDC_PASSWORD_METRIC_NAME);
        Password commandPassword = (Password) this.properties.get(COMMAND_PASSWORD_ID);

        KuraCommandResponsePayload commandResp = new KuraCommandResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        boolean isExecutionAllowed = verifyPasswords(commandPassword, receivedPassword);
        if (isExecutionAllowed) {

            String command = commandReq.getCommand();
            if (command == null || command.trim().isEmpty()) {
                logger.error("null command");
                commandResp.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
                return commandResp;
            }

            String[] cmdarray = prepareCommandArray(commandReq, command);

            String[] envp = getEnvironment(commandReq);
            String dir = getDir(commandReq);

            byte[] zipBytes = commandReq.getZipBytes();
            if (zipBytes != null) {
                try {
                    UnZip.unZipBytes(zipBytes, dir);
                } catch (IOException e) {
                    logger.error("Error unzipping command zip bytes", e);
                    commandResp.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
                    commandResp.setException(e);
                    return commandResp;
                }
            }

            Process proc = null;
            try {
                proc = createExecutionProcess(dir, cmdarray, envp);
            } catch (Throwable t) {
                logger.error("Error executing command {}", t);
                commandResp.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
                commandResp.setException(t);
                return commandResp;
            }

            boolean runAsync = commandReq.isRunAsync() != null ? commandReq.isRunAsync() : false;
            int timeout = getTimeout(commandReq);

            ProcessMonitorThread pmt = new ProcessMonitorThread(proc, commandReq.getStdin(), timeout);
            pmt.start();

            if (!runAsync) {
                try {
                    pmt.join();
                    prepareResponseNoTimeout(commandResp, pmt);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    pmt.interrupt();
                    prepareTimeoutResponse(commandResp, pmt);
                }
            }

        } else {
            logger.error("Password required but not correct and/or missing");
            commandResp.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
            commandResp.setExceptionMessage("Password missing or not correct");
        }

        return commandResp;
    }

    @Override
    public String execute(String cmd, String password) throws KuraException {
        boolean verificationEnabled = (Boolean) this.properties.get(COMMAND_ENABLED_ID);
        if (verificationEnabled) {

            Password commandPassword = (Password) this.properties.get(COMMAND_PASSWORD_ID);
            boolean isExecutionAllowed = verifyPasswords(commandPassword, password);
            if (isExecutionAllowed) {

                String[] cmdArray = cmd.split(" ");
                String defaultDir = getDefaultWorkDir();
                String[] environment = getDefaultEnvironment();
                try {
                    Process proc = createExecutionProcess(defaultDir, cmdArray, environment);

                    int timeout = getDefaultTimeout();
                    ProcessMonitorThread pmt = new ProcessMonitorThread(proc, null, timeout);
                    pmt.start();

                    try {
                        pmt.join();
                        // until the process finishes, exitValue is null; in case of timeout, it remains null
                        if (pmt.getExitValue() != null && pmt.getExitValue() == 0) {
                            return pmt.getStdout();
                        } else {
                            return pmt.getStderr();
                        }
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        pmt.interrupt();
                        throw KuraException.internalError(e);
                    }
                } catch (IOException ex) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, ex);
                }
            } else {
                throw new KuraException(KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID);
            }
        } else {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
        }
    }

    // command service defaults getters
    private String getDefaultWorkDir() {

        String workDir = (String) this.properties.get(COMMAND_WORKDIR_ID);
        if (workDir != null && !workDir.isEmpty()) {
            return workDir;
        }
        return System.getProperty("java.io.tmpdir");
    }

    private int getDefaultTimeout() {
        return (Integer) this.properties.get(COMMAND_TIMEOUT_ID);
    }

    private String[] getDefaultEnvironment() {
        String envString = (String) this.properties.get(COMMAND_ENVIRONMENT_ID);
        if (envString != null) {
            return envString.split(" ");
        }
        return null;
    }

    private String getDir(KuraCommandRequestPayload req) {
        String dir = req.getWorkingDir();
        String defaultDir = getDefaultWorkDir();
        if (dir != null && !dir.isEmpty()) {
            return dir;
        }
        return defaultDir;
    }

    private int getTimeout(KuraCommandRequestPayload req) {
        Integer timeout = req.getTimeout();
        int defaultTimeout = getDefaultTimeout();
        if (timeout != null) {
            return timeout;
        }
        return defaultTimeout;
    }

    private String[] getEnvironment(KuraCommandRequestPayload req) {
        String[] envp = req.getEnvironmentPairs();
        String[] defaultEnv = getDefaultEnvironment();
        if (envp != null && envp.length != 0) {
            return envp;
        }
        return defaultEnv;
    }

    private boolean verifyPasswords(Password commandPassword, String receivedPassword) {
        if (commandPassword == null && receivedPassword == null) {
            return true;
        }
        if (commandPassword == null && "".equals(receivedPassword)) {
            return true;
        }
        if (commandPassword == null) {
            return false;
        }

        if ("".equals(commandPassword.toString()) && receivedPassword == null) {
            return true;
        }
        if ("".equals(commandPassword.toString()) && "".equals(receivedPassword)) {
            return true;
        }

        String pwd = commandPassword.toString();
        return pwd.equals(receivedPassword);
    }

    private Process createExecutionProcess(String dir, String[] cmdarray, String[] envp) throws IOException {
        Runtime rt = Runtime.getRuntime();
        File fileDir = dir == null ? null : new File(dir);
        return rt.exec(cmdarray, envp, fileDir);
    }

    private String[] prepareCommandArray(KuraCommandRequestPayload req, String command) {
        String[] args = req.getArguments();
        int argsCount = args != null ? args.length : 0;
        String[] cmdarray = new String[1 + argsCount];

        cmdarray[0] = command;
        if (args != null) {
            System.arraycopy(args, 0, cmdarray, 1, argsCount);
        }

        for (String element : cmdarray) {
            logger.debug("cmdarray: {}", element);
        }

        return cmdarray;
    }

    private void prepareResponseNoTimeout(KuraCommandResponsePayload resp, ProcessMonitorThread pmt) {
        if (pmt.getException() != null) {
            resp.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
            resp.setException(pmt.getException());
        } else {
            resp.setTimedout(pmt.isTimedOut());

            if (!pmt.isTimedOut()) {
                resp.setExitCode(pmt.getExitValue());
            }
        }
        resp.setStderr(pmt.getStderr());
        resp.setStdout(pmt.getStdout());
    }

    private void prepareTimeoutResponse(KuraCommandResponsePayload resp, ProcessMonitorThread pmt) {
        resp.setStderr(pmt.getStderr());
        resp.setStdout(pmt.getStdout());
        resp.setTimedout(true);
    }
}