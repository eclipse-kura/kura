/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
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

import java.io.OutputStream;

import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.ssl.SslManagerService;

public class DownloadOptions {

    private OutputStream out;
    private DeploymentPackageDownloadOptions options;
    private ProgressListener callback;
    private SslManagerService sslManagerService;
    private String downloadURL;
    private int alreadyDownloaded;
    private long maxDownloadSizeBytes = CloudDeploymentHandlerV2.MAX_DOWNLOAD_SIZE_DEFAULT;

    public OutputStream getOut() {
        return this.out;
    }

    public void setOut(OutputStream out) {
        this.out = out;
    }

    public DeploymentPackageDownloadOptions getRequestOptions() {
        return this.options;
    }

    public void setRequestOptions(DeploymentPackageDownloadOptions options) {
        this.options = options;
    }

    public ProgressListener getCallback() {
        return this.callback;
    }

    public void setCallback(ProgressListener callback) {
        this.callback = callback;
    }

    public SslManagerService getSslManagerService() {
        return this.sslManagerService;
    }

    public void setSslManagerService(SslManagerService sslManagerService) {
        this.sslManagerService = sslManagerService;
    }

    public String getDownloadURL() {
        return this.downloadURL;
    }

    public void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
    }

    public int getAlreadyDownloaded() {
        return this.alreadyDownloaded;
    }

    public void setAlreadyDownloaded(int alreadyDownloaded) {
        this.alreadyDownloaded = alreadyDownloaded;
    }

    public long getMaxDownloadSizeBytes() {
        return this.maxDownloadSizeBytes;
    }

    public void setMaxDownloadSizeBytes(long maxDownloadSize) {
        this.maxDownloadSizeBytes = maxDownloadSize;
    }
}
