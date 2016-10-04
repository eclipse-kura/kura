/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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
