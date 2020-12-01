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
 ******************************************************************************/
package org.eclipse.kura.deployment.hook;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This class provides some context information describing a DEPLOY-V2 request.
 *
 * @since 1.3
 */
@ProviderType
public class RequestContext {

    private final String downloadFilePath;
    private final String requestType;

    /**
     * Creates a new {@link RequestContext} instance.
     *
     * @param downloadFilePath
     *            the path of the downloaded file
     * @param requestType
     *            the value of the {@code request.type} metric contained in the request
     */
    public RequestContext(String downloadFilePath, String requestType) {
        this.downloadFilePath = downloadFilePath;
        this.requestType = requestType;
    }

    /**
     * Returns the path of the downloaded file.
     *
     * @return the path of the downloaded file
     */
    public String getDownloadFilePath() {
        return this.downloadFilePath;
    }

    /**
     * Returns the value of the {@code request.type} metric of the request.
     *
     * @return the value of the {@code request.type} metric
     */
    public String getRequestType() {
        return this.requestType;
    }
}
