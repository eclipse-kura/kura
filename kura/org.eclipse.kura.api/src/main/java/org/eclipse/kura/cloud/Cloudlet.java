/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cloudlet is an abstract class that can be extended by services that wants to implement remote resource management.
 * The Cloudlet  abstracts the detailed of the communication with the remote clients providing easy to use template
 * methods to be implemented by subclasses to handle CRUD operations on local resources.
 * <ul>
 * <li>{@link Cloudlet#doGet} is used to implement a READ request for a resource identified by the supplied {@link CloudletTopic#getResources()} 
 * <li>{@link Cloudlet#doPut} is used to implement a CREATE or UPDATE request for a resource identified by the supplied {@link CloudletTopic#getResources()} 
 * <li>{@link Cloudlet#doDel} is used to implement a DELETE request for a resource identified by the supplied {@link CloudletTopic#getResources()} 
 * <li>{@link Cloudlet#doPost} is used to implement other operations on a resource identified by the supplied {@link CloudletTopic#getResources()} 
 * <li>{@link Cloudlet#doExec} is used to perform applicatioon operation not necessary tied to a given resource.
 * </ul> 
 */
public abstract class Cloudlet implements CloudClientListener 
{		
	private static final Logger s_logger = LoggerFactory.getLogger(Cloudlet.class);

	protected static final int     DFLT_PUB_QOS  = 0;
	protected static final boolean DFLT_RETAIN   = false;
	protected static final int     DFLT_PRIORITY = 1;

	private static int NUM_CONCURRENT_CALLBACKS = 2;
	private static ExecutorService m_callbackExecutor = Executors.newFixedThreadPool(NUM_CONCURRENT_CALLBACKS);

	private CloudService m_cloudService;
	private CloudClient  m_cloudClient;

	private ComponentContext m_ctx;

	private String       m_applicationId;



	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public void setCloudService(CloudService cloudService) {
		m_cloudService = cloudService;
	}

	public void unsetCloudService(CloudService cloudService) {
		m_cloudService = null;
	}



	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext) 
	{			
		// get the mqtt client for this application
		try  {

			s_logger.info("Getting CloudApplicationClient for {}...", m_applicationId);
			m_cloudClient = m_cloudService.newCloudClient(m_applicationId);
			m_cloudClient.addCloudClientListener(this);

			//Don't subscribe because these are handled by the default subscriptions and we don't want to get messages twice
			m_ctx= componentContext;
		}
		catch (KuraException e) {
			s_logger.error("Cannot activate", e);
			throw new ComponentException(e);
		}
	}

	protected void deactivate(ComponentContext componentContext) 
	{
		// close the application client. 
		// this will unsubscribe all open subscriptions
		s_logger.info("Releasing CloudApplicationClient for {}...", m_applicationId);
		if(m_cloudClient != null){
			m_cloudClient.release();
		}
	}

	protected Cloudlet(String appId) {
		this.m_applicationId = appId;
	}

	public String getAppId() {
		return m_applicationId;
	}

	protected CloudService getCloudService() {
		return m_cloudService;
	}

	protected CloudClient getCloudApplicationClient() {
		return m_cloudClient;
	}

	protected ComponentContext getComponentContext(){
		return m_ctx;
	}

	// ----------------------------------------------------------------
	//
	//   Default handlers
	//
	// ----------------------------------------------------------------

	protected void doGet(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
			throws KuraException {
		s_logger.info("Default GET handler");
		respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
	}

	protected void doPut(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
			throws KuraException {
		s_logger.info("Default PUT handler");
		respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
	}

	protected void doPost(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
			throws KuraException {
		s_logger.info("Default POST handler");
		respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
	}

	protected void doDel(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
			throws KuraException {
		s_logger.info("Default DEL handler");
		respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
	}

	protected void doExec(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload)
			throws KuraException {
		s_logger.info("Default EXEC handler");
		respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
	}

	@Override
	public void onControlMessageArrived(String deviceId, 
			String appTopic,
			KuraPayload msg, 
			int qos, 
			boolean retain) 
	{		
		try {

			s_logger.debug("Control Arrived on topic: {}", appTopic);

			StringBuilder sb = new StringBuilder(m_applicationId)
			.append("/")
			.append("REPLY");

			if (appTopic.startsWith(sb.toString())) {
				// Ignore replies
				return;
			}

			// Handle the message asynchronously to not block the master client
			m_callbackExecutor.submit( new MessageHandlerCallable(this,
					deviceId,
					appTopic,
					msg,
					qos,
					retain));
		} catch (Throwable t) {
			s_logger.error("Unexpected throwable: {}", t);
		}
	}

	@Override
	public void onMessageArrived(String deviceId, 
			String appTopic,
			KuraPayload msg, 
			int qos, 
			boolean retain) 
	{
		s_logger.error("Unexpected message arrived on topic: " + appTopic);
	}

	@Override
	public void onConnectionLost() {
		s_logger.warn("Cloud Client Connection Lost!");
	}

	@Override
	public void onConnectionEstablished() {
		s_logger.info("Cloud Client Connection Restored");
	}

	@Override
	public void onMessageConfirmed(int messageId, String topic) {
		s_logger.debug("Message Confirmed (" + messageId + ")");
	}

	@Override
	public void onMessagePublished(int messageId, String topic) {
		s_logger.debug("Message Published (" + messageId + ")");
	}
}

class MessageHandlerCallable implements Callable<Void> 
{	
	private static final Logger s_logger = LoggerFactory.getLogger(MessageHandlerCallable.class);

	private Cloudlet   m_cloudApp;
	@SuppressWarnings("unused")
	private String     m_deviceId;
	private String     m_appTopic;
	private KuraPayload m_msg;
	@SuppressWarnings("unused")
	private int        m_qos;
	@SuppressWarnings("unused")
	private boolean    m_retain;

	public MessageHandlerCallable(Cloudlet cloudApp,
			String deviceId,
			String appTopic,
			KuraPayload msg,
			int qos,
			boolean retain) {
		super();
		this.m_cloudApp = cloudApp;
		this.m_deviceId = deviceId;
		this.m_appTopic = appTopic;
		this.m_msg = msg;
		this.m_qos = qos;
		this.m_retain = retain;
	}

	@Override
	public Void call() throws Exception 
	{		
		s_logger.debug("Control Arrived on topic: {}", m_appTopic);

		// Prepare the default response
		KuraRequestPayload reqPayload = KuraRequestPayload.buildFromKuraPayload(m_msg);
		KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

		try {

			CloudletTopic reqTopic = CloudletTopic.parseAppTopic(m_appTopic);
			CloudletTopic.Method method = reqTopic.getMethod();
			switch (method) {
			case GET:
				s_logger.debug("Handling GET request topic: {}", m_appTopic);
				m_cloudApp.doGet(reqTopic, reqPayload, respPayload);
				break;

			case PUT:
				s_logger.debug("Handling PUT request topic: {}", m_appTopic);
				m_cloudApp.doPut(reqTopic, reqPayload, respPayload);
				break;

			case POST:
				s_logger.debug("Handling POST request topic: {}", m_appTopic);
				m_cloudApp.doPost(reqTopic, reqPayload, respPayload);
				break;

			case DEL:
				s_logger.debug("Handling DEL request topic: {}", m_appTopic);
				m_cloudApp.doDel(reqTopic, reqPayload, respPayload);
				break;

			case EXEC:
				s_logger.debug("Handling EXEC request topic: {}", m_appTopic);
				m_cloudApp.doExec(reqTopic, reqPayload, respPayload);
				break;

			default:
				s_logger.error("Bad request topic: {}", m_appTopic);
				respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
				break;
			}
		}
		catch (IllegalArgumentException e) {
			s_logger.error("Bad request topic: {}", m_appTopic);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
		}
		catch (KuraException e) {
			s_logger.error("Error handling request topic: {}\n{}", m_appTopic, e);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			respPayload.setException(e);
		}

		try {

			CloudClient cloudClient = m_cloudApp.getCloudApplicationClient();			
			respPayload.setTimestamp(new Date());

			StringBuilder sb = new StringBuilder("REPLY")
			.append("/")
			.append(reqPayload.getRequestId());

			String requesterClientId = reqPayload.getRequesterClientId();

			s_logger.debug("Publishing response topic: {}", sb.toString());			
			cloudClient.controlPublish(
					requesterClientId,
					sb.toString(),
					respPayload,
					Cloudlet.DFLT_PUB_QOS,
					Cloudlet.DFLT_RETAIN,
					Cloudlet.DFLT_PRIORITY);
		}
		catch (KuraException e) {
			s_logger.error("Error publishing response for topic: {}\n{}", m_appTopic, e);
		}

		return null;
	}

}
