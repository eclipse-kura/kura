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
package org.eclipse.kura.core.data;

import java.util.Arrays;
import java.util.Date;

/**
 * DataMessage is a message that is currently transiting through the DataService.
 * It is a wrapper class over the message payload as produced by the client application
 * but it also capture all the state information of the message through its
 * DataService life-cycle of: stored -> published -> confirmed.
 */
public class DataMessage 
{
	private int  	id;
	private String  topic;
	private int     qos;
	private boolean retain;
	private Date    createdOn;
	private Date   	publishedOn;
    private int     publishedMessageId;
	private Date    confirmedOn;
	private byte[]  payload;
	private int     priority;
	private String  sessionId;
	private Date    droppedOn;
		
	public DataMessage()
	{}
	
	public DataMessage(Builder b) {
		id                 = b.id;
		topic              = b.topic;
		qos                = b.qos;
		retain             = b.retain;
		createdOn          = b.createdOn;	
		publishedOn        = b.publishedOn;
		publishedMessageId = b.publishedMessageId;;
		confirmedOn        = b.confirmedOn;
		payload            = b.payload;
		priority           = b.priority;
		sessionId          = b.sessionId;
		droppedOn          = b.droppedOn;
	}

	public int getId() {
		return id;
	}

	public String getTopic() {
		return topic;
	}

	public int getQos() {
		return qos;
	}

	public boolean isRetain() {
		return retain;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public Date getPublishedOn() {
		return publishedOn;
	}

    public int getPublishedMessageId() {
        return publishedMessageId;
    }

    public void setPublishedMessageId(int publishedMessageId) {
        this.publishedMessageId = publishedMessageId;
    }

    public Date getConfirmedOn() {
		return confirmedOn;
	}

	public byte[] getPayload() {
		return payload;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public Date droppedOn() {
		return this.droppedOn;
	}

	@Override
	public String toString() {
		return "DataMessage [id=" + id + ", topic=" + topic + ", qos=" + qos
				+ ", retain=" + retain + ", createdOn=" + createdOn
				+ ", publishedOn=" + publishedOn + ", publishedMessageId="
				+ publishedMessageId + ", confirmedOn=" + confirmedOn
				+ ", payload=" + Arrays.toString(payload) + ", priority="
				+ priority + ", sessionId=" 
				+ sessionId + ", droppedOn="
				+ droppedOn + "]";
	}

	public static class Builder {

		private int		   id;
		private String     topic;
		private int        qos;
		private boolean    retain;
		private Date       createdOn;
		private Date   	   publishedOn;
        private int        publishedMessageId;
		private Date       confirmedOn;
		private byte[]	   payload;
		private int        priority;
		private String     sessionId;
		private Date       droppedOn;
		
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
			return id;
		}

		public String getTopic() {
			return topic;
		}

		public int getQos() {
			return qos;
		}

		public boolean getRetain() {
			return retain;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public Date getPublishedOn() {
			return publishedOn;
		}

        public int getPublishedMessageId() {
            return publishedMessageId;
        }

        public Date getConfirmedOn() {
			return confirmedOn;
		}

		public byte[] getPayload() {
			return payload;
		}
		
		public int getPriority() {
			return priority;
		}
		
		public String getSessionId() {
			return sessionId;
		}
		
		public Date getDroppedOn() {
			return droppedOn;
		}
 		
		public DataMessage build() {			
			return new DataMessage(this);
		}
	}
}
