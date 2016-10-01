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
package org.eclipse.kura.core.data;

import java.util.Arrays;
import java.util.Date;

/**
 * DataMessage is a message that is currently transiting through the DataService.
 * It is a wrapper class over the message payload as produced by the client application
 * but it also capture all the state information of the message through its
 * DataService life-cycle of: stored -> published -> confirmed.
 */
public class DataMessage {

    private int id;
    private String topic;
    private int qos;
    private boolean retain;
    private Date createdOn;
    private Date publishedOn;
    private int publishedMessageId;
    private Date confirmedOn;
    private byte[] payload;
    private int priority;
    private String sessionId;
    private Date droppedOn;

    public DataMessage() {
    }

    public DataMessage(Builder b) {
        this.id = b.id;
        this.topic = b.topic;
        this.qos = b.qos;
        this.retain = b.retain;
        this.createdOn = b.createdOn;
        this.publishedOn = b.publishedOn;
        this.publishedMessageId = b.publishedMessageId;
        ;
        this.confirmedOn = b.confirmedOn;
        this.payload = b.payload;
        this.priority = b.priority;
        this.sessionId = b.sessionId;
        this.droppedOn = b.droppedOn;
    }

    public int getId() {
        return this.id;
    }

    public String getTopic() {
        return this.topic;
    }

    public int getQos() {
        return this.qos;
    }

    public boolean isRetain() {
        return this.retain;
    }

    public Date getCreatedOn() {
        return this.createdOn;
    }

    public Date getPublishedOn() {
        return this.publishedOn;
    }

    public int getPublishedMessageId() {
        return this.publishedMessageId;
    }

    public void setPublishedMessageId(int publishedMessageId) {
        this.publishedMessageId = publishedMessageId;
    }

    public Date getConfirmedOn() {
        return this.confirmedOn;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public int getPriority() {
        return this.priority;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public Date droppedOn() {
        return this.droppedOn;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("DataMessage [id=").append(this.id).append(", topic=")
                .append(this.topic).append(", qos=").append(this.qos).append(", retain=").append(this.retain)
                .append(", createdOn=").append(this.createdOn).append(", publishedOn=").append(this.publishedOn)
                .append(", publishedMessageId=").append(this.publishedMessageId).append(", confirmedOn=")
                .append(this.confirmedOn).append(", payload=").append(Arrays.toString(this.payload))
                .append(", priority=").append(this.priority).append(", sessionId=").append(this.sessionId)
                .append(", droppedOn=").append(this.droppedOn).append("]");

        return builder.toString();
    }

    public static class Builder {

        private final int id;
        private String topic;
        private int qos;
        private boolean retain;
        private Date createdOn;
        private Date publishedOn;
        private int publishedMessageId;
        private Date confirmedOn;
        private byte[] payload;
        private int priority;
        private String sessionId;
        private Date droppedOn;

        public Builder(int id) {
            this.id = id;
        }

        public Builder withTopic(String topic) {
            this.topic = topic;
            return this;
        }

        public Builder withQos(int qos) {
            this.qos = qos;
            return this;
        }

        public Builder withRetain(boolean retain) {
            this.retain = retain;
            return this;
        }

        public Builder withCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public Builder withPublishedOn(Date publishedOn) {
            this.publishedOn = publishedOn;
            return this;
        }

        public Builder withPublishedMessageId(int publishedMessageId) {
            this.publishedMessageId = publishedMessageId;
            return this;
        }

        public Builder withConfirmedOn(Date confirmedOn) {
            this.confirmedOn = confirmedOn;
            return this;
        }

        public Builder withPayload(byte[] payload) {
            this.payload = payload;
            return this;
        }

        public Builder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder withSessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder withDroppedOn(Date droppedOn) {
            this.droppedOn = droppedOn;
            return this;
        }

        public int getId() {
            return this.id;
        }

        public String getTopic() {
            return this.topic;
        }

        public int getQos() {
            return this.qos;
        }

        public boolean getRetain() {
            return this.retain;
        }

        public Date getCreatedOn() {
            return this.createdOn;
        }

        public Date getPublishedOn() {
            return this.publishedOn;
        }

        public int getPublishedMessageId() {
            return this.publishedMessageId;
        }

        public Date getConfirmedOn() {
            return this.confirmedOn;
        }

        public byte[] getPayload() {
            return this.payload;
        }

        public int getPriority() {
            return this.priority;
        }

        public String getSessionId() {
            return this.sessionId;
        }

        public Date getDroppedOn() {
            return this.droppedOn;
        }

        public DataMessage build() {
            return new DataMessage(this);
        }
    }
}
