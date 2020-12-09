/*******************************************************************************
 * Copyright (c) 2011, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.camel.camelcloud;

import org.eclipse.kura.cloud.CloudService;

/**
 * An extension interface adding Camel specific functionality to the {@link CloudService} interface
 */
public interface CamelCloudService extends CloudService {

    void registerBaseEndpoint(String applicationId, String baseEndpoint);

    void release(String applicationId);

}
