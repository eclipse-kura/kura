/*******************************************************************************
 * Copyright (c) 2011, 2016 Red Hat and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.camel.camelcloud;

import org.eclipse.kura.cloud.CloudService;

public interface CamelCloudService extends CloudService {

    void registerBaseEndpoint(String applicationId, String baseEndpoint);

    void release(String applicationId);

}
