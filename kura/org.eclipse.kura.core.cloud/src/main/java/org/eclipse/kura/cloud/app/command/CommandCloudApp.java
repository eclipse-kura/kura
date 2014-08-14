/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandCloudApp extends Cloudlet {
	private static final Logger s_logger = LoggerFactory.getLogger(CommandCloudApp.class);

	public static final String APP_ID = "CMD-V1";

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
	

	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	// This component inherits the activation methods from the parent
	// class CloudApp.
	
	@Override
	protected void doExec(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
			throws KuraException {

		String[] resources = reqTopic.getResources();

		if (resources == null || resources.length != 1) {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Expected one resource but found {}", resources !=null ? resources.length: "none");
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

		KuraCommandRequestPayload commandReq = new KuraCommandRequestPayload(reqPayload);
		KuraCommandResponsePayload commandResp = new KuraCommandResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

		KuraCommandHandler.handleRequest(commandReq, commandResp);

		for (String name : commandResp.metricNames()) {
			Object value = commandResp.getMetric(name);
			respPayload.addMetric(name, value);
		}
		respPayload.setBody(commandResp.getBody());
	}
}