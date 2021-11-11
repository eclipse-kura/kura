/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/

package org.eclipse.kura.log;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The LogReader interface is implemented by all the services responsible to read logs from the system, filesystem or
 * processes running on the system.
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 2.3
 */
@ProviderType
public interface LogReader extends LogProvider {

}
