/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.wire;

/**
 * The SeverityLevel Enum signifies a severity level of a {@link WireField}.
 * Depending on the configuration as provided by different Wire Components, the
 * Wire Components will choose the specific Wire Fields that will be processed.
 * <br/>
 * <br/>
 *
 * <p>
 * The Severity levels in descending order are:
 * <ul>
 * <li>{@code SEVERE} (highest value)
 * <li>{@code CONFIG}
 * <li>{@code INFO} or {@code ERROR} (lowest value)
 * </ul>
 *
 * The priority of the Severity Levels are as follows: {@code SEVERE} > {@code CONFIG}
 * > ({@code INFO} or {@code ERROR})
 * <br/>
 * <br/>
 *
 * Also note that, {@code INFO} and {@code ERROR} have the same priority level.
 */
public enum SeverityLevel {

    /**
     * CONFIG is a Wire Field level for notifying configuration oriented status.
     * <p>
     * CONFIG Wire Fields are intended to provide a variety of configuration
     * information, for instance, {@code Timer} Wire Field is a configuration
     * oriented Wire Field
     */
    CONFIG,

    /**
     * ERROR is a Wire Field level indicating a failure or exception.
     */
    ERROR,

    /**
     * INFO is a message level for informational messages.
     * <p>
     * Typically INFO messages will be delegated to the connected wire
     * components
     */
    INFO,

    /**
     * SEVERE is a Wire Field level indicating a serious failure or exception.
     * <p>
     * In general SEVERE messages should describe events that are of most
     * importance and which will prevent normal program execution. They should
     * be reasonably intelligible to end users and to system administrators.
     */
    SEVERE

}
