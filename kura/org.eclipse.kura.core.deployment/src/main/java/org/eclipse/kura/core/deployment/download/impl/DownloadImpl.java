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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.CancellationException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.DownloadStatus;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadCountingOutputStream;
import org.eclipse.kura.core.deployment.download.DownloadFactory;
import org.eclipse.kura.core.deployment.download.DownloadFileUtilities;
import org.eclipse.kura.core.deployment.download.DownloadOptions;
import org.eclipse.kura.core.deployment.install.DeploymentPackageInstallOptions;
import org.eclipse.kura.core.deployment.progress.ProgressEvent;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.core.deployment.util.FileUtilities;
import org.eclipse.kura.core.deployment.util.HashUtil;
import org.eclipse.kura.deployment.hook.DeploymentHook;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.ssl.SslManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadImpl implements ProgressListener {

    private static final Logger s_logger = LoggerFactory.getLogger(DownloadImpl.class);
    public static final String RESOURCE_DOWNLOAD = "download";

    private final CloudDeploymentHandlerV2 callback;
    private final DeploymentPackageDownloadOptions options;
    private DownloadCountingOutputStream downloadHelper;
    private SslManagerService sslManagerService;
    private boolean alreadyDownloadedFlag;
    private String verificationDirectory;

    public DownloadImpl(DeploymentPackageDownloadOptions options, CloudDeploymentHandlerV2 callback) {
        this.options = options;
        this.callback = callback;
    }

    // ----------------------------------------------------------------
    //
    // Public methods
    //
    // ----------------------------------------------------------------

    public DownloadCountingOutputStream getDownloadHelper() {
        return this.downloadHelper;
    }

    public void setSslManager(SslManagerService sslManager) {
        this.sslManagerService = sslManager;
    }

    public void setAlreadyDownloadedFlag(boolean alreadyDownloaded) {
        this.alreadyDownloadedFlag = alreadyDownloaded;
    }

    public void setVerificationDirectory(String verificationDirectory) {
        this.verificationDirectory = verificationDirectory;
    }

    @Override
    public void progressChanged(ProgressEvent progress) {

        s_logger.info("{}% downloaded", progress.getTransferProgress());

        KuraNotifyPayload notify = new KuraNotifyPayload(progress.getClientId());
        notify.setTimestamp(new Date());
        notify.setTransferSize(progress.getTransferSize());
        notify.setTransferProgress(progress.getTransferProgress());
        notify.setTransferStatus(progress.getTransferStatus());
        notify.setJobId(progress.getJobId());
        if (progress.getExceptionMessage() != null) {
            notify.setErrorMessage(progress.getExceptionMessage());
        }

        notify.setTransferIndex(progress.getDownloadIndex());

        this.callback.publishMessage(this.options, notify, RESOURCE_DOWNLOAD);
    }

    public void downloadDeploymentPackageInternal() throws KuraException {
        File dpFile = null;
        int downloadIndex = 0;
        boolean downloadSuccess = true;
        try {
            // Download the package to a temporary file.
            // Check for file existence has already been done
            dpFile = DownloadFileUtilities.getDpDownloadFile(this.options);
            boolean forceDownload = this.options.isDownloadForced();

            if (!this.alreadyDownloadedFlag || forceDownload) {
                s_logger.info("To download");
                incrementalDownloadFromURL(dpFile, this.options.getDeployUri(), downloadIndex);
                downloadIndex++;

                if (this.options.getVerifierURL() != null) {
                    File dpVerifier = getDpVerifierFile(this.options);
                    incrementalDownloadFromURL(dpVerifier, this.options.getVerifierURL(), downloadIndex);
                }
            } else {
                alreadyDownloadedAsync();
            }
        } catch (CancellationException ce) {
            s_logger.error("Download exception", ce);
            downloadSuccess = false;
        } catch (Exception e) {
            s_logger.info("Download exception", e);
            downloadSuccess = false;
            downloadFailedAsync(downloadIndex, null);
        }

        final DeploymentHook hook = this.options.getDeploymentHook();

        if (hook != null) {
            try {
                hook.postDownload(this.options.getHookRequestContext(), this.options.getHookProperties());
            } catch (Exception e) {
                s_logger.warn("DeploymentHook cancelled operation at postDownload phase");
                throw e;
            }
        }

        if (downloadSuccess && dpFile != null && this.options.isInstall()) {
            s_logger.info("Ready to install");
            this.callback.installDownloadedFile(dpFile, this.options);
        }
    }

    public boolean isAlreadyDownloaded() throws KuraException {
        try {
            File dp = DownloadFileUtilities.getDpDownloadFile(this.options);

            return dp.exists();
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    public boolean deleteDownloadedFile() throws KuraException {
        try {
            return DownloadFileUtilities.deleteDownloadedFile(this.options);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    protected DownloadCountingOutputStream getDownloadInstance(String protocol, DownloadOptions downloadOptions) {
        return DownloadFactory.getDownloadInstance(protocol, downloadOptions);
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    private void incrementalDownloadFromURL(File dpFile, String url, int downloadIndex) throws Exception {
        OutputStream os = null;

        try {
            os = new FileOutputStream(dpFile);
            DownloadOptions downloadOptions = new DownloadOptions();
            downloadOptions.setOut(os);
            downloadOptions.setRequestOptions(this.options);
            downloadOptions.setCallback(this);
            downloadOptions.setSslManagerService(this.sslManagerService);
            downloadOptions.setDownloadURL(url);
            downloadOptions.setAlreadyDownloaded(downloadIndex);

            this.downloadHelper = getDownloadInstance(this.options.getDownloadProtocol(), downloadOptions);
            this.downloadHelper.startWork();
            this.downloadHelper.close();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e1) {
                    s_logger.error("Exception while trying to close stream.", e1);
                }
            }
        }

        if (this.options.getHash() != null) {
            String[] hashAlgorithmValue = this.options.getHash().split(":");

            String hashAlgorithm = null;
            String hashValue = null;
            if (hashAlgorithmValue.length == 2) {
                hashAlgorithm = hashAlgorithmValue[0].trim();
                hashValue = hashAlgorithmValue[1].trim();
            }
            s_logger.info("--> Going to verify hash signature!");
            try {
                // these things should be checked beforehand, so that hash() has a chance to succeed
                if (hashAlgorithm == null || "".equals(hashAlgorithm) || hashValue == null || "".equals(hashValue)) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, null,
                            "Failed to verify checksum with empty algorithm: " + hashAlgorithm);
                }

                String checksum = HashUtil.hash(hashAlgorithm, dpFile);

                if (checksum == null || !checksum.equals(hashValue)) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, null,
                            "Failed to verify checksum with algorithm: " + hashAlgorithm);
                }
            } catch (Exception e) {
                dpFile.delete();
                throw e;
            }
        }
    }

    // Synchronous messages
    public static void downloadInProgressSyncMessage(KuraResponsePayload respPayload,
            DownloadCountingOutputStream downloadHelper, DeploymentPackageDownloadOptions downloadOptions) {
        respPayload.setTimestamp(new Date());
        respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_SIZE, downloadHelper.getTotalBytes().intValue());
        respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_PROGRESS,
                downloadHelper.getDownloadTransferProgressPercentage().intValue());
        respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_STATUS,
                downloadHelper.getDownloadTransferStatus().getStatusString());
        respPayload.addMetric(KuraNotifyPayload.METRIC_JOB_ID, downloadOptions.getJobId());
    }

    public static void downloadAlreadyDoneSyncMessage(KuraResponsePayload respPayload) {
        respPayload.setTimestamp(new Date());
        respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_SIZE, 0);
        respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_PROGRESS, 100);
        respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_STATUS, DownloadStatus.ALREADY_DONE.getStatusString());
    }

    private void alreadyDownloadedAsync() {
        KuraNotifyPayload notify = new KuraNotifyPayload(this.options.getClientId());
        notify.setTimestamp(new Date());
        notify.setTransferSize(0);
        notify.setTransferProgress(100);
        notify.setTransferStatus(DownloadStatus.COMPLETED.getStatusString());
        notify.setJobId(this.options.getJobId());

        this.callback.publishMessage(this.options, notify, RESOURCE_DOWNLOAD);
    }

    private void downloadFailedAsync(int downloadIndex, Exception e) {
        KuraNotifyPayload notify = new KuraNotifyPayload(this.options.getClientId());
        notify.setTimestamp(new Date());
        notify.setTransferSize(0);
        notify.setTransferProgress(0);
        notify.setTransferStatus(DownloadStatus.FAILED.getStatusString());
        notify.setJobId(this.options.getJobId());
        notify.setErrorMessage(e == null ? "Error during download process and verification!" : e.getMessage()); // message
                                                                                                                // to
                                                                                                                // get
                                                                                                                // cause
        notify.setTransferIndex(downloadIndex);

        this.callback.publishMessage(this.options, notify, RESOURCE_DOWNLOAD);
    }

    private File getDpVerifierFile(DeploymentPackageInstallOptions options) {

        String shName = FileUtilities.getFileName(options.getDpName(), options.getDpVersion(), ".sh_verifier.sh");
        String packageFilename = new StringBuilder().append(this.verificationDirectory).append(File.separator)
                .append(shName).toString();

        return new File(packageFilename);
    }
}
