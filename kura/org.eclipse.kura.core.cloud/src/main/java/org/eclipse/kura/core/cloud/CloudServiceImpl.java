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
/*
* Copyright (c) 2011 Eurotech Inc. All rights reserved.
*/

package org.eclipse.kura.core.cloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudConnectionEstablishedEvent;
import org.eclipse.kura.cloud.CloudConnectionLostEvent;
import org.eclipse.kura.cloud.CloudPayloadProtoBufDecoder;
import org.eclipse.kura.cloud.CloudPayloadProtoBufEncoder;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.message.KuraBirthPayload;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.DataServiceListener;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraTopic;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.modem.ModemReadyEvent;
import org.eclipse.kura.position.PositionLockedEvent;
import org.eclipse.kura.position.PositionService;
import org.eclipse.kura.system.SystemAdminService;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudServiceImpl implements CloudService, DataServiceListener, ConfigurableComponent, EventHandler, CloudPayloadProtoBufEncoder, CloudPayloadProtoBufDecoder
{	
	private static final Logger s_logger = LoggerFactory.getLogger(CloudServiceImpl.class);
	
	private static final String     TOPIC_BA_APP = "BA";
	
	private ComponentContext        m_ctx;

	private CloudServiceOptions     m_options;

	private DataService             m_dataService;
	private SystemService           m_systemService;
	private SystemAdminService      m_systemAdminService;
	private NetworkService          m_networkService;
	private PositionService		    m_positionService;
	private EventAdmin              m_eventAdmin;

	// use a synchronized implementation for the list
	private List<CloudClientImpl>   m_cloudClients;
	
	// package visibility for LyfeCyclePayloadBuilder
	String                  		m_imei;
	String                  		m_iccid;
	String                  		m_imsi;

	
	public CloudServiceImpl() {
		m_cloudClients = new CopyOnWriteArrayList<CloudClientImpl>();
	}
	
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public void setDataService(DataService dataService) {
		this.m_dataService = dataService;
	}

	public void unsetDataService(DataService dataService) {
		this.m_dataService = null;
	}
	
	public DataService getDataService() {
		return m_dataService;
	}

	public void setSystemAdminService(SystemAdminService systemAdminService) {
		this.m_systemAdminService = systemAdminService;
	}

	public void unsetSystemAdminService(SystemAdminService systemAdminService) {
		this.m_systemAdminService = null;
	}

	public SystemAdminService getSystemAdminService() {
		return m_systemAdminService;
	}

	public void setSystemService(SystemService systemService) {
		this.m_systemService = systemService;
	}

	public void unsetSystemService(SystemService systemService) {
		this.m_systemService = null;
	}

	public SystemService getSystemService() {
		return m_systemService;
	}

	public void setNetworkService(NetworkService networkService) {
		this.m_networkService = networkService;
	}
	
	public void unsetNetworkService(NetworkService networkService) {
		this.m_networkService = null;
	}
	
	public NetworkService getNetworkService() {
		return m_networkService;
	}

	public void setPositionService(PositionService positionService) {
		this.m_positionService = positionService;
	}
	
	public void unsetPositionService(PositionService positionService) {
		this.m_positionService = null;
	}

	public PositionService getPositionService() {
		return m_positionService;
	}
	
	public void setEventAdmin(EventAdmin eventAdmin) {
		this.m_eventAdmin = eventAdmin;
	}
	
	public void unsetEventAdmin(EventAdmin eventAdmin) {
		this.m_eventAdmin = null;
	}
	
	
	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("activate...");
		
		//
		// save the bundle context and the properties
		m_ctx = componentContext;
		m_options = new CloudServiceOptions(properties, m_systemService);

		//
		// install event listener for GPS locked event
		Dictionary<String,Object> props = new Hashtable<String,Object>();
		String[] eventTopics = {PositionLockedEvent.POSITION_LOCKED_EVENT_TOPIC, ModemReadyEvent.MODEM_EVENT_READY_TOPIC};
		props.put(EventConstants.EVENT_TOPIC, eventTopics);
		m_ctx.getBundleContext().registerService(EventHandler.class.getName(), this, props);
	}
		
	public void updated(Map<String,Object> properties)
	{
		s_logger.info("updated...: " + properties);

		// Update properties and re-publish Birth certificate
		m_options = new CloudServiceOptions(properties, m_systemService);
		if (m_dataService != null && m_dataService.isConnected()) {
			publishBirthCertificate();
		}
	}
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("deactivate...");

		if (m_dataService != null && m_dataService.isConnected()) {
			publishDisconnectCertificate();
		}

		// no need to release the cloud clients as the updated app 
		// certificate is already published due the missing dependency
		// we only need to empty our CloudClient list
		m_cloudClients.clear();

		m_dataService        = null;
		m_systemService      = null;
		m_systemAdminService = null;
		m_networkService     = null;
		m_positionService    = null;
		m_eventAdmin         = null;
	}
	
	
	public void handleEvent(Event event) 
	{
		if (PositionLockedEvent.POSITION_LOCKED_EVENT_TOPIC.contains(event.getTopic())) {
			// if we get a position locked event, 
			// republish the birth certificate only if we are configured to
			s_logger.info("Handling PositionLockedEvent");
			if (m_dataService.isConnected() && m_options.getRepubBirthCertOnGpsLock()) {
				publishBirthCertificate();
			}
		} else if (ModemReadyEvent.MODEM_EVENT_READY_TOPIC.contains(event.getTopic())) {
			s_logger.info("Handling ModemReadyEvent");
			ModemReadyEvent modemReadyEvent = (ModemReadyEvent) event;
			// keep these identifiers around until we can publish the certificate
			m_imei = (String)modemReadyEvent.getProperty(ModemReadyEvent.IMEI);
			m_imsi = (String)modemReadyEvent.getProperty(ModemReadyEvent.IMSI);
			m_iccid = (String)modemReadyEvent.getProperty(ModemReadyEvent.ICCID);
			if (m_dataService.isConnected() && m_options.getRepubBirthCertOnModemDetection()) {
				publishBirthCertificate();
			}
		}
	}
	
	
	
	// ----------------------------------------------------------------
	//
	//   Service APIs
	//
	// ----------------------------------------------------------------

	@Override
	public CloudClient newCloudClient(String applicationId)
		throws KuraException 
	{
		// create new instance
		CloudClientImpl cloudClient = new CloudClientImpl(applicationId,
														  m_dataService,
													      this);
		m_cloudClients.add(cloudClient);
		
		// publish updated birth certificate with list of active apps
		if (m_dataService != null && m_dataService.isConnected()) {
			publishAppCertificate();
		}
		
		// return 
		return cloudClient;
	}
	
	
	@Override
	public String[] getCloudApplicationIdentifiers() 
	{
		List<String> appIds = new ArrayList<String>();
		for (CloudClientImpl cloudClient : m_cloudClients) {
			appIds.add(cloudClient.getApplicationId());
		}		
		return appIds.toArray(new String[0]);
	}

	@Override
	public boolean isConnected() 
	{
		return (m_dataService != null && m_dataService.isConnected());
	}
	
	
	// ----------------------------------------------------------------
	//
	//   Package APIs
	//
	// ----------------------------------------------------------------

	public CloudServiceOptions getCloudServiceOptions() {
		return m_options;
	}
	
	
	public void removeCloudClient(CloudClientImpl cloudClient) 
	{
		// remove the client		
		m_cloudClients.remove(cloudClient);
		
		// publish updated birth certificate with updated list of active apps
		if (m_dataService != null && m_dataService.isConnected()) {
			publishAppCertificate();
		}
	}
	
	
	byte[] encodePayload(KuraPayload payload)
		throws KuraException
	{
		byte[] bytes = new byte[0];
		if (payload == null) {
			return bytes;
		}
		
		CloudPayloadEncoder encoder = new CloudPayloadProtoBufEncoderImpl(payload);
		if (m_options.getEncodeGzip()) {
			encoder = new CloudPayloadGZipEncoder(encoder);
		}
		
		try {
			bytes = encoder.getBytes();
			return bytes;
		}
		catch (IOException e) {
			throw new KuraException(KuraErrorCode.ENCODE_ERROR, e);
		}
	}

	
	
	// ----------------------------------------------------------------
	//
	//   DataServiceListener API
	//
	// ----------------------------------------------------------------

	@Override
	public void onConnectionEstablished() 
	{
		// publish birth certificate
		publishBirthCertificate();
		
		// raise event
		m_eventAdmin.postEvent( new CloudConnectionEstablishedEvent( new HashMap<String,Object>()));		
		
		// restore default subscriptions
		activateDeviceSubscriptions();
		
		// notify listeners
		for (CloudClientImpl cloudClient : m_cloudClients) {
			cloudClient.onConnectionEstablished();
		}
	}
	

	private void activateDeviceSubscriptions()		
	{
		StringBuilder sbDeviceSubscription = new StringBuilder();
		sbDeviceSubscription.append(m_options.getTopicControlPrefix())
  						    .append(m_options.getTopicSeparator())
						    .append(m_options.getTopicAccountToken())
						    .append(m_options.getTopicSeparator())
						    .append(m_options.getTopicClientIdToken())
						    .append(m_options.getTopicSeparator())
						    .append(m_options.getTopicWildCard());

		// restore default subscriptions
		try {
			m_dataService.subscribe(sbDeviceSubscription.toString(), 1);
		}
		catch (KuraException e) {
			s_logger.error("Error - Cannot activate default subscriptions", e);
		}
	}

	
	@Override
	public void onDisconnecting() 
	{
		// publish disconnect certificate
		publishDisconnectCertificate();
	}

	@Override
	public void onDisconnected() {
		// raise event
		m_eventAdmin.postEvent( new CloudConnectionLostEvent( new HashMap<String,Object>()));
	}

	@Override
	public void onConnectionLost(Throwable cause) 
	{
		// raise event
		m_eventAdmin.postEvent( new CloudConnectionLostEvent( new HashMap<String,Object>()));
		
		// notify listeners
		for (CloudClientImpl cloudClient : m_cloudClients) {
			cloudClient.onConnectionLost();
		}
	}

	@Override
	public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) 
	{
		s_logger.debug("Message arrived on topic: {}", topic);		
			
		// notify listeners			
		KuraTopic kuraTopic = new KuraTopic(topic);
		if (TOPIC_BA_APP.equals(kuraTopic.getApplicationId())) {
			s_logger.info("Ignoring feedback message from "+topic);
		}
		else {
			KuraPayload kuraPayload = null;
			try {
				// try to decode the message into an KuraPayload					
				kuraPayload = (new CloudPayloadProtoBufDecoderImpl(payload)).buildFromByteArray(); 
			}
			catch (Exception e) {
				// Wrap the received bytes payload into an KuraPayload					
				s_logger.debug("Received message on topic "+topic+" that could not be decoded. Wrapping it into an KuraPayload.");
				kuraPayload = new KuraPayload();
				kuraPayload.setBody(payload);
			}
			for (CloudClientImpl cloudClient : m_cloudClients) {
				if (cloudClient.getApplicationId().equals(kuraTopic.getApplicationId())) {
					try {
						if (m_options.getTopicControlPrefix().equals(kuraTopic.getPrefix())) {
							cloudClient.onControlMessageArrived(kuraTopic.getDeviceId(), 
																kuraTopic.getApplicationTopic(), 
																kuraPayload, 
														        qos, 
														        retained);
						}
						else {
							cloudClient.onMessageArrived(kuraTopic.getDeviceId(), 
													     kuraTopic.getApplicationTopic(), 
													     kuraPayload, 
													     qos, 
													     retained);
						}
					}
					catch (Exception e) {
						s_logger.error("Error during CloudClientListener notification.", e);
					}
				}
			}
		}
	}

	@Override
	public void onMessagePublished(int messageId, String topic) {
		// notify listeners			
		KuraTopic kuraTopic = new KuraTopic(topic);
		for (CloudClientImpl cloudClient : m_cloudClients) {
			if (cloudClient.getApplicationId().equals(kuraTopic.getApplicationId())) {
				cloudClient.onMessagePublished(messageId, kuraTopic.getApplicationTopic());
			}
		}
	}

	@Override
	public void onMessageConfirmed(int messageId, String topic) {
		// notify listeners			
		KuraTopic kuraTopic = new KuraTopic(topic);
		for (CloudClientImpl cloudClient : m_cloudClients) {
			if (cloudClient.getApplicationId().equals(kuraTopic.getApplicationId())) {
				cloudClient.onMessageConfirmed(messageId, kuraTopic.getApplicationTopic());
			}
		}
	}

	// ----------------------------------------------------------------
	//
	//   CloudPayloadProtoBufEncoder API
	//
	// ----------------------------------------------------------------
	
	@Override
	public byte[] getBytes(KuraPayload kuraPayload, boolean gzipped) throws KuraException {		
		CloudPayloadEncoder encoder = new CloudPayloadProtoBufEncoderImpl(kuraPayload);
		if (gzipped) {
			encoder = new CloudPayloadGZipEncoder(encoder);
		}
		
		byte[] bytes;
		try {
			bytes = encoder.getBytes();
			return bytes;
		}
		catch (IOException e) {
			throw new KuraException(KuraErrorCode.ENCODE_ERROR, e);
		}
	}

	// ----------------------------------------------------------------
	//
	//   CloudPayloadProtoBufDecoder API
	//
	// ----------------------------------------------------------------
	
	@Override
	public KuraPayload buildFromByteArray(byte[] payload) throws KuraException {
		CloudPayloadProtoBufDecoderImpl encoder = new CloudPayloadProtoBufDecoderImpl(payload);
		KuraPayload kuraPayload;
		
		try {
			kuraPayload = encoder.buildFromByteArray();
			return kuraPayload;
		} catch (KuraInvalidMessageException e) {
			throw new KuraException(KuraErrorCode.DECODER_ERROR, e);
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.DECODER_ERROR, e);
		}
	}
	
	// ----------------------------------------------------------------
	//
	//   Birth and Disconnect Certificates
	//
	// ----------------------------------------------------------------
		
	
	private void publishBirthCertificate() 
	{
		StringBuilder sbTopic = new StringBuilder();
		sbTopic.append(m_options.getTopicControlPrefix())
		  	   .append(m_options.getTopicSeparator())
		       .append(m_options.getTopicAccountToken())
		       .append(m_options.getTopicSeparator())
		       .append(m_options.getTopicClientIdToken())
		       .append(m_options.getTopicSeparator())
		       .append(m_options.getTopicBirthSuffix());
		
		String topic = sbTopic.toString();
		KuraPayload payload = createBirthPayload();
		publishLifeCycleMessage(topic, payload);
	}
	
	
	private void publishDisconnectCertificate() 
	{
		StringBuilder sbTopic = new StringBuilder();
		sbTopic.append(m_options.getTopicControlPrefix())
		  	   .append(m_options.getTopicSeparator())
		       .append(m_options.getTopicAccountToken())
		       .append(m_options.getTopicSeparator())
		       .append(m_options.getTopicClientIdToken())
		       .append(m_options.getTopicSeparator())
		       .append(m_options.getTopicDisconnectSuffix());
		
		String topic = sbTopic.toString();
		KuraPayload payload = createDisconnectPayload();
		publishLifeCycleMessage(topic, payload);
	}
	
	
	private void publishAppCertificate() 
	{
		StringBuilder sbTopic = new StringBuilder();
		sbTopic.append(m_options.getTopicControlPrefix())
		  	   .append(m_options.getTopicSeparator())
		       .append(m_options.getTopicAccountToken())
		       .append(m_options.getTopicSeparator())
		       .append(m_options.getTopicClientIdToken())
		       .append(m_options.getTopicSeparator())
		       .append(m_options.getTopicAppsSuffix());
		
		String topic = sbTopic.toString();
		KuraPayload payload = createBirthPayload();
		publishLifeCycleMessage(topic, payload);
	}

	
	private KuraPayload createBirthPayload()
	{		
		LifeCyclePayloadBuilder payloadBuilder = new LifeCyclePayloadBuilder(this);
		return payloadBuilder.buildBirthPayload();
	}
	
	
	private KuraPayload createDisconnectPayload() 
	{
		LifeCyclePayloadBuilder payloadBuilder = new LifeCyclePayloadBuilder(this);
		return payloadBuilder.buildDisconnectPayload();
	}
		
	
	private void publishLifeCycleMessage(String topic, KuraPayload payload) 
	{
		try {			
			byte[] encodedPayload = encodePayload(payload);
			m_dataService.publish(topic, 
								  encodedPayload, 
							      m_options.getLifeCycleMessageQos(), 
							      m_options.getLifeCycleMessageRetain(), 
							      m_options.getLifeCycleMessagePriority());
		}
		catch (Exception e) {
			s_logger.error("Error publishing life-cycle message on topic: "+topic, e);
		}
	}
}
