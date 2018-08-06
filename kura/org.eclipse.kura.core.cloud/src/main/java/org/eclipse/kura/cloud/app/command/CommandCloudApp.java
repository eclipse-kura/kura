/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

import static org.eclipse.kura.cloudconnection.request.RequestHandlerConstants.ARGS_KEY;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.command.PasswordCommandService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandCloudApp implements ConfigurableComponent, PasswordCommandService, RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(CommandCloudApp.class);
    private static final String EDC_PASSWORD_METRIC_NAME = "command.password";
    private static final String COMMAND_ENABLED_ID = "command.enable";
    private static final String COMMAND_PASSWORD_ID = "command.password.value";
    private static final String COMMAND_WORKDIR_ID = "command.working.directory";
    private static final String COMMAND_TIMEOUT_ID = "command.timeout";
    private static final String COMMAND_ENVIRONMENT_ID = "command.environment";

    public static final String APP_ID = "CMD-V1";

    private Map<String, Object> properties;

    private CryptoService cryptoService;

    private boolean currentStatus;

    /* EXEC */
    public static final String RESOURCE_COMMAND = "command";

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

    public void setRequestHandlerRegistry(RequestHandlerRegistry requestHandlerRegistry) {
        try {
            requestHandlerRegistry.registerRequestHandler(APP_ID, this);
        } catch (KuraException e) {
            logger.info("Unable to register request handler {} in {}", APP_ID, requestHandlerRegistry.getClass().getName());
        }
    }

    public void unsetRequestHandlerRegistry(RequestHandlerRegistry requestHandlerRegistry) {
        try {
            requestHandlerRegistry.unregister(APP_ID);
        } catch (KuraException e) {
            logger.info("Unable to register request handler {} in {}", APP_ID, requestHandlerRegistry.getClass().getName());
        }
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    // This component inherits the activation methods from the parent
    // class CloudApp.
    protected void activate(Map<String, Object> properties) {
        logger.info("Request handler {} has started with config!", APP_ID);

        updated(properties);
    }

    public void updated(Map<String, Object> properties) {
        logger.info("updated...: {}", properties);

        this.currentStatus = (Boolean) properties.get(COMMAND_ENABLED_ID);

        this.properties = new HashMap<>();

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
    }

    protected void deactivate(ComponentContext componentContext) {
        logger.info("Request handler {} is deactivating!", APP_ID);

    }

    @SuppressWarnings("unchecked")
    @Override
    public KuraMessage doExec(RequestHandlerContext requestContext, KuraMessage reqMessage) throws KuraException {

        if (!this.currentStatus) {
            throw new KuraException(KuraErrorCode.NOT_FOUND);
        }

        Object requestObject = reqMessage.getProperties().get(ARGS_KEY.value());
        List<String> resources;
        if (requestObject instanceof List) {
            resources = (List<String>) requestObject;
        } else {
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        if (resources.size() != 1) {
            logger.error("Bad request topic: {}", resources);
            logger.error("Expected one resource but found {}", resources.size());
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
        }

        if (!resources.get(0).equals(RESOURCE_COMMAND)) {
            logger.error("Bad request topic: {}", resources);
            logger.error("Cannot find resource with name: {}", resources.get(0));
            throw new KuraException(KuraErrorCode.NOT_FOUND);
        }

        logger.info("EXECuting resource: {}", RESOURCE_COMMAND);

        KuraPayload reqPayload = reqMessage.getPayload();
        KuraPayload resPayload = execute(reqPayload);
        return new KuraMessage(resPayload);
    }

    @Override
    public KuraPayload execute(KuraPayload reqPayload) throws KuraException {
        KuraCommandRequestPayload commandReq = new KuraCommandRequestPayload(reqPayload);

        String receivedPassword = (String) commandReq.getMetric(EDC_PASSWORD_METRIC_NAME);
        Password commandPassword = (Password) this.properties.get(COMMAND_PASSWORD_ID);

        KuraCommandResponsePayload commandResp = new KuraCommandResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

        boolean isExecutionAllowed = verifyPasswords(commandPassword, receivedPassword);
        if (isExecutionAllowed) {

            String command = commandReq.getCommand();
            if (command == null || command.trim().isEmpty()) {
                logger.error("null command");
                throw new KuraException(KuraErrorCode.BAD_REQUEST);
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
                    throw new KuraException(KuraErrorCode.DECODER_ERROR);
                }
            }

            Process proc = null;
            try {
                proc = createExecutionProcess(dir, cmdarray, envp);
            } catch (Throwable t) {
                logger.error("Error executing command {}", t);
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR);
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
                    Thread.currentThread().interrupt();
                    pmt.interrupt();
                    prepareTimeoutResponse(commandResp, pmt);
                }
            }

        } else {
            logger.error("Password required but not correct and/or missing");
            throw new KuraException(KuraErrorCode.BAD_REQUEST);
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
                        Thread.currentThread().interrupt();
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