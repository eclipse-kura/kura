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
