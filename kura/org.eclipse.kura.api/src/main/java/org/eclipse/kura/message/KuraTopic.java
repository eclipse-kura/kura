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
package org.eclipse.kura.message;

/**
 * Models a topic for messages posted to the Kura platform.
 * Topic are expected to be in the form of "account/asset/<application_specific>";
 * system topic starts with the $EDC account.
 */
public class KuraTopic {

    private String m_fullTopic;
    private String[] m_topicParts;
    private String m_prefix;
    private String m_accountName;
    private String m_deviceId;
    private String m_applicationId;
    private String m_applicationTopic;

    public KuraTopic(String fullTopic) {
        this(fullTopic, "$");
    }
    
    public KuraTopic(String fullTopic, String controlPrefix) {
        this.m_fullTopic = fullTopic;
        if (fullTopic.compareTo("#") == 0) {
            return;
        }

        this.m_topicParts = fullTopic.split("/");
        if (this.m_topicParts.length == 0) {
            return;
        }

        // prefix
        int index = 0;
        int offset = 0; // skip a slash
        if (this.m_topicParts[0].startsWith(controlPrefix)) {
            this.m_prefix = this.m_topicParts[index];
            offset += this.m_prefix.length() + 1;
            index++;
        }

        // account name
        if (index < this.m_topicParts.length) {
            this.m_accountName = this.m_topicParts[index];
            offset += this.m_accountName.length() + 1;
            index++;
        }

        // deviceId
        if (index < this.m_topicParts.length) {
            this.m_deviceId = this.m_topicParts[index];
            offset += this.m_deviceId.length() + 1;
            index++;
        }

        // applicationId
        if (index < this.m_topicParts.length) {
            this.m_applicationId = this.m_topicParts[index];
            offset += this.m_applicationId.length() + 1;
            index++;
        }

        // applicationTopic
        if (offset < this.m_fullTopic.length()) {
            this.m_applicationTopic = this.m_fullTopic.substring(offset);
        }
    }

    public String getFullTopic() {
        return this.m_fullTopic;
    }

    public String[] getTopicParts() {
        return this.m_topicParts;
    }

    public String getPrefix() {
        return this.m_prefix;
    }

    public String getAccountName() {
        return this.m_accountName;
    }

    public String getDeviceId() {
        return this.m_deviceId;
    }

    public String getApplicationId() {
        return this.m_applicationId;
    }

    public String getApplicationTopic() {
        return this.m_applicationTopic;
    }

    /*
     * public static void main(String argv[])
     * {
     * KuraTopic topic = new KuraTopic("$EDC/edcguest/68:A8:6D:27:B4:B0/DEPLOY-V1/GET/packages");
     * System.err.println(topic.getPrefix());
     * System.err.println(topic.getAccountName());
     * System.err.println(topic.getDeviceId());
     * System.err.println(topic.getApplicationId());
     * System.err.println(topic.getApplicationTopic());
     *
     * KuraTopic topic1 = new KuraTopic("edcguest/68:A8:6D:27:B4:B0/app/appTopic1/appTopic2");
     * System.err.println(topic1.getPrefix());
     * System.err.println(topic1.getAccountName());
     * System.err.println(topic1.getDeviceId());
     * System.err.println(topic1.getApplicationId());
     * System.err.println(topic1.getApplicationTopic());
     * }
     */
}
