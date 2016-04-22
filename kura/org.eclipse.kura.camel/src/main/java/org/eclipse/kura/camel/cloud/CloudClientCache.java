/*******************************************************************************
 * Copyright (c) 2011, 2016 Red Hat and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.camel.cloud;

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;

public interface CloudClientCache {

    void put(String appId, CloudClient cloudClient);

    CloudClient get(String appId);

    CloudClient getOrCreate(String appId, CloudService cloudService);

}
