/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.channel;

/**
 * This represents all the Channel specific flag codes
 * 
 * @since 1.2
 */
public enum ChannelFlag {
    /**
     * In case of any failure on the channel
     */
    FAILURE,
    /**
     * In case of successful operation on channel
     */
    SUCCESS
}
