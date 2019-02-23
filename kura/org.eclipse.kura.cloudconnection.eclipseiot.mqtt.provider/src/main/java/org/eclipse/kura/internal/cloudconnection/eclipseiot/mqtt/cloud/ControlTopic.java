package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud;

import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageType;
import org.eclipse.kura.message.KuraApplicationTopic;

public class ControlTopic extends KuraApplicationTopic {

    private String fullTopic;
    private String[] topicParts;
    private String prefix;
    private String accountName;
    private String deviceId;
    private String req;
    private String reqId;

    public ControlTopic(String fullTopic) {
        this(fullTopic, MessageType.CONTROL.getTopicPrefix());
    }
    
    public ControlTopic(String fullTopic, String controlPrefix) {
        this.fullTopic = fullTopic;
        if (fullTopic.compareTo("#") == 0) {
            return;
        }

        this.topicParts = fullTopic.split("/");
        if (this.topicParts.length == 0) {
            return;
        }

        // prefix
        int index = 0;
        int offset = 0; // skip a slash
        if (this.topicParts[0].startsWith(controlPrefix)) {
            this.prefix = this.topicParts[index];
            offset += this.prefix.length() + 1;
            index++;
        }

        // account name
        if (index < this.topicParts.length) {
            this.accountName = this.topicParts[index];
            offset += this.accountName.length() + 1;
            index++;
        }

        // deviceId
        if (index < this.topicParts.length) {
            this.deviceId = this.topicParts[index];
            offset += this.deviceId.length() + 1;
            index++;
        }

        // req
        // to skip?
        if (index < this.topicParts.length) {
            this.req = this.topicParts[index];
            offset += this.req.length() + 1;
            index++;
        }

        //reqId
        if (index < this.topicParts.length) {
            this.reqId = this.topicParts[index];
            offset += this.reqId.length() + 1;
            index++;
        }
        
        // applicationId
        if (index < this.topicParts.length) {
            this.applicationId = this.topicParts[index];
            offset += this.applicationId.length() + 1;
            index++;
        }
        if (offset < this.fullTopic.length()) {
            this.applicationTopic = this.fullTopic.substring(offset);
        }
    }

    public String getFullTopic() {
        return this.fullTopic;
    }

    public String[] getTopicParts() {
        return this.topicParts;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getAccountName() {
        return this.accountName;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public String getReqId() {
        return this.reqId;
    }
}
