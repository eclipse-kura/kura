/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 ******************************************************************************/
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
