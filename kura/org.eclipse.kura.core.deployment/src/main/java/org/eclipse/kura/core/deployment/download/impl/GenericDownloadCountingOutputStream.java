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
package org.eclipse.kura.core.deployment.download.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.CountingOutputStream;
import org.eclipse.kura.core.deployment.DownloadStatus;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadOptions;
import org.eclipse.kura.core.deployment.progress.ProgressEvent;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.ssl.SslManagerService;

public class GenericDownloadCountingOutputStream extends CountingOutputStream {

    private int propResolution;
    private int propBufferSize;
    private int propConnectTimeout = 5000;
    private int propReadTimeout = 6000;
    private int propBlockDelay = 1000;

    long totalBytes;

    final DeploymentPackageDownloadOptions options;
    final SslManagerService sslManagerService;
    final ProgressListener pl;
    final int alreadyDownloaded;
    final String downloadURL;

    InputStream is = null;

    private long currentStep = 1;
    private DownloadStatus downloadStatus = DownloadStatus.FAILED;

    public GenericDownloadCountingOutputStream(DownloadOptions downloadOptions) {
        super(downloadOptions.getOut());
        this.options = downloadOptions.getRequestOptions();
        this.sslManagerService = downloadOptions.getSslManagerService();
        this.pl = downloadOptions.getCallback();
        this.downloadURL = downloadOptions.getDownloadURL();
        this.alreadyDownloaded = downloadOptions.getAlreadyDownloaded();
    }

    public DownloadStatus getDownloadTransferStatus() {
        return this.downloadStatus;
    }

    public Long getDownloadTransferProgressPercentage() {
        Long percentage = (long) Math
                .floor(((Long) getByteCount()).doubleValue() / ((Long) this.totalBytes).doubleValue() * 100);
        if (percentage < 0) {
            return (long) 50;
        }
        return percentage;
    }

    public Long getTotalBytes() {
        return this.totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    @Override
    protected void afterWrite(int n) throws IOException {
        super.afterWrite(n);
        if (this.propResolution == 0 && getTotalBytes() > 0) {
            this.propResolution = Math.round(this.totalBytes / 100F * 5F);
        } else if (this.propResolution == 0) {
            this.propResolution = 1024 * 256;
        }
        if (getByteCount() >= this.currentStep * this.propResolution) {
            this.currentStep++;
            postProgressEvent(this.options.getClientId(), getByteCount(), this.totalBytes, DownloadStatus.IN_PROGRESS,
                    null);
        }
        try {
            Thread.sleep(this.propBlockDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void postProgressEvent(String clientId, long progress, long total, DownloadStatus status,
            String errorMessage) {
        Long perc = getDownloadTransferProgressPercentage();
        this.downloadStatus = status;
        ProgressEvent pe = new ProgressEvent(this, this.options, ((Long) total).intValue(), perc.intValue(),
                getDownloadTransferStatus().getStatusString(), this.alreadyDownloaded);
        if (errorMessage != null) {
            pe.setExceptionMessage(errorMessage);
        }
        this.pl.progressChanged(pe);

    }

    protected void setResolution(int resolution) {
        this.propResolution = resolution;
    }

    protected void setBufferSize(int size) {
        this.propBufferSize = size;
    }

    protected void setConnectTimeout(int timeout) {
        this.propConnectTimeout = timeout;
    }

    protected void setReadTimeout(int timeout) {
        this.propReadTimeout = timeout;
    }

    protected void setBlockDelay(int delay) {
        this.propBlockDelay = delay;
    }

    protected int getResolution() {
        return this.propResolution;
    }

    protected int getBufferSize() {
        return this.propBufferSize;
    }

    protected int getConnectTimeout() {
        return this.propConnectTimeout;
    }

    protected int getPropReadTimeout() {
        return this.propReadTimeout;
    }

    protected int getPropBlockDelay() {
        return this.propBlockDelay;
    }
}
