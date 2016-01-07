/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
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
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandCloudApp extends Cloudlet implements ConfigurableComponent,
PasswordCommandService {
	private static final Logger s_logger = LoggerFactory.getLogger(CommandCloudApp.class);
	private static final String EDC_PASSWORD_METRIC_NAME = "command.password";
	private static final String COMMAND_ENABLED_ID = "command.enable";
	private static final String COMMAND_PASSWORD_ID = "command.password.value";
	private static final String COMMAND_WORKDIR_ID = "command.working.directory";
	private static final String COMMAND_TIMEOUT_ID = "command.timeout";
	private static final String COMMAND_ENVIRONMENT_ID = "command.environment";


	public static final String APP_ID = "CMD-V1";

	private Map<String, Object> properties;

	private ComponentContext compCtx;
	private CryptoService m_cryptoService;

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
		this.m_cryptoService = cryptoService;
	}

	public void unsetCryptoService(CryptoService cryptoService) {
		this.m_cryptoService = null;
	}

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	// This component inherits the activation methods from the parent
	// class CloudApp.
	protected void activate(ComponentContext componentContext,
			Map<String, Object> properties) {
		s_logger.info("Bundle " + APP_ID + " has started with config!");
		this.compCtx = componentContext;
		currentStatus = (Boolean) properties.get(COMMAND_ENABLED_ID);
		if (currentStatus) {
			super.activate(compCtx);
		}
		updated(properties);
	}

	public void updated(Map<String, Object> properties) {
		s_logger.info("updated...: " + properties);

		this.properties= new HashMap<String, Object>();

		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (key.equals(COMMAND_PASSWORD_ID)) {
				try {
					char[] decryptedPassword= m_cryptoService.decryptAes(value.toString().toCharArray());
					this.properties.put(key, decryptedPassword);
				} catch (Exception e) {
					this.properties.put(key, value.toString().toCharArray());
				} 
			}else{
				this.properties.put(key, value);
			}
		}

		boolean newStatus = (Boolean) properties.get(COMMAND_ENABLED_ID);
		boolean stateChanged= currentStatus != newStatus;
		if(stateChanged){
			currentStatus= newStatus;
			if (!currentStatus && getCloudApplicationClient() != null) {
				super.deactivate(compCtx);
			}
			if (currentStatus) {
				super.activate(compCtx);
			}
		}
	}

	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("Bundle " + APP_ID + " is deactivating!");
		if (getCloudApplicationClient() != null) {
			super.deactivate(compCtx);
		}
	}

	@Override
	protected void doExec(CloudletTopic reqTopic,
			KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
					throws KuraException {

		String[] resources = reqTopic.getResources();

		if (resources == null || resources.length != 1) {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Expected one resource but found {}",
					resources != null ? resources.length : "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}

		if (!resources[0].equals(RESOURCE_COMMAND)) {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Cannot find resource with name: {}", resources[0]);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			return;
		}

		s_logger.info("EXECuting resource: {}", RESOURCE_COMMAND);

		KuraCommandResponsePayload commandResp = execute(reqPayload);

		for (String name : commandResp.metricNames()) {
			Object value = commandResp.getMetric(name);
			respPayload.addMetric(name, value);
		}
		respPayload.setBody(commandResp.getBody());
	}

	@Override
	public KuraCommandResponsePayload execute(KuraRequestPayload reqPayload) {
		KuraCommandRequestPayload commandReq = new KuraCommandRequestPayload(
				reqPayload);

		// String receivedPassword= (String)
		// reqPayload.getMetric(EDC_PASSWORD_METRIC_NAME);
		String receivedPassword = (String) commandReq.getMetric(EDC_PASSWORD_METRIC_NAME);
		char[] commandPassword = (char[])properties.get(COMMAND_PASSWORD_ID);

		KuraCommandResponsePayload commandResp = new KuraCommandResponsePayload(
				KuraResponsePayload.RESPONSE_CODE_OK);

		boolean isExecutionAllowed = verifyPasswords(commandPassword,
				receivedPassword);
		if (isExecutionAllowed) {

			String command = commandReq.getCommand();
			if (command == null) {
				s_logger.error("null command");
				commandResp
				.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			}

			String[] cmdarray = prepareCommandArray(commandReq, command);

			String[] envp = getEnvironment(commandReq);
			String dir = getDir(commandReq);

			byte[] zipBytes = commandReq.getZipBytes();
			if (zipBytes != null) {
				try {
					UnZip.unZipBytes(zipBytes, dir);
				} catch (IOException e) {
					s_logger.error("Error unzipping command zip bytes", e);

					commandResp.setException(e);
				}
			}

			Process proc = null;
			try {
				proc = createExecutionProcess(dir, cmdarray, envp);
			} catch (Throwable t) {
				s_logger.error("Error executing command {}", t);
				commandResp
				.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
				commandResp.setException(t);

			}

			boolean runAsync = commandReq.isRunAsync() != null ? commandReq
					.isRunAsync() : false;
					int timeout = getTimeout(commandReq);

					ProcessMonitorThread pmt = null;
					pmt = new ProcessMonitorThread(proc, commandReq.getStdin(), timeout);
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

			s_logger.error("Password required but not correct and/or missing");
			commandResp
			.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			commandResp.setExceptionMessage("Password missing or not correct");
		}

		return commandResp;

	}

	@Override
	public String execute(String cmd, String password) throws KuraException {
		// TODO Auto-generated method stub
		boolean verificationEnabled = (Boolean) properties.get(COMMAND_ENABLED_ID);
		if (verificationEnabled) {

			char[] commandPassword = (char[]) properties.get(COMMAND_PASSWORD_ID);
			boolean isExecutionAllowed = verifyPasswords(commandPassword,
					password);
			if (isExecutionAllowed) {

				String[] cmdArray = cmd.split(" ");
				String defaultDir = getDefaultWorkDir();
				String[] environment = getDefaultEnvironment();
				try {
					Process proc = createExecutionProcess(defaultDir, cmdArray,
							environment);

					int timeout = getDefaultTimeout();
					ProcessMonitorThread pmt = null;
					pmt = new ProcessMonitorThread(proc, null, timeout);
					pmt.start();

					try {
						pmt.join();
						if (pmt.getExitValue() == 0) {
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
				throw new KuraException(
						KuraErrorCode.CONFIGURATION_ATTRIBUTE_INVALID);
			}
		} else {
			throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
		}

	}

	// command service defaults getters
	private String getDefaultWorkDir() {
		return (String) properties.get(COMMAND_WORKDIR_ID);
	}

	private int getDefaultTimeout() {
		return (Integer) properties.get(COMMAND_TIMEOUT_ID);
	}

	private String[] getDefaultEnvironment() {
		String envString = (String) properties.get(COMMAND_ENVIRONMENT_ID);
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

	private boolean verifyPasswords(char[] commandPassword,
			String receivedPassword) {
		if (commandPassword == null && receivedPassword == null) {
			return true;
		}
		if(commandPassword == null){
			return false;
		}
		String pwd = new String(commandPassword);
		return pwd.equals(receivedPassword);
	}

	private Process createExecutionProcess(String dir, String[] cmdarray,
			String[] envp) throws IOException {
		Runtime rt = Runtime.getRuntime();
		Process proc = null;
		File fileDir = dir == null ? null : new File(dir);
		proc = rt.exec(cmdarray, envp, fileDir);
		return proc;
	}

	private String[] prepareCommandArray(KuraCommandRequestPayload req,
			String command) {
		String[] args = req.getArguments();
		int argsCount = args != null ? args.length : 0;
		String[] cmdarray = new String[1 + argsCount];

		cmdarray[0] = command;
		for (int i = 0; i < argsCount; i++) {
			cmdarray[1 + i] = args[i];
		}

		for (int i = 0; i < cmdarray.length; i++) {
			s_logger.debug("cmdarray: {}", cmdarray[i]);
		}

		return cmdarray;
	}

	private void prepareResponseNoTimeout(KuraCommandResponsePayload resp,
			ProcessMonitorThread pmt) {

		if (pmt.getException() != null) {
			resp.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			resp.setException(pmt.getException());
			resp.setStderr(pmt.getStderr());
			resp.setStdout(pmt.getStdout());
		} else {
			resp.setStderr(pmt.getStderr());
			resp.setStdout(pmt.getStdout());
			resp.setTimedout(pmt.isTimedOut());

			if (!pmt.isTimedOut()) {
				resp.setExitCode(pmt.getExitValue());
			}
		}

	}

	private void prepareTimeoutResponse(KuraCommandResponsePayload resp,
			ProcessMonitorThread pmt) {
		resp.setStderr(pmt.getStderr());
		resp.setStdout(pmt.getStdout());
		resp.setTimedout(true);
	}

}