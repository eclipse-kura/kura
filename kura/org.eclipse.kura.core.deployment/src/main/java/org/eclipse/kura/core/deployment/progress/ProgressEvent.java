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

package org.eclipse.kura.core.deployment.progress;

import java.util.EventObject;

import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;

public class ProgressEvent extends EventObject {

    /**
     *
     */
    private static final long serialVersionUID = -4316652505853478843L;

    String clientId;
    /**
     * Size in B.
     */
    int transferSize;
    /**
     * Progress in %.
     */
    int transferProgress;
    String transferStatus;
    String requesterClientId;
    long jobId;
    String exceptionMessage = null;
    /**
     * Already downloaded/missing downloads.
     */
    int downloadIndex;

    public ProgressEvent(Object source, DeploymentPackageDownloadOptions options, int transferSize,
            int transferProgress, String trasnferStatus, int downloadIndex) {
        super(source);
        this.clientId = options.getClientId();
        this.transferSize = transferSize;
        this.transferProgress = transferProgress;
        this.transferStatus = trasnferStatus;
        this.requesterClientId = options.getRequestClientId();
        this.jobId = options.getJobId();
        this.downloadIndex = downloadIndex;
    }

    public String getClientId() {
        return this.clientId;
    }

    public int getTransferSize() {
        return this.transferSize;
    }

    public int getTransferProgress() {
        return this.transferProgress;
    }

    public String getTransferStatus() {
        return this.transferStatus;
    }

    public String getRequesterClientId() {
        return this.requesterClientId;
    }

    public long getJobId() {
        return this.jobId;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionMessage() {
        return this.exceptionMessage;
    }

    public void setDownloadIndex(int downloadIndex) {
        this.downloadIndex = downloadIndex;
    }

    public int getDownloadIndex() {
        return this.downloadIndex;
    }
}
