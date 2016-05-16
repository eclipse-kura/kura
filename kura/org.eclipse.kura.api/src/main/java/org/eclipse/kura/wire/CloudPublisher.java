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

import java.util.List;

import org.eclipse.kura.KuraRuntimeException;

/**
 * A wire Component interface responsible for publishing the provided wire
 * records to the configured cloud platform
 */
public interface CloudPublisher {

	/**
	 * Publishes the provided list of wire records to the configured cloud
	 * platform
	 *
	 * @param wireRecords
	 *            the wire records to be published
	 * @throws KuraRuntimeException
	 *             if argument is null or empty
	 */
	public void publish(List<WireRecord> wireRecords) throws KuraRuntimeException;

}
