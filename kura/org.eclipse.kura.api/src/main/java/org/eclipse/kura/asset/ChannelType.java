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
package org.eclipse.kura.asset;

/**
 * This provides the necessary constants to denote the type of the channel
 * (whether the channel is for reading or writing or both)
 */
public enum ChannelType {

	/**
	 * The channel will be used for performing reading operation
	 */
	READ,

	/**
	 * The channel will be used for performing reading and writing operation
	 */
	READ_WRITE,

	/**
	 * The channel will be used for performing writing operation
	 */
	WRITE
}
