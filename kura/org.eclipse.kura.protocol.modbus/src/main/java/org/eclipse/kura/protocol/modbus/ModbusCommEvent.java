/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.protocol.modbus;

/**
 * The ModbusCommEvent class contains the values returned by Modbus functions 11(0x0B)
 *  and 12(0x0C).
 *  <p>
 *  <li>status : two-bytes status word, 0xFFFF if a busy condition exists, 0 otherwise
 *  <li>eventCount : event counter incremented for each successful message completion
 *  <li>messageCount : quantity of messages processed since last restart
 *  <li>events[] : 0 to 64 bytes, each byte corresponding to the status of one Modbus 
 *  send or receive operation, byte 0 is the most recent event.
 */
public class ModbusCommEvent {
	private int status;
	private int eventCount;
	private int messageCount;
	private int[] events;

	public ModbusCommEvent(){
		status=0;
		eventCount=0;
		messageCount=0;
		events=null;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public int getEventCount() {
		return eventCount;
	}
	public void setEventCount(int eventCount) {
		this.eventCount = eventCount;
	}
	public int getMessageCount() {
		return messageCount;
	}
	public void setMessageCount(int messageCount) {
		this.messageCount = messageCount;
	}
	public int[] getEvents() {
		return events;
	}
	public void setEvents(int[] events) {
		this.events = events;
	}
}
