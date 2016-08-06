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
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.wire;

/**
 * The SeverityLevel Enum signifies a severity level of a {@link WireField}.
 * Depending on the configuration as provided by different Wire Components, the
 * Wire Components will choose the specific Wire Fields that will be processed.
 * <br/>
 * <br/>
 *
 * The priority of the Severity Levels are as follows: ERROR > CONFIG > INFO
 */
public enum SeverityLevel {

	/**
	 * CONFIG is a Wire Field level for notifying configuration oriented status.
	 * <p>
	 * CONFIG Wire Fields are intended to provide a variety of configuration
	 * information, for instance, {@code TimerWireField} is a configuration
	 * oriented Wire Field
	 */
	CONFIG,

	/**
	 * ERROR is a Wire Field level indicating a serious failure or exception.
	 * <p>
	 * In general SEVERE messages should describe events that are of
	 * considerable importance and which will prevent normal program execution.
	 * They should be reasonably intelligible to end users and to system
	 * administrators.
	 */
	ERROR,

	/**
	 * INFO is a message level for informational messages.
	 * <p>
	 * Typically INFO messages will be delegated to the connected wire
	 * components
	 */
	INFO

}
