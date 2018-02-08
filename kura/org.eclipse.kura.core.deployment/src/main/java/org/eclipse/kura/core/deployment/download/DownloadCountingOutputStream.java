/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
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

import java.io.IOException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.DownloadStatus;

public interface DownloadCountingOutputStream {

    public void cancelDownload() throws Exception;

    public void startWork() throws KuraException;

    public DownloadStatus getDownloadTransferStatus();

    public Long getDownloadTransferProgressPercentage();

    public Long getTotalBytes();

    public void setTotalBytes(long totalBytes);

    public void close() throws IOException;
}
