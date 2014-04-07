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
package org.eclipse.kura.message;

/**
 * Models a topic for messages posted to the Kura platform.
 * Topic are expected to be in the form of "account/asset/<application_specific>";
 * system topic starts with the $EDC account. 
 */
public class KuraTopic 
{	
	private String   m_fullTopic;
	private String[] m_topicParts;
	private String   m_prefix;
	private String   m_accountName;
	private String   m_deviceId;
	private String   m_applicationId;
	private String   m_applicationTopic;

	public KuraTopic(String fullTopic) 
	{	    
		m_fullTopic = fullTopic;		
		if(fullTopic.compareTo("#") == 0) {
			return;
		}
		
		m_topicParts = fullTopic.split("/");		
		if (m_topicParts.length == 0) {
			return;
		}
		
		// prefix
		int index  = 0;
		int offset = 0; // skip a slash
		if (m_topicParts[0].startsWith("$")) {			
			m_prefix = m_topicParts[index];
			offset += m_prefix.length()+1;
			index++;
		}
		
		// account name
		if (index < m_topicParts.length) {
			m_accountName = m_topicParts[index];
			offset += m_accountName.length()+1;
			index++;
		}

		// deviceId
		if (index < m_topicParts.length) {
			m_deviceId = m_topicParts[index];
			offset += m_deviceId.length()+1;
			index++;
		}

		// applicationId
		if (index < m_topicParts.length) {
			m_applicationId = m_topicParts[index];
			offset += m_applicationId.length()+1;
			index++;
		}
		
		// applicationTopic
		if (offset < m_fullTopic.length()) {
			m_applicationTopic = m_fullTopic.substring(offset);
		}
	}

	public String getFullTopic() {
		return m_fullTopic;
	}

	public String[] getTopicParts() {
		return m_topicParts;
	}

	public String getPrefix() {
		return m_prefix;
	}

	public String getAccountName() {
		return m_accountName;
	}

	public String getDeviceId() {
		return m_deviceId;
	}

	public String getApplicationId() {
		return m_applicationId;
	}

	public String getApplicationTopic() {
		return m_applicationTopic;
	}
	
/*	
	public static void main(String argv[]) 
	{
		KuraTopic topic = new KuraTopic("$EDC/edcguest/68:A8:6D:27:B4:B0/DEPLOY-V1/GET/packages");
		System.err.println(topic.getPrefix());
		System.err.println(topic.getAccountName());
		System.err.println(topic.getDeviceId());
		System.err.println(topic.getApplicationId());
		System.err.println(topic.getApplicationTopic());

		KuraTopic topic1 = new KuraTopic("edcguest/68:A8:6D:27:B4:B0/app/appTopic1/appTopic2");
		System.err.println(topic1.getPrefix());
		System.err.println(topic1.getAccountName());
		System.err.println(topic1.getDeviceId());
		System.err.println(topic1.getApplicationId());
		System.err.println(topic1.getApplicationTopic());
	}
*/
}
