/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/

package org.eclipse.kura.core.deployment.download;

import org.eclipse.kura.core.deployment.download.impl.HttpDownloadCountingOutputStream;

public class DownloadFactory {

    private static final String DOWNLOAD_PROTOCOL_HTTP = "HTTP";

    public static DownloadCountingOutputStream getDownloadInstance(String protocol, DownloadOptions downloadOptions) {
        if (protocol.equals(DOWNLOAD_PROTOCOL_HTTP)) {
            return new HttpDownloadCountingOutputStream(downloadOptions);
        }
        return null;
    }

}
