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
package org.eclipse.kura.core.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.message.KuraPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the CloudClient interface.  
 */
public class CloudClientImpl implements CloudClient, CloudClientListener 
{
	@SuppressWarnings("unused")
	private static final Logger s_logger = LoggerFactory.getLogger(CloudClientImpl.class);
	
	private String 			           m_applicationId;
	private DataService                m_dataService;
	private CloudServiceImpl           m_cloudServiceImpl;

	private List<CloudClientListenerAdapter> m_listeners;
	
	protected CloudClientImpl(String applicationId,
						      DataService dataService,
						      CloudServiceImpl cloudServiceImpl)
	{
		m_applicationId    = applicationId;
		m_dataService      = dataService;
		m_cloudServiceImpl = cloudServiceImpl;
		m_listeners        = new CopyOnWriteArrayList<CloudClientListenerAdapter>();
	}


	/**
	 * Returns the applicationId of this CloudApplicationClient 
	 * @return applicationId
	 */
	public String getApplicationId() {
		return m_applicationId;
	}
	
	
	/**
	 * Releases this CloudClient handle.  This instance should no longer be used.
	 * Note: CloudClient does not unsubscribes all subscriptions incurred by this client,
	 * this responsibility is left to the application developer
	 */
	public void release() 
	{
		// remove this from being a callback handler
		m_cloudServiceImpl.removeCloudClient(this);
	}

	
	// --------------------------------------------------------------------
	//
	//  CloudCallbackHandler API
	//
	// --------------------------------------------------------------------

	public void addCloudClientListener(CloudClientListener cloudClientListener) 
	{
		m_listeners.add( new CloudClientListenerAdapter(cloudClientListener));
	}

	
	public void removeCloudClientListener(CloudClientListener cloudClientListener) 
	{
		// create a copy to avoid concurrent modification exceptions
		List<CloudClientListenerAdapter> adapters = new ArrayList<CloudClientListenerAdapter>(m_listeners);
		for (CloudClientListenerAdapter adapter : adapters) {
			if (adapter.getCloudClientListenerAdapted() == cloudClientListener) {
				m_listeners.remove(adapter);
				break;
			}
		}
	}

	
	// --------------------------------------------------------------------
	//
	//  CloudClient API
	//
	// --------------------------------------------------------------------

	public boolean isConnected() {
		return m_dataService.isConnected();
	}	
	
	
	public int publish(String topic, KuraPayload payload, int qos, boolean retain) 
		throws KuraException 
	{
		boolean isControl = false;
		String   appTopic = encodeTopic(topic, isControl);
		byte[] appPayload = m_cloudServiceImpl.encodePayload(payload);
		return m_dataService.publish(appTopic, 
									 appPayload, 
								     qos, 
								     retain,
								     5);
	}

	
	public int publish(String topic, KuraPayload payload, int qos, boolean retain, int priority) 
		throws KuraException 
	{
		boolean isControl = false;
		String   appTopic = encodeTopic(topic, isControl);
		byte[] appPayload = m_cloudServiceImpl.encodePayload(payload);
		return m_dataService.publish(appTopic, 
									 appPayload, 
								     qos, 
								     retain,
								     priority);
	}

	
	public int publish(String topic, byte[] payload, int qos, boolean retain, int priority) 
		throws KuraException 
	{
		boolean isControl = false;
		String   appTopic = encodeTopic(topic, isControl);
		return m_dataService.publish(appTopic, 
									 payload, 
								     qos, 
								     retain,
								     priority);
	}

	
	public int controlPublish(String topic, KuraPayload payload, int qos, boolean retain, int priority) 
		throws KuraException 
	{
		boolean isControl = true;
		String   appTopic = encodeTopic(topic, isControl);
		byte[] appPayload = m_cloudServiceImpl.encodePayload(payload);
		return m_dataService.publish(appTopic, 
									 appPayload, 
									 qos, 
									 retain,
									 priority);
	}
	
	
	public int controlPublish(String deviceId, String topic, KuraPayload payload, int qos, boolean retain, int priority)
		throws KuraException 
	{
		boolean isControl = true;
		String   appTopic = encodeTopic(deviceId, topic, isControl);
		byte[] appPayload = m_cloudServiceImpl.encodePayload(payload);
		return m_dataService.publish(appTopic, 
								     appPayload, 
								     qos, 
								     retain,
								     priority);
	}


	public int controlPublish(String deviceId, String topic, byte[] payload, int qos, boolean retain, int priority) 
		throws KuraException 
	{
		boolean isControl = true;
		String   appTopic = encodeTopic(deviceId, topic, isControl);
		return m_dataService.publish(appTopic, 
								     payload, 
								     qos, 
								     retain,
								     priority);
	}
	

	public void subscribe(String topic, int qos) 
		throws KuraException 
	{
		boolean isControl = false;
		String   appTopic = encodeTopic(topic, isControl);
		m_dataService.subscribe(appTopic, qos);
	}

	
	public void controlSubscribe(String topic, int qos) 
		throws KuraException 
	{
		boolean isControl = true;
		String   appTopic = encodeTopic(topic, isControl);
		m_dataService.subscribe(appTopic, qos);
	}

	
	public void unsubscribe(String topic) 
		throws KuraException 
	{
		boolean isControl = false;
		String   appTopic = encodeTopic(topic, isControl);
		m_dataService.unsubscribe(appTopic);
	}


	public void controlUnsubscribe(String topic) 
		throws KuraException 
	{
		boolean isControl = true;
		String   appTopic = encodeTopic(topic, isControl);
		m_dataService.unsubscribe(appTopic);
	}
		
	@Override
	public List<Integer> getUnpublishedMessageIds() throws KuraException {
		String topicRegex = getAppTopicRegex();
		return m_dataService.getUnpublishedMessageIds(topicRegex);
	}
	
	@Override
	public List<Integer> getInFlightMessageIds() throws KuraException {
		String topicRegex = getAppTopicRegex();
		return m_dataService.getInFlightMessageIds(topicRegex);
	}
	
	@Override
	public List<Integer> getDroppedInFlightMessageIds() throws KuraException {
		String topicRegex = getAppTopicRegex();
		return m_dataService.getDroppedInFlightMessageIds(topicRegex);
	}

	// --------------------------------------------------------------------
	//
	//  CloudCallbackHandler API
	//
	// --------------------------------------------------------------------	

	public void onMessageArrived(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain) 
	{
		for (CloudClientListener listener : m_listeners) {
			listener.onMessageArrived(deviceId, appTopic, payload, qos, retain);
		}
	}

	
	public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload payload, int qos, boolean retain) 
	{
		for (CloudClientListener listener : m_listeners) {
			listener.onControlMessageArrived(deviceId, appTopic, payload, qos, retain);
		}
	}


	public void onMessageConfirmed(int pubId, String appTopic) {
		for (CloudClientListener listener : m_listeners) {
			listener.onMessageConfirmed(pubId, appTopic);
		}
	}


	public void onMessagePublished(int pubId, String appTopic) {
		for (CloudClientListener listener : m_listeners) {
			listener.onMessagePublished(pubId, appTopic);
		}
	}

	public void onConnectionEstablished() {
		for (CloudClientListener listener : m_listeners) {
			listener.onConnectionEstablished();
		}
	}

	public void onConnectionLost() {
		for (CloudClientListener listener : m_listeners) {
			listener.onConnectionLost();
		}
	}

	
	// ----------------------------------------------------------------
	//
	//   Private methods
	//
	// ----------------------------------------------------------------

	private String encodeTopic(String topic, boolean isControl)
	{
		CloudServiceOptions options = m_cloudServiceImpl.getCloudServiceOptions();
		return encodeTopic(options.getTopicClientIdToken(), topic, isControl);
	}
	
	
	private String encodeTopic(String deviceId, String topic, boolean isControl)
	{
		CloudServiceOptions options = m_cloudServiceImpl.getCloudServiceOptions();
		StringBuilder sb = new StringBuilder();
		if (isControl) {
			sb.append(options.getTopicControlPrefix())
			  .append(options.getTopicSeparator());
		}
		
		sb.append(options.getTopicAccountToken())
		  .append(options.getTopicSeparator())
		  .append(deviceId)
		  .append(options.getTopicSeparator())
		  .append(m_applicationId);
		  
		  if (topic != null && !topic.isEmpty()) {
			  sb.append(options.getTopicSeparator())
			    .append(topic);
		  }
		
		return sb.toString();
	}
	
	private String getAppTopicRegex() {
		CloudServiceOptions options = m_cloudServiceImpl.getCloudServiceOptions();
		StringBuilder sb = new StringBuilder();

		//String regexExample = "^(\\$EDC/)?eurotech/.+/conf-v1(/.+)?";
		
		// Optional control prefix
		sb.append("^(")
		//.append(options.getTopicControlPrefix())
		.append("\\$EDC")
		.append(options.getTopicSeparator())
		.append(")?")

		.append(options.getTopicAccountToken())
		.append(options.getTopicSeparator())
		.append(".+") // Any device ID
		.append(options.getTopicSeparator())
		.append(m_applicationId)
		.append("(/.+)?");
		
		return sb.toString();
	}
}
