package org.eclipse.kura.core.deployment.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.DeploymentPackageOptions;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2.INSTALL_STATUS;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstallImpl {
	private static final Logger s_logger = LoggerFactory.getLogger(InstallImpl.class);
	private final static int   PROGRESS_COMPLETE = 100;

	public static final String RESOURCE_INSTALL = "install";

	public static final String PERSISTANCE_SUFFIX = "_persistance";
	public static final String PERSISTANCE_FOLDER_NAME= "persistance";
	public static final String PERSISTANCE_VERIFICATION_FOLDER_NAME= "verification";
	public static final String PERSISTANCE_FILE_NAME = "persistance.file.name";

	private DeploymentPackageInstallOptions options;
	private CloudDeploymentHandlerV2 callback;
	private DeploymentAdmin deploymentAdmin;
	private Properties deployedPackages;
	private Properties m_installPersistance;
	private String dpaConfPath;
	private String m_installVerifDir;
	private String m_installPersistanceDir;
	private String packagesPath;

	public InstallImpl(CloudDeploymentHandlerV2 callback, String kuraDataDir){
		this.callback = callback;

		deployedPackages = new Properties();
		m_installPersistanceDir= kuraDataDir + File.separator + PERSISTANCE_FOLDER_NAME;
		File installPersistanceDir = new File(m_installPersistanceDir);
		if (!installPersistanceDir.exists()) {
			installPersistanceDir.mkdir();
		}

		m_installVerifDir= m_installPersistanceDir + File.separator + PERSISTANCE_VERIFICATION_FOLDER_NAME;
		File installVerificationDir = new File(m_installVerifDir);
		if (!installVerificationDir.exists()) {
			installVerificationDir.mkdir();
		}
	}

	public Properties getDeployedPackages(){
		return deployedPackages;
	}

	public void setOptions(DeploymentPackageInstallOptions options){
		this.options = options;
	}

	public void setPackagesPath(String packagesPath){
		this.packagesPath= packagesPath;
	}

	public void setDeploymentAdmin(DeploymentAdmin deploymentAdmin){
		this.deploymentAdmin= deploymentAdmin;
	}

	public void setDeployedPackages(Properties deployedPackages){
		this.deployedPackages = deployedPackages;
	}

	public void setDpaConfPath(String dpaConfPath){
		this.dpaConfPath= dpaConfPath;
	}

	public void installDp(DeploymentPackageInstallOptions options, File dpFile) throws KuraException{
		SafeProcess proc = null;
		try {
			installDeploymentPackageInternal(dpFile);
			installCompleteAsync(options, dpFile.getName());
			s_logger.info("Install completed!");

			if(options.isReboot()){
				Thread.sleep(options.getRebootDelay());
				proc = ProcessUtil.exec("reboot");
			}
		} catch (Exception e) {
			s_logger.info("Install failed!");
			installFailedAsync(options, dpFile.getName(), e);
		} finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}
	}

	public void installSh(DeploymentPackageOptions options, File shFile) throws KuraException{	

		updateInstallPersistance(shFile.getName(), options);

		//Esecuzione script
		SafeProcess proc = null;
		try {
			proc = ProcessUtil.exec("chmod 700 " + shFile.getCanonicalPath());
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
		} finally {
			if (proc != null) {
				ProcessUtil.destroy(proc);
			}
		}

		SafeProcess proc2 = null;
		try {
			proc2 = ProcessUtil.exec(shFile.getCanonicalPath());
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
		} finally {
			if (proc2 != null) {
				ProcessUtil.destroy(proc2);
			}
		}
	}

	public void installInProgressSyncMessage(KuraResponsePayload respPayload) {
		respPayload.setTimestamp(new Date());
		respPayload.addMetric(KuraInstallPayload.METRIC_INSTALL_STATUS, INSTALL_STATUS.IN_PROGRESS);
		respPayload.addMetric(KuraInstallPayload.METRIC_DP_NAME, options.getDpName());
		respPayload.addMetric(KuraInstallPayload.METRIC_DP_VERSION, options.getDpVersion());
	}

	public void installIdleSyncMessage(KuraResponsePayload respPayload) {
		respPayload.setTimestamp(new Date());
		respPayload.addMetric(KuraInstallPayload.METRIC_INSTALL_STATUS, INSTALL_STATUS.IDLE);
	}

	public void installCompleteAsync(DeploymentPackageOptions options, String dpName) throws KuraException{
		KuraInstallPayload notify = null;

		notify = new KuraInstallPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setInstallStatus(INSTALL_STATUS.COMPLETED.getStatusString());
		notify.setJobId(options.getJobId());
		notify.setDpName(dpName); //Probably split dpName and dpVersion?
		notify.setInstallProgress(PROGRESS_COMPLETE);

		callback.publishMessage(options, notify, RESOURCE_INSTALL);
	}

	public void installFailedAsync(DeploymentPackageInstallOptions options, String dpName, Exception e) throws KuraException{
		KuraInstallPayload notify = null;

		notify = new KuraInstallPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setInstallStatus(INSTALL_STATUS.FAILED.getStatusString());
		notify.setJobId(options.getJobId());
		notify.setDpName(dpName); //Probably split dpName and dpVersion?
		notify.setInstallProgress(0);
		if (e != null){
			notify.setErrorMessage(e.getMessage());
		}

		callback.publishMessage(options, notify, RESOURCE_INSTALL);
	}

	public void sendInstallConfirmations(){
		s_logger.info("Ready to send Confirmations");
		File verificationDir= new File(m_installVerifDir);
		if (verificationDir.listFiles() != null) {
			for (File fileEntry : verificationDir.listFiles()) {
				if (fileEntry.isFile() && fileEntry.getName().endsWith(".sh")) {
					SafeProcess proc = null;
					try {
						proc = ProcessUtil.exec("chmod 700 " + fileEntry.getCanonicalPath());
					} catch (IOException e) {

					} finally {
						if (proc != null) {
							ProcessUtil.destroy(proc);
						}
					}

					SafeProcess proc2 = null;
					try {
						proc2 = ProcessUtil.exec(fileEntry.getCanonicalPath());
						int exitValue = proc2.exitValue();
						if(exitValue == 0){
							sendSysUpdateSuccess();
						} else {
							sendSysUpdateFailure();
						}
					} catch (Exception e) {

					} finally {
						fileEntry.delete();
						if (proc2 != null) {
							ProcessUtil.destroy(proc2);
						}
					}

				}
			}
		}
	}

	private DeploymentPackage installDeploymentPackageInternal(File fileReference) 
			throws DeploymentException, IOException {

		InputStream dpInputStream = null;
		DeploymentPackage dp = null;
		File dpPersistentFile = null;
		File downloadedFile = fileReference;

		try {
			String dpBasename = fileReference.getName();
			String dpPersistentFilePath = packagesPath + File.separator + dpBasename;
			dpPersistentFile = new File(dpPersistentFilePath);
			//downloadedFile = getDpDownloadFile(options);


			dpInputStream = new FileInputStream(downloadedFile);
			dp = deploymentAdmin.installDeploymentPackage(dpInputStream);

			// Now we need to copy the deployment package file to the Kura
			// packages directory unless it's already there.

			if (!downloadedFile.getCanonicalPath().equals(dpPersistentFile.getCanonicalPath())) {
				s_logger.debug("dpFile.getCanonicalPath(): " + downloadedFile.getCanonicalPath());
				s_logger.debug("dpPersistentFile.getCanonicalPath(): " + dpPersistentFile.getCanonicalPath());
				FileUtils.copyFile(downloadedFile, dpPersistentFile);
				addPackageToConfFile(dp.getName(), "file:" + dpPersistentFilePath);
			}
		} catch (FileNotFoundException ex) {

		} catch (IOException ex){

		} finally{
			if (dpInputStream != null) {
				try {
					dpInputStream.close();
				} catch (IOException e) {
					s_logger.warn("Cannot close input stream", e);
				}
			}
			// The file from which we have installed the deployment package will be deleted
			// unless it's a persistent deployment package file.
			if (downloadedFile != null && !downloadedFile.getCanonicalPath().equals(dpPersistentFile.getCanonicalPath())) {
				downloadedFile.delete();
			}			
		}

		return dp;
	}

	private void updateInstallPersistance(String fileName, DeploymentPackageOptions options){
		m_installPersistance = new Properties();
		m_installPersistance.setProperty(DeploymentPackageOptions.METRIC_DP_CLIENT_ID, options.getClientId());
		m_installPersistance.setProperty(DeploymentPackageOptions.METRIC_JOB_ID, Long.toString(options.getJobId()));
		m_installPersistance.setProperty(DeploymentPackageOptions.METRIC_DP_NAME, fileName);
		m_installPersistance.setProperty(DeploymentPackageOptions.METRIC_DP_VERSION, options.getDpVersion());
		m_installPersistance.setProperty(CloudDeploymentHandlerV2.METRIC_REQUESTER_CLIENT_ID, options.getRequestClientId());
		m_installPersistance.setProperty(PERSISTANCE_FILE_NAME, fileName);

		if (m_installPersistanceDir == null) {
			s_logger.warn("Configuration file not specified");
			return;
		}

		FileOutputStream fos= null;
		try {
			String persistanceFile= m_installPersistanceDir + File.separator + fileName + PERSISTANCE_SUFFIX;
			fos = new FileOutputStream(persistanceFile);
			m_installPersistance.store(fos, null);
			fos.flush();
			fos.getFD().sync();
		} catch (IOException e) {
			s_logger.error("Error writing remote install configuration file", e);
		} finally {
			if(fos != null){
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void addPackageToConfFile(String packageName, String packageUrl) {
		deployedPackages.setProperty(packageName, packageUrl);

		if (dpaConfPath == null) {
			s_logger.warn("Configuration file not specified");
			return;
		}

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(dpaConfPath);
			deployedPackages.store(fos, null);
			fos.flush();
			fos.getFD().sync();
		} catch (IOException e) {
			s_logger.error("Error writing package configuration file", e);
		} finally {
			try{
				if (fos != null){
					fos.close();
				}
			} catch (IOException e){
				e.printStackTrace();
			}
		}
	}

	public void removePackageFromConfFile(String packageName) {
		deployedPackages.remove(packageName);

		if (dpaConfPath == null) {
			s_logger.warn("Configuration file not specified");
			return;
		}

		FileOutputStream fos= null;
		try {
			fos = new FileOutputStream(dpaConfPath);
			deployedPackages.store(fos, null);
			fos.flush();
			fos.getFD().sync();
			fos.close();
		} catch (IOException e) {
			s_logger.error("Error writing package configuration file", e);
		} finally {
			try{
				if (fos != null){
					fos.close();
				}
			} catch (IOException e){
				e.printStackTrace();
			}
		}
	}

	private void sendSysUpdateSuccess() throws KuraException {
		s_logger.info("Ready to send success after install");
		File installDir= new File(m_installPersistanceDir);
		if(installDir != null){
			for (File fileEntry : installDir.listFiles()) {

				if (fileEntry.isFile() && fileEntry.getName().endsWith(PERSISTANCE_SUFFIX)) { //&& fileEntry.getName().contains(verificationFile.getName()
					Properties downloadProperties= loadInstallPersistance(fileEntry);
					String deployUrl= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI);
					String dpName= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_NAME);
					String dpVersion= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_VERSION);
					String clientId= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_CLIENT_ID);
					Long jobId= Long.valueOf(downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_JOB_ID));
					String fileSystemFileName= downloadProperties.getProperty(PERSISTANCE_FILE_NAME);
					String requestClientId = downloadProperties.getProperty(CloudDeploymentHandlerV2.METRIC_REQUESTER_CLIENT_ID);

					DeploymentPackageDownloadOptions deployOptions = new DeploymentPackageDownloadOptions(deployUrl, dpName, dpVersion);
					deployOptions.setClientId(clientId);
					deployOptions.setJobId(jobId);
					deployOptions.setRequestClientId(requestClientId);

					try {
						installCompleteAsync(deployOptions, fileSystemFileName);
						s_logger.info("Sent install complete");
						fileEntry.delete();
						break;
					} catch (KuraException e) {
						throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
					}
				}
			}
		}
	}

	private void sendSysUpdateFailure() throws KuraException {
		File installDir= new File(m_installPersistanceDir);
		if(installDir != null){
			for (final File fileEntry : installDir.listFiles()) {
				if (fileEntry.isFile() && fileEntry.getName().endsWith(PERSISTANCE_SUFFIX)) { //&& fileEntry.getName().contains(verificationFile.getName())
					Properties downloadProperties= loadInstallPersistance(fileEntry);
					String deployUrl= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI);
					String dpName= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_NAME);
					String dpVersion= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_VERSION);
					String clientId= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_CLIENT_ID);
					Long jobId= Long.valueOf(downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_JOB_ID));
					String fileSystemFileName= downloadProperties.getProperty(PERSISTANCE_FILE_NAME);
					String requestClientId = downloadProperties.getProperty(CloudDeploymentHandlerV2.METRIC_REQUESTER_CLIENT_ID);

					DeploymentPackageDownloadOptions deployOptions = new DeploymentPackageDownloadOptions(deployUrl, dpName, dpVersion);
					deployOptions.setClientId(clientId);
					deployOptions.setJobId(jobId);
					deployOptions.setRequestClientId(requestClientId);

					try {
						installFailedAsync(deployOptions, fileSystemFileName, new KuraException(KuraErrorCode.INTERNAL_ERROR));
						s_logger.info("Sent install failed");
						fileEntry.delete();
						break;
					} catch (KuraException e) {
						throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
					}
				}
			}
		}
	}

	private Properties loadInstallPersistance(File installedDpPersistance){
		Properties downloadProperies= new Properties();
		FileReader fr = null;
		try {
			fr= new FileReader(installedDpPersistance);
			downloadProperies.load(fr);
		} catch (IOException e) {
			s_logger.error("Exception loading install configuration file", e);
		} finally {
			try{
				if (fr != null) {
					fr.close();
				}
			} catch (IOException e){
				e.printStackTrace();
			}
		}
		return downloadProperies;
	}
}
