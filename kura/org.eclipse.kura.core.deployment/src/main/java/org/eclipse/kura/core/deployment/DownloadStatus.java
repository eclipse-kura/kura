/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.deployment;

/**
 * Enum representing the different status of the download process
 *
 * {@link DownloadStatus.DOWNLOAD_STATUS.PROGRESS} Download in
 * progress {@link DownloadStatus.DOWNLOAD_STATUS.COMPLETE} Download
 * completed {@link DownloadStatus.DOWNLOAD_STATUS.FAILED} Download
 * failed
 */
public enum DownloadStatus {
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    ALREADY_DONE("ALREADY DONE"),
    CANCELLED("CANCELLED");

    private final String status;

    DownloadStatus(String status) {
        this.status = status;
    }

    public String getStatusString() {
        return this.status;
    }
}