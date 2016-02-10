/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.example.publisher;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExamplePublisher implements ConfigurableComponent, CloudClientListener  
{	
	private static final Logger s_logger = LoggerFactory.getLogger(ExamplePublisher.class);
	
	// Cloud Application identifier
	private static final String APP_ID = "EXAMPLE_PUBLISHER";

	// Publishing Property Names
	private static final String   PUBLISH_RATE_PROP_NAME   = "publish.rate";
	private static final String   PUBLISH_TOPIC_PROP_NAME  = "publish.appTopic";
	private static final String   PUBLISH_QOS_PROP_NAME    = "publish.qos";
	private static final String   PUBLISH_RETAIN_PROP_NAME = "publish.retain";
	private static final String   TEMP_INITIAL_PROP_NAME   = "metric.temperature.initial";
	private static final String   TEMP_INCREMENT_PROP_NAME = "metric.temperature.increment";
	private static final String[] METRIC_PROP_NAMES        = { 
		"metric.string",
		"metric.string.oneof",
		"metric.long",
		"metric.integer",
		"metric.integer.fixed",
		"metric.short",
		"metric.double",
		"metric.float",
		"metric.char",
		"metric.byte",
		"metric.boolean",
		"metric.password"
	};

	
	private CloudService m_cloudService;
	private CloudClient m_cloudClient;
	
	private ScheduledExecutorService    m_worker;
	private ScheduledFuture<?>          m_handle;
	
	private float               m_temperature;
	private Map<String, Object> m_properties;
	
	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------
	
	public ExamplePublisher() 
	{
		super();
		m_worker = Executors.newSingleThreadScheduledExecutor();
	}

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

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("Activating ExamplePublisher...");
		
		m_properties = properties;
		for (String s : properties.keySet()) {
			s_logger.info("Activate - "+s+": "+properties.get(s));
		}
		
		// get the mqtt client for this application
		try  {
			
			// Acquire a Cloud Application Client for this Application 
			s_logger.info("Getting CloudApplicationClient for {}...", APP_ID);
			m_cloudClient = m_cloudService.newCloudClient(APP_ID);
			m_cloudClient.addCloudClientListener(this);
			
			// Don't subscribe because these are handled by the default 
			// subscriptions and we don't want to get messages twice			
			doUpdate();
		}
		catch (Exception e) {
			s_logger.error("Error during component activation", e);
			throw new ComponentException(e);
		}
		s_logger.info("Activating ExamplePublisher... Done.");
	}
	
	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.debug("Deactivating ExamplePublisher...");

		// shutting down the worker and cleaning up the properties
		m_worker.shutdown();
		
		// Releasing the CloudApplicationClient
		s_logger.info("Releasing CloudApplicationClient for {}...", APP_ID);
		m_cloudClient.release();

		s_logger.debug("Deactivating ExamplePublisher... Done.");
	}	
	
	public void updated(Map<String,Object> properties)
	{
		s_logger.info("Updated ExamplePublisher...");

		// store the properties received
		m_properties = properties;
		for (String s : properties.keySet()) {
			s_logger.info("Update - "+s+": "+properties.get(s));
		}
		
		// try to kick off a new job
		doUpdate();
		s_logger.info("Updated ExamplePublisher... Done.");
	}
	
	
	// ----------------------------------------------------------------
	//
	//   Cloud Application Callback Methods
	//
	// ----------------------------------------------------------------
	
	@Override
	public void onConnectionEstablished() 
	{
		s_logger.info("Connection established");
		
		try {
			// Getting the lists of unpublished messages
			s_logger.info("Number of unpublished messages: {}", m_cloudClient.getUnpublishedMessageIds().size());
		} catch (KuraException e) {
			s_logger.error("Cannot get the list of unpublished messages");
		}
		
		try {
			// Getting the lists of in-flight messages
			s_logger.info("Number of in-flight messages: {}", m_cloudClient.getInFlightMessageIds().size());
		} catch (KuraException e) {
			s_logger.error("Cannot get the list of in-flight messages");
		}
		
		try {
			// Getting the lists of dropped in-flight messages
			s_logger.info("Number of dropped in-flight messages: {}", m_cloudClient.getDroppedInFlightMessageIds().size());
		} catch (KuraException e) {
			s_logger.error("Cannot get the list of dropped in-flight messages");
		}
	}

	@Override
	public void onConnectionLost() 
	{
		s_logger.warn("Connection lost!");
	}

	@Override
	public void onControlMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		s_logger.info("Control message arrived on assetId: {} and semantic topic: {}", deviceId, appTopic);
	}

	@Override
	public void onMessageArrived(String deviceId, String appTopic,
			KuraPayload msg, int qos, boolean retain) {
		s_logger.info("Message arrived on assetId: {} and semantic topic: {}", deviceId, appTopic);
	}

	@Override
	public void onMessagePublished(int messageId, String appTopic) {
		s_logger.info("Published message with ID: {} on application topic: {}", messageId, appTopic);
	}

	@Override
	public void onMessageConfirmed(int messageId, String appTopic) {
		s_logger.info("Confirmed message with ID: {} on application topic: {}", messageId, appTopic);
	}
	
	// ----------------------------------------------------------------
	//
	//   Private Methods
	//
	// ----------------------------------------------------------------

	/**
	 * Called after a new set of properties has been configured on the service
	 */
	private void doUpdate() 
	{
		// cancel a current worker handle if one if active
		if (m_handle != null) {
			m_handle.cancel(true);
		}
		
		if (!m_properties.containsKey(TEMP_INITIAL_PROP_NAME) ||
		    !m_properties.containsKey(PUBLISH_RATE_PROP_NAME)) {
			s_logger.info("Update ExamplePublisher - Ignore as properties do not contain TEMP_INITIAL_PROP_NAME and PUBLISH_RATE_PROP_NAME.");
			return;
		}
		
		// reset the temperature to the initial value
		m_temperature = (Float) m_properties.get(TEMP_INITIAL_PROP_NAME);		
		
		// schedule a new worker based on the properties of the service
		int pubrate = (Integer) m_properties.get(PUBLISH_RATE_PROP_NAME);
		m_handle = m_worker.scheduleAtFixedRate(new Runnable() {		
			@Override
			public void run() {
				doPublish();
			}
		}, 0, pubrate, TimeUnit.MILLISECONDS);
	}
	
	
	/**
	 * Called at the configured rate to publish the next temperature measurement.
	 */
	private void doPublish() 
	{				
		// fetch the publishing configuration from the publishing properties
		String  topic  = (String) m_properties.get(PUBLISH_TOPIC_PROP_NAME);
		Integer qos    = (Integer) m_properties.get(PUBLISH_QOS_PROP_NAME);
		Boolean retain = (Boolean) m_properties.get(PUBLISH_RETAIN_PROP_NAME);
		
		// Increment the simulated temperature value
		float tempIncr = (Float) m_properties.get(TEMP_INCREMENT_PROP_NAME);
		m_temperature += tempIncr;
				
		// Allocate a new payload
		KuraPayload payload = new KuraPayload();
		
		// Timestamp the message
		payload.setTimestamp(new Date());
		
		// Add the temperature as a metric to the payload
		payload.addMetric("temperature", m_temperature);

		// add all the other metrics
		for (String metric : METRIC_PROP_NAMES) {
			if ("metric.char".equals(metric)) {			
				// publish character as a string as the 
				// "char" type is not support in the EDC Payload
				payload.addMetric(metric, String.valueOf(m_properties.get(metric)));
			}
			else if ("metric.short".equals(metric)) {
				// publish short as an integer as the 
				// "short " type is not support in the EDC Payload
				payload.addMetric(metric, ((Short) (m_properties.get(metric))).intValue());
			}
			else if ("metric.byte".equals(metric)) {
				// publish byte as an integer as the 
				// "byte" type is not support in the EDC Payload
				payload.addMetric(metric, ((Byte) (m_properties.get(metric))).intValue());
			}
			else {
				payload.addMetric(metric, m_properties.get(metric));
			}
		}
		
		// Publish the message
		try {
			int messageId = m_cloudClient.publish(topic, payload, qos, retain);
			s_logger.info("Published to {} message: {} with ID: {}", new Object[] {topic, payload, messageId});
		} 
		catch (Exception e) {
			s_logger.error("Cannot publish topic: "+topic, e);
		}
	}
}
