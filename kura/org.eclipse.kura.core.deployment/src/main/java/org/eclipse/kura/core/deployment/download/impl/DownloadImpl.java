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
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2.DOWNLOAD_STATUS;
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
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.ssl.SslManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadImpl implements ProgressListener{
	private static final Logger s_logger = LoggerFactory.getLogger(DownloadImpl.class);
	public static final String RESOURCE_DOWNLOAD = "download";

	private CloudDeploymentHandlerV2 callback;
	private DeploymentPackageDownloadOptions options;
	private DownloadCountingOutputStream downloadHelper;
	private SslManagerService sslManagerService;
	private boolean alreadyDownloadedFlag;
	private String verificationDirectory;

	public DownloadImpl(DeploymentPackageDownloadOptions options, CloudDeploymentHandlerV2 callback){
		this.options = options;
		this.callback = callback;
	}


	// ----------------------------------------------------------------
	//
	// Public methods
	//
	// ----------------------------------------------------------------

	public DownloadCountingOutputStream getDownloadHelper(){
		return downloadHelper;
	}

	public void setSslManager(SslManagerService sslManager){
		this.sslManagerService = sslManager;
	}

	public void setAlreadyDownloadedFlag(boolean alreadyDownloaded){
		this.alreadyDownloadedFlag = alreadyDownloaded;
	}

	public void setVerificationDirectory(String verificationDirectory){
		this.verificationDirectory = verificationDirectory;
	}

	@Override
	public void progressChanged(ProgressEvent progress) {

		s_logger.info("{}% downloaded", progress.getTransferProgress());

		KuraNotifyPayload notify = null;

		notify = new KuraNotifyPayload(progress.getClientId());
		notify.setTimestamp(new Date());
		notify.setTransferSize(progress.getTransferSize());
		notify.setTransferProgress(progress.getTransferProgress());
		notify.setTransferStatus(progress.getTransferStatus());
		notify.setJobId(progress.getJobId());
		if (progress.getExceptionMessage() != null){
			notify.setErrorMessage(progress.getExceptionMessage());
		}

		notify.setTransferIndex(progress.getDownloadIndex());

		callback.publishMessage(options, notify, RESOURCE_DOWNLOAD);
	}

	public void downloadDeploymentPackageInternal() throws KuraException{
		File dpFile = null;
		int downloadIndex = 0;
		boolean downloadSuccess= true;
		try {
			// Download the package to a temporary file.
			// Check for file existence has already been done
			dpFile = DownloadFileUtilities.getDpDownloadFile(options);
			boolean forceDownload = options.isDownloadForced();

			if (!alreadyDownloadedFlag || forceDownload) {
				s_logger.info("To download");
				incrementalDownloadFromURL(dpFile, options.getDeployUri(), downloadIndex);
				downloadIndex++;

				if(options.getVerifierURL() != null){
					File dpVerifier= getDpVerifierFile(options);
					incrementalDownloadFromURL(dpVerifier, options.getVerifierURL(), downloadIndex);
				}
			} else {
				alreadyDownloadedAsync();
			}
		} catch (CancellationException ce) {
			s_logger.error("Download exception", ce);
			downloadSuccess = false;
		} catch (Exception e) {
			s_logger.info("Download exception", e);
			downloadSuccess= false;
			downloadFailedAsync(e, downloadIndex);
		} 


		if (downloadSuccess && dpFile != null && options.isInstall()) {
			s_logger.info("Ready to install");
			callback.installDownloadedFile(dpFile, options);
		}
	}

	public boolean isAlreadyDownloaded() throws KuraException {
		try {
			File dp = DownloadFileUtilities.getDpDownloadFile(options);

			return dp.exists();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	public boolean deleteDownloadedFile() throws KuraException {
		try {
			return DownloadFileUtilities.deleteDownloadedFile(options);
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
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
			DownloadOptions downloadOptions= new DownloadOptions();
			downloadOptions.setOut(os);
			downloadOptions.setRequestOptions(options);
			downloadOptions.setCallback(this);
			downloadOptions.setSslManagerService(sslManagerService);
			downloadOptions.setDownloadURL(url);
			downloadOptions.setAlreadyDownloaded(downloadIndex);
			
			downloadHelper = DownloadFactory.getDownloadInstance(options.getDownloadProtocol(), downloadOptions);
			downloadHelper.startWork();
			downloadHelper.close();
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		if(options.getHash() != null){
			String[] hashAlgorithmValue= options.getHash().split(":");

			String hashAlgorithm= null;
			String hashValue= null;
			if(hashAlgorithmValue.length == 2){
				hashAlgorithm= hashAlgorithmValue[0].trim();
				hashValue= hashAlgorithmValue[1].trim();
			}
			s_logger.info("--> Going to verify hash signature!");
			try{
				String checksum= HashUtil.hash(hashAlgorithm, dpFile);
				if(		   hashAlgorithm == null 
						|| hashAlgorithm.equals("") 
						|| hashValue == null 
						|| hashValue.equals("")
						|| checksum == null
						|| !checksum.equals(hashValue)
						){
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, null, "Failed to verify checksum with algorithm: " + hashAlgorithm);
				}
			}catch(Exception e){
				dpFile.delete();
				throw e;
			}
		}
	}

	//Synchronous messages
	public static void downloadInProgressSyncMessage(KuraResponsePayload respPayload, DownloadCountingOutputStream downloadHelper, DeploymentPackageDownloadOptions m_downloadOptions) {
		respPayload.setTimestamp(new Date());
		respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_SIZE, downloadHelper.getTotalBytes().intValue());
		respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_PROGRESS, downloadHelper.getDownloadTransferProgressPercentage().intValue());
		respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_STATUS, downloadHelper.getDownloadTransferStatus().getStatusString());
		respPayload.addMetric(KuraNotifyPayload.METRIC_JOB_ID, m_downloadOptions.getJobId());
	}

	public static void downloadAlreadyDoneSyncMessage(KuraResponsePayload respPayload) {
		respPayload.setTimestamp(new Date());
		respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_SIZE, 0);
		respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_PROGRESS, 100);
		respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_STATUS, DOWNLOAD_STATUS.ALREADY_DONE);
		//respPayload.addMetric(METRIC_JOB_ID, m_options.getJobId());
	}

	private void alreadyDownloadedAsync(){
		KuraNotifyPayload notify = null;

		notify = new KuraNotifyPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setTransferSize(0);
		notify.setTransferProgress(100);
		notify.setTransferStatus(DOWNLOAD_STATUS.COMPLETED.getStatusString());
		notify.setJobId(options.getJobId());


		callback.publishMessage(options, notify, RESOURCE_DOWNLOAD);
	}

	private void downloadFailedAsync(Exception e, int downloadIndex) {
		KuraNotifyPayload notify = null;

		notify = new KuraNotifyPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setTransferSize(0);
		notify.setTransferProgress(100);
		notify.setTransferStatus(DOWNLOAD_STATUS.FAILED.getStatusString());
		notify.setJobId(options.getJobId());
		notify.setErrorMessage("Error during download process and verification!"); //message to get cause
		notify.setTransferIndex(downloadIndex);

		callback.publishMessage(options, notify, RESOURCE_DOWNLOAD);
	}

	private File getDpVerifierFile(DeploymentPackageInstallOptions options) throws IOException {
		String packageFilename = null;

		String shName= FileUtilities.getFileName(options.getDpName(), options.getDpVersion(), "_verifier.sh");
		packageFilename = new StringBuilder().append(verificationDirectory)
				.append(File.separator)
				.append(shName)
				.toString();

		File dpFile = new File(packageFilename);
		return dpFile;
	}
}
