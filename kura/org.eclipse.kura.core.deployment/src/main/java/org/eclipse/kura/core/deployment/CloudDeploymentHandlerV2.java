/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */

package org.eclipse.kura.core.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadCountingOutputStream;
import org.eclipse.kura.core.deployment.download.HttpDownloadCountingOutputStream;
import org.eclipse.kura.core.deployment.download.KuraNotifyPayload;
import org.eclipse.kura.core.deployment.install.DeploymentPackageInstallOptions;
import org.eclipse.kura.core.deployment.install.KuraInstallPayload;
import org.eclipse.kura.core.deployment.progress.ProgressEvent;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.core.deployment.uninstall.DeploymentPackageUninstallOptions;
import org.eclipse.kura.core.deployment.uninstall.KuraUninstallPayload;
import org.eclipse.kura.core.deployment.util.HashUtil;
import org.eclipse.kura.core.deployment.xml.XmlBundle;
import org.eclipse.kura.core.deployment.xml.XmlBundleInfo;
import org.eclipse.kura.core.deployment.xml.XmlBundles;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackage;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackages;
import org.eclipse.kura.core.deployment.xml.XmlUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.core.util.ThrowableUtil;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.ssl.SslManagerService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudDeploymentHandlerV2 extends Cloudlet implements ProgressListener {

	private static final Logger s_logger = LoggerFactory.getLogger(CloudDeploymentHandlerV2.class);
	public static final String APP_ID = "DEPLOY-V2";

	private static final String DPA_CONF_PATH_PROPNAME = "dpa.configuration";
	private static final String KURA_CONF_URL_PROPNAME = "kura.configuration";
	private static final String PACKAGES_PATH_PROPNAME = "kura.packages";
	private static final String KURA_DATA_DIR = "kura.data";
	private static final String PERSISTANCE_FILE_NAME = "persistance.file.name";


	public static final String  RESOURCE_PACKAGES = "packages";
	public static final String  RESOURCE_BUNDLES  = "bundles";


	/* EXEC */
	public static final String RESOURCE_DOWNLOAD = "download";
	public static final String RESOURCE_INSTALL = "install";
	public static final String RESOURCE_UNINSTALL = "uninstall";
	public static final String RESOURCE_CANCEL = "cancel";
	public static final String RESOURCE_START     = "start";
	public static final String RESOURCE_STOP      = "stop";

	/* Metrics in the REPLY to RESOURCE_DOWNLOAD */
	public static final String METRIC_DOWNLOAD_STATUS = "download.status";
	public static final String METRIC_REQUESTER_CLIENT_ID = "requester.client.id";

	private static final String PERSISTANCE_SUFFIX = "_persistance";
	private static final String PERSISTANCE_FOLDER_NAME= "persistance";
	private static final String PERSISTANCE_VERIFICATION_FOLDER_NAME= "verification";


	/**
	 * Enum representing the different status of the download process
	 * 
	 * {@link DeploymentAgentService.DOWNLOAD_STATUS.PROGRESS} Download in
	 * progress {@link DeploymentAgentService.DOWNLOAD_STATUS.COMPLETE} Download
	 * completed {@link DeploymentAgentService.DOWNLOAD_STATUS.FAILED} Download
	 * failed
	 */
	public static enum DOWNLOAD_STATUS {
		IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED"), FAILED("FAILED"), ALREADY_DONE("ALREADY DONE");

		private final String status;

		DOWNLOAD_STATUS(String status) {
			this.status = status;
		}

		public String getStatusString() {
			return status;
		}
	}

	public static enum INSTALL_STATUS {
		IDLE("IDLE"), IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED"), FAILED("FAILED"), ALREADY_DONE("ALREADY DONE");

		private final String status;

		INSTALL_STATUS(String status) {
			this.status = status;
		}

		public String getStatusString() {
			return status;
		}
	}

	public static enum UNINSTALL_STATUS {
		IDLE("IDLE"), IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED"), FAILED("FAILED"), ALREADY_DONE("ALREADY DONE");

		private final String status;

		UNINSTALL_STATUS(String status) {
			this.status = status;
		}

		public String getStatusString() {
			return status;
		}
	}

	private static String s_pendingPackageUrl = null;

	private SslManagerService m_sslManagerService;
	private DeploymentAdmin   m_deploymentAdmin;

	private DownloadCountingOutputStream downloadHelper;

	private static ExecutorService executor = Executors.newSingleThreadExecutor();


	private Future<?> downloaderFuture;
	private Future<?> installerFuture;

	private BundleContext m_bundleContext;

	private DataTransportService m_dataTransportService;

	private Properties m_deployedPackages;
	private Properties m_installPersistance;
	private String m_dpaConfPath;
	private String m_packagesPath;

	private String m_installPersistanceDir;
	private DeploymentPackageDownloadOptions m_downloadOptions;

	private boolean m_isInstalling = false;
	private DeploymentPackageInstallOptions m_installOptions;

	private String m_pendingUninstPackageName;
	private String m_installVerificationDir;
	private String m_clientId;



	// ----------------------------------------------------------------
	//
	// Dependencies
	//
	// ----------------------------------------------------------------

	public void setSslManagerService(SslManagerService sslManagerService) {
		this.m_sslManagerService = sslManagerService;
	}

	public void unsetSslManagerService(SslManagerService sslManagerService) {
		this.m_sslManagerService = null;
	}

	protected void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
		m_deploymentAdmin = deploymentAdmin; 
	}

	protected void unsetDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
		m_deploymentAdmin = null;
	}

	public void setDataTransportService(DataTransportService dataTransportService) {
		m_dataTransportService = dataTransportService;
	}

	public void unsetDataTransportService(DataTransportService dataTransportService) {
		m_dataTransportService = null;
	}

	public CloudDeploymentHandlerV2() {
		super(APP_ID);
	}


	// ----------------------------------------------------------------
	//
	// Activation APIs
	//
	// ----------------------------------------------------------------

	@Override
	protected void activate(ComponentContext componentContext) {
		s_logger.info("Cloud Deployment v2 is starting");
		super.activate(componentContext);

		m_bundleContext = componentContext.getBundleContext();
		m_clientId= m_dataTransportService.getClientId();

		m_deployedPackages = new Properties();

		m_dpaConfPath = System.getProperty(DPA_CONF_PATH_PROPNAME);
		if (m_dpaConfPath == null || m_dpaConfPath.isEmpty()) {
			throw new ComponentException("The value of '" + DPA_CONF_PATH_PROPNAME + "' is not defined");
		}

		String sKuraConfUrl = System.getProperty(KURA_CONF_URL_PROPNAME);
		if (sKuraConfUrl == null || sKuraConfUrl.isEmpty()) {
			throw new ComponentException("The value of '" + KURA_CONF_URL_PROPNAME + "' is not defined");
		}

		URL kuraUrl = null;
		try {
			kuraUrl = new URL(sKuraConfUrl);
		} catch (MalformedURLException e) {
			throw new ComponentException("Invalid Kura configuration URL");
		}

		Properties kuraProperties = new Properties();
		try {
			kuraProperties.load(kuraUrl.openStream());
		} catch (FileNotFoundException e) {
			throw new ComponentException("Kura configuration file not found", e);
		} catch (IOException e) {
			throw new ComponentException("Exception loading Kura configuration file", e);
		}

		m_packagesPath = kuraProperties.getProperty(PACKAGES_PATH_PROPNAME);
		if (m_packagesPath == null || m_packagesPath.isEmpty()) {
			throw new ComponentException("The value of '" + PACKAGES_PATH_PROPNAME + "' is not defined");
		}
		if (kuraProperties.getProperty(PACKAGES_PATH_PROPNAME) != null && kuraProperties.getProperty(PACKAGES_PATH_PROPNAME).trim().equals("kura/packages")) {
			kuraProperties.setProperty(PACKAGES_PATH_PROPNAME, "/opt/eclipse/kura/kura/packages");
			m_packagesPath = kuraProperties.getProperty(PACKAGES_PATH_PROPNAME);
			s_logger.warn("Overridding invalid kura.packages location");
		}

		String kuraDataDir= kuraProperties.getProperty(KURA_DATA_DIR);
		m_installPersistanceDir= kuraDataDir + File.separator + PERSISTANCE_FOLDER_NAME;
		File installPersistanceDir = new File(m_installPersistanceDir);
		if (!installPersistanceDir.exists()) {
			installPersistanceDir.mkdir();
		}

		m_installVerificationDir= m_installPersistanceDir + File.separator + PERSISTANCE_VERIFICATION_FOLDER_NAME;
		File installVerificationDir = new File(m_installVerificationDir);
		if (!installVerificationDir.exists()) {
			installVerificationDir.mkdir();
		}


		sendInstallConfirmations();
	}

	@Override
	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("Bundle " + APP_ID + " is deactivating!");
		if(downloaderFuture != null){
			downloaderFuture.cancel(true);
		}

		if(installerFuture != null){
			installerFuture.cancel(true);
		}

		m_bundleContext = null;
	}



	// ----------------------------------------------------------------
	//
	// Public methods
	//
	// ----------------------------------------------------------------

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

		try {
			getCloudApplicationClient().controlPublish(progress.getRequesterClientId(), "NOTIFY/"+progress.getClientId()+"/progress", notify, 2, DFLT_RETAIN, DFLT_PRIORITY);
		} catch (KuraException e) {
			s_logger.error("Error publishing response for command {} {}", RESOURCE_DOWNLOAD, e);
		}
	}



	// ----------------------------------------------------------------
	//
	// Protected methods
	//
	// ----------------------------------------------------------------

	@Override
	protected void doGet(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {

		//doGetResource(reqTopic, reqPayload);

		String[] resources = reqTopic.getResources();

		if (resources == null || resources.length == 0) {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Expected one resource but found {}", resources != null ? resources.length : "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}

		if (resources[0].equals(RESOURCE_DOWNLOAD)) {
			doGetDownload(reqPayload, respPayload);
		} else if (resources[0].equals(RESOURCE_INSTALL)) {
			doGetInstall(reqPayload, respPayload);
		} else if (resources[0].equals(RESOURCE_PACKAGES)) {
			doGetPackages(reqPayload, respPayload);
		} else if (resources[0].equals(RESOURCE_BUNDLES)) {
			doGetBundles(reqPayload, respPayload);
		} else {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Cannot find resource with name: {}", resources[0]);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			return;
		}
	}

	@Override
	protected void doExec(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {

		String[] resources = reqTopic.getResources();

		if (resources == null || resources.length == 0) {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Expected one resource but found {}", resources != null ? resources.length : "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}


		if (resources[0].equals(RESOURCE_DOWNLOAD)) {

			doExecDownload(reqPayload, respPayload);

		} else if (resources[0].equals(RESOURCE_CANCEL)) {

			doExecCancel(reqPayload, respPayload);

		} else if (resources[0].equals(RESOURCE_INSTALL)) {

			doExecInstall(reqPayload, respPayload);

		} else if (resources[0].equals(RESOURCE_UNINSTALL)) {

			doExecUninstall(reqPayload, respPayload);

		} else if (resources[0].equals(RESOURCE_START)) {
			String bundleId = resources[1];
			doExecStartStopBundle(reqPayload, respPayload, true, bundleId);
		} else if (resources[0].equals(RESOURCE_STOP)) {
			String bundleId = resources[1];
			doExecStartStopBundle(reqPayload, respPayload, false, bundleId);
		}else {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Cannot find resource with name: {}", resources[0]);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			return;
		}
	}



	// ----------------------------------------------------------------
	//
	// Private methods
	//
	// ----------------------------------------------------------------

	private void doExecCancel(KuraRequestPayload request, KuraResponsePayload response) {

		String dpName = (String) request.getMetric(DeploymentPackageDownloadOptions.METRIC_DP_NAME);
		String dpVersion = (String) request.getMetric(DeploymentPackageDownloadOptions.METRIC_DP_VERSION);

		try{
			downloadHelper.cancelDownload();
		}catch(Exception ex){
			s_logger.info("Error cancelling download!", ex);
		}

		if ((dpName != null) && (dpVersion != null)) {
			try {
				String packageFilename = new StringBuilder().append(File.separator)
						.append("tmp").append(File.separator)
						.append(dpName).append("-").append(dpVersion).append(".dp").toString();

				File dpFile = new File(packageFilename);
				if (dpFile.exists()) {
					if (!dpFile.delete()) {
						throw new Exception("Could not delete file dp!");
					}
				}

			} catch (Exception e) {
				s_logger.info("Exception while deleting dp file!");
				response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
				response.setTimestamp(new Date());
			}
		}
	}

	private void doExecDownload(KuraRequestPayload request, KuraResponsePayload response) {

		final DeploymentPackageDownloadOptions options;
		try {
			options = new DeploymentPackageDownloadOptions(request);
			options.setClientId(m_clientId);
		} catch (Exception ex) {
			s_logger.info("Malformed download request!");
			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
			try {
				response.setBody("Malformed donwload request".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
			response.setException(ex);

			return;
		}
		m_downloadOptions = options;

		if (s_pendingPackageUrl != null && s_pendingPackageUrl.equals(options.getDeployUrl())) {
			s_logger.info("Another request seems for the same URL is pending: {}.", s_pendingPackageUrl);

			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
			response.setTimestamp(new Date());
			response.addMetric(METRIC_DOWNLOAD_STATUS, DOWNLOAD_STATUS.IN_PROGRESS);
			try {
				response.setBody("The requested resource is already in download".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
			return;
		} else if (s_pendingPackageUrl != null && !s_pendingPackageUrl.equals(options.getDeployUrl())) {
			s_logger.info("Another request is pending for a different URL: {}.", s_pendingPackageUrl);

			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
			response.addMetric(METRIC_DOWNLOAD_STATUS, DOWNLOAD_STATUS.IN_PROGRESS);
			try {
				response.setBody("Only one request at a time is allowed".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
			return;
		}

		boolean alreadyDownloaded = false;

		try {
			alreadyDownloaded = deploymentPackageAlreadyDownloaded(options);
		} catch (KuraException ex) {
			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setException(ex);
			response.setTimestamp(new Date());
			try {
				response.setBody("Error checking download status".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
			return;
		}

		final boolean alreadyDownloadedFinal = alreadyDownloaded;

		s_logger.info("About to download and install package at URL {}", options.getDeployUrl());

		try {
			s_pendingPackageUrl = options.getDeployUrl();
			// m_pendingInstRequestId = request.getRequestId();
			// m_pendingInstRequesterClientId = request.getRequesterClientId();

			s_logger.info("Downloading package from URL: " + options.getDeployUrl());


			downloaderFuture = executor.submit(new Runnable(){

				@Override
				public void run() {
					try {
						downloadDeploymentPackageInternal(options, alreadyDownloadedFinal);
					} catch (KuraException e) {

					} finally{
						s_pendingPackageUrl = null;
					}
				}
			});

		} catch (Exception e) {
			s_logger.error("Failed to download and install package at URL {}: {}", options.getDeployUrl(), e);

			s_pendingPackageUrl = null;

			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
			try {
				response.setBody(e.getMessage().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException uee) {
			}
		}

		return;
	}

	private void doExecInstall(KuraRequestPayload request, KuraResponsePayload response){
		final DeploymentPackageInstallOptions options;
		try {
			options = new DeploymentPackageInstallOptions(request);
			options.setClientId(m_clientId);
		} catch (Exception ex) {
			s_logger.info("Malformed install request!");
			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
			try {
				response.setBody("Malformed install request".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
			response.setException(ex);

			return;
		}

		m_installOptions = options;
		boolean alreadyDownloaded = false;

		try {
			alreadyDownloaded = deploymentPackageAlreadyDownloaded(options);
		} catch (KuraException ex) {
			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setException(ex);
			response.setTimestamp(new Date());
			try {
				response.setBody("Error checking download status".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
			return;
		}

		if(alreadyDownloaded && !m_isInstalling){
			//Check if file exists

			try {
				m_isInstalling = true;
				final File dpFile = getDpDownloadFile(options);
				//if yes, install

				installerFuture = executor.submit(new Runnable(){

					@Override
					public void run() {
						try {
							installDownloadedFile(dpFile, options);
						} catch (Exception e) {
							try {
								installFailedAsync(options, dpFile.getName(), e);
							} catch (KuraException e1) {

							}
						} finally {
							m_installOptions = null;
							m_isInstalling = false;
						}
					}
				});
			} catch (IOException e) {
				response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
				response.setException(e);
				response.setTimestamp(new Date());
				try {
					response.setBody("Exception during install".getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e1) {
				}
			} 
		} else {
			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setException(new KuraException(KuraErrorCode.INTERNAL_ERROR));
			response.setTimestamp(new Date());
			try {
				response.setBody("Already installing/uninstalling".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
			return;
		}
	}

	private void doExecUninstall(KuraRequestPayload request, KuraResponsePayload response) {
		final DeploymentPackageUninstallOptions options;
		try {
			options = new DeploymentPackageUninstallOptions(request);
			options.setClientId(m_clientId);
		} catch (Exception ex) {
			s_logger.info("Malformed uninstall request!");
			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
			try {
				response.setBody("Malformed uninstall request".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
			response.setException(ex);

			return;
		}



		final String packageName = options.getDpName();

		//
		// We only allow one request at a time
		if (!m_isInstalling && m_pendingUninstPackageName != null) {
			s_logger.info("Antother request seems still pending: {}. Checking if stale...", m_pendingUninstPackageName);

			response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
			try {
				response.setBody("Only one request at a time is allowed".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
		} else {

			s_logger.info("About to uninstall package {}", packageName);

			try {
				m_isInstalling = true;
				m_pendingUninstPackageName = packageName;

				s_logger.info("Uninstalling package...");
				installerFuture = executor.submit(new Runnable(){

					@Override
					public void run() {
						try {
							uninstaller(options, packageName);
						} catch (Exception e) {
							try {
								uninstallFailedAsync(options, packageName, e);
							} catch (KuraException e1) {

							}
						} finally {
							m_installOptions = null;
							m_isInstalling = false;
						}
					}
				});
			} catch (Exception e) {
				s_logger.error("Failed to uninstall package {}: {}", packageName, e);

				response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);
				response.setTimestamp(new Date());
				try {
					response.setBody(e.getMessage().getBytes("UTF-8"));
				} catch (UnsupportedEncodingException uee) {
					// Ignore
				}
			} finally {
				m_isInstalling = false;
				m_pendingUninstPackageName = null;
			}
		}
	}

	private void doExecStartStopBundle(KuraRequestPayload request, KuraResponsePayload response, boolean start, String bundleId) {
		if (bundleId == null) {
			s_logger.info("EXEC start/stop bundle: null bundle ID");

			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);

			response.setTimestamp(new Date());
		} else {
			Long id = null;
			try {
				id = Long.valueOf(bundleId);
			} catch (NumberFormatException e){

				s_logger.error("EXEC start/stop bundle: bad bundle ID format: {}", e);
				response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
				response.setTimestamp(new Date());
				response.setExceptionMessage(e.getMessage());
				response.setExceptionStack(ThrowableUtil.stackTraceAsString(e));
			}

			if (id != null) {

				s_logger.info("Executing command {}", start ? RESOURCE_START : RESOURCE_STOP);

				Bundle bundle = m_bundleContext.getBundle(id);
				if (bundle == null) {
					s_logger.error("Bundle ID {} not found", id);
					response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
					response.setTimestamp(new Date());
				} else {
					try {
						if (start) {
							bundle.start();
						} else {
							bundle.stop();
						}
						s_logger.info("{} bundle ID {} ({})", new Object[] {start ? "Started" : "Stopped", id, bundle.getSymbolicName()});
						response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
						response.setTimestamp(new Date());
					} catch (BundleException e) {
						s_logger.error("Failed to {} bundle {}: {}", new Object[] {start ? "start" : "stop", id, e});
						response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
						response.setTimestamp(new Date());
					}
				}				
			}
		}
	}

	private void uninstaller(DeploymentPackageUninstallOptions options, String packageName) throws KuraException {
		try{
			String name = packageName;
			if (name != null) {
				s_logger.info("About to uninstall package ", name);
				DeploymentPackage dp = null;

				dp = m_deploymentAdmin.getDeploymentPackage(name);
				if (dp != null) {
					dp.uninstall();

					String sUrl = m_deployedPackages.getProperty(name);
					File dpFile = new File(new URL(sUrl).getPath());
					if (!dpFile.delete()) {
						s_logger.warn("Cannot delete file at URL: {}", sUrl);
					}
					removePackageFromConfFile(name);
					uninstallCompleteAsync(options, name);
				}
			}
		} catch (Exception e) {
			throw KuraException.internalError(e.getMessage());
		}
	}

	private void removePackageFromConfFile(String packageName) {
		m_deployedPackages.remove(packageName);

		if (m_dpaConfPath == null) {
			s_logger.warn("Configuration file not specified");
			return;
		}

		try {
			FileOutputStream fos = new FileOutputStream(m_dpaConfPath);
			m_deployedPackages.store(fos, null);
			fos.flush();
			fos.getFD().sync();
			fos.close();
		} catch (IOException e) {
			s_logger.error("Error writing package configuration file", e);
		}
	}

	private void doGetInstall(KuraRequestPayload reqPayload, KuraResponsePayload respPayload) {
		if(m_isInstalling){
			installInProgressSyncMessage(respPayload);
		} else {
			installIdleSyncMessage(respPayload);
		}
	}

	private void doGetDownload(KuraRequestPayload reqPayload, KuraResponsePayload respPayload) {
		if (s_pendingPackageUrl != null){ //A download is pending
			downloadInProgressSyncMessage(respPayload);
		} else { //No pending downloads
			downloadAlreadyDoneSyncMessage(respPayload); //is it right? Do we remove the last object
		}
	}

	private void doGetPackages(KuraRequestPayload request, KuraResponsePayload response) {

		DeploymentPackage[] dps = m_deploymentAdmin.listDeploymentPackages();
		XmlDeploymentPackages xdps = new XmlDeploymentPackages();
		XmlDeploymentPackage[] axdp = new XmlDeploymentPackage[dps.length];

		for (int i = 0; i < dps.length; i++) {
			DeploymentPackage dp = dps[i];

			XmlDeploymentPackage xdp = new XmlDeploymentPackage();
			xdp.setName(dp.getName());
			xdp.setVersion(dp.getVersion().toString());

			BundleInfo[] bis = dp.getBundleInfos();
			XmlBundleInfo[] axbi = new XmlBundleInfo[bis.length];

			for (int j = 0; j < bis.length; j++) {

				BundleInfo bi = bis[j];
				XmlBundleInfo xbi = new XmlBundleInfo();
				xbi.setName(bi.getSymbolicName());
				xbi.setVersion(bi.getVersion().toString());

				axbi[j] = xbi;
			}

			xdp.setBundleInfos(axbi);

			axdp[i] = xdp;
		}

		xdps.setDeploymentPackages(axdp);

		try {
			String s = XmlUtil.marshal(xdps);

			//s_logger.info("Getting resource {}: {}", RESOURCE_PACKAGES, s);
			response.setTimestamp(new Date());

			try {
				response.setBody(s.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
		} catch (Exception e) {
			s_logger.error("Error getting resource {}: {}", RESOURCE_PACKAGES, e);
		}
	}

	private void doGetBundles(KuraRequestPayload request, KuraResponsePayload response) {
		Bundle[] bundles = m_bundleContext.getBundles();
		XmlBundles xmlBundles = new XmlBundles();
		XmlBundle[] axb = new XmlBundle[bundles.length];

		for (int i = 0; i < bundles.length; i++) {

			Bundle bundle = bundles[i];
			XmlBundle xmlBundle = new XmlBundle();

			xmlBundle.setName(bundle.getSymbolicName());
			xmlBundle.setVersion(bundle.getVersion().toString());
			xmlBundle.setId(bundle.getBundleId());

			int state = bundle.getState();

			switch(state) {
			case Bundle.UNINSTALLED:
				xmlBundle.setState("UNINSTALLED");
				break;

			case Bundle.INSTALLED:
				xmlBundle.setState("INSTALLED");
				break;

			case Bundle.RESOLVED:
				xmlBundle.setState("RESOLVED");
				break;

			case Bundle.STARTING:
				xmlBundle.setState("STARTING");
				break;

			case Bundle.STOPPING:
				xmlBundle.setState("STOPPING");
				break;

			case Bundle.ACTIVE:
				xmlBundle.setState("ACTIVE");
				break;

			default:
				xmlBundle.setState(String.valueOf(state));
			}

			axb[i] = xmlBundle;
		}

		xmlBundles.setBundles(axb);

		try {
			String s = XmlUtil.marshal(xmlBundles);

			//s_logger.info("Getting resource {}: {}", RESOURCE_BUNDLES, s);
			response.setTimestamp(new Date());

			try {
				response.setBody(s.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
		} catch (Exception e) {
			s_logger.error("Error getting resource {}: {}", RESOURCE_BUNDLES, e);
		}
	}

	private DeploymentPackage installDeploymentPackageInternal(File fileReference, DeploymentPackageOptions options) 
			throws DeploymentException, IOException {

		InputStream dpInputStream = null;
		DeploymentPackage dp = null;
		File dpPersistentFile = null;
		File downloadedFile = fileReference;

		try {
			String dpBasename = fileReference.getName();
			String dpPersistentFilePath = m_packagesPath + File.separator + dpBasename;
			dpPersistentFile = new File(dpPersistentFilePath);
			//downloadedFile = getDpDownloadFile(options);


			dpInputStream = new FileInputStream(downloadedFile);
			dp = m_deploymentAdmin.installDeploymentPackage(dpInputStream);

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

	private void addPackageToConfFile(String packageName, String packageUrl) {
		m_deployedPackages.setProperty(packageName, packageUrl);

		if (m_dpaConfPath == null) {
			s_logger.warn("Configuration file not specified");
			return;
		}

		try {
			FileOutputStream fos = new FileOutputStream(m_dpaConfPath);
			m_deployedPackages.store(fos, null);
			fos.flush();
			fos.getFD().sync();
			fos.close();
		} catch (IOException e) {
			s_logger.error("Error writing package configuration file", e);
		}
	}

	private Properties loadInstallPersistance(File installedDpPersistance){
		Properties downloadProperies= new Properties();
		try {
			downloadProperies.load(new FileReader(installedDpPersistance));
		} catch (IOException e) {
			s_logger.error("Exception loading install configuration file", e);
		}
		return downloadProperies;
	}

	private void updateInstallPersistance(String fileName, DeploymentPackageOptions options){
		m_installPersistance = new Properties();
		m_installPersistance.setProperty(DeploymentPackageOptions.METRIC_DP_CLIENT_ID, options.getClientId());
		m_installPersistance.setProperty(DeploymentPackageOptions.METRIC_JOB_ID, Long.toString(options.getJobId()));
		m_installPersistance.setProperty(DeploymentPackageOptions.METRIC_DP_NAME, fileName);
		m_installPersistance.setProperty(DeploymentPackageOptions.METRIC_DP_VERSION, options.getDpVersion());
		m_installPersistance.setProperty(METRIC_REQUESTER_CLIENT_ID, options.getRequestClientId());
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

	private void incrementalDownloadFromURL(File dpFile, DeploymentPackageDownloadOptions options, String url, int downloadIndex) throws Exception {
		OutputStream os = null;

		try {
			os = new FileOutputStream(dpFile);
			downloadHelper = new HttpDownloadCountingOutputStream(os, options, this, m_sslManagerService, url, downloadIndex);
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
				if(hashAlgorithm == null || hashValue== null || !checksum.equals(hashValue)){
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR, null, "Failed to verify checksum with algorithm: " + hashAlgorithm);
				}
			}catch(Exception e){
				dpFile.delete();
				throw e;
			}
		}
	}

	private void downloadDeploymentPackageInternal(DeploymentPackageDownloadOptions options, boolean alreadyDownloaded) throws KuraException{
		File dpFile = null;
		int downloadIndex = 0;
		boolean downloadSuccess= true;
		try {
			// Download the package to a temporary file.
			// Check for file existence has already been done
			dpFile = getDpDownloadFile(options);
			boolean forceDownload = options.isDownloadForced();

			if (!alreadyDownloaded || forceDownload) {
				s_logger.info("To download");
				incrementalDownloadFromURL(dpFile, options, options.getDeployUrl(), downloadIndex);
				downloadIndex++;

				if(options.getVerifierURL() != null){
					File dpVerifier= getDpVerifierFile(options);
					incrementalDownloadFromURL(dpVerifier, options, options.getVerifierURL(), downloadIndex);
				}
			} else {
				alreadyDownloadedAsync(options);
			}
		} catch (Exception e) {
			s_logger.info("Download exception");
			downloadSuccess= false;
			downloadFailedAsync(options, e, downloadIndex);
		} 

		try{
			if (downloadSuccess && dpFile != null && options.isInstall()) {
				s_logger.info("Ready to install");
				installDownloadedFile(dpFile, options);
			}
		} catch (Exception e) {
			s_logger.info("Install exception");
			installFailedAsync(options, dpFile.getName(), e);
		} 
	}

	private void installDownloadedFile(File dpFile, DeploymentPackageInstallOptions options) throws KuraException {
		if(options.getSystemUpdate()){
			installSh(options, dpFile);
		} else {
			installDp(options, dpFile);
		}
	}

	private void installDp(DeploymentPackageInstallOptions options, File dpFile) throws KuraException{
		SafeProcess proc = null;
		try {
			installDeploymentPackageInternal(dpFile, options);
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
			if (proc != null) ProcessUtil.destroy(proc);
			dpFile = null;
		}
	}

	private void installSh(DeploymentPackageOptions options, File shFile) throws KuraException{	

		updateInstallPersistance(shFile.getName(), options);

		//Esecuzione script
		SafeProcess proc = null;
		try {
			proc = ProcessUtil.exec("chmod +x " + shFile.getCanonicalPath());
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
		} finally {
			if (proc != null) ProcessUtil.destroy(proc);
		}

		SafeProcess proc2 = null;
		try {
			proc2 = ProcessUtil.exec(shFile.getCanonicalPath());
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
		} finally {
			if (proc2 != null) ProcessUtil.destroy(proc2);
		}
	}


	private boolean deploymentPackageAlreadyDownloaded(DeploymentPackageInstallOptions options) throws KuraException {
		try {
			File dp = getDpDownloadFile(options);

			return dp.exists();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	private void alreadyDownloadedAsync(DeploymentPackageOptions options){
		KuraNotifyPayload notify = null;

		notify = new KuraNotifyPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setTransferSize(0);
		notify.setTransferProgress(100);
		notify.setTransferStatus(DOWNLOAD_STATUS.COMPLETED.getStatusString());
		notify.setJobId(options.getJobId());


		try {
			getCloudApplicationClient().controlPublish(options.getRequestClientId(), "NOTIFY/"+options.getClientId()+"/progress", notify, 2, DFLT_RETAIN, DFLT_PRIORITY);
		} catch (KuraException e) {
			s_logger.error("Error publishing response for command {} {}", RESOURCE_DOWNLOAD, e);
		}
	}

	private void downloadFailedAsync(DeploymentPackageOptions options, Exception e, int downloadIndex) {
		KuraNotifyPayload notify = null;

		notify = new KuraNotifyPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setTransferSize(0);
		notify.setTransferProgress(100);
		notify.setTransferStatus(DOWNLOAD_STATUS.FAILED.getStatusString());
		notify.setJobId(options.getJobId());
		notify.setErrorMessage(e.getMessage());
		notify.setTransferIndex(downloadIndex);

		try {
			getCloudApplicationClient().controlPublish(options.getRequestClientId(), "NOTIFY/"+options.getClientId()+"/progress", notify, 2, DFLT_RETAIN, DFLT_PRIORITY);
		} catch (KuraException e1) {
			s_logger.error("Error publishing response for command {} {}", RESOURCE_DOWNLOAD, e1);
		}
	}

	private void downloadInProgressSyncMessage(KuraResponsePayload respPayload) {
		respPayload.setTimestamp(new Date());
		respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_SIZE, downloadHelper.getTotalBytes().intValue());
		respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_PROGRESS, downloadHelper.getDownloadTransferProgressPercentage().intValue());
		respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_STATUS, downloadHelper.getDownloadTransferStatus().getStatusString());
		respPayload.addMetric(KuraNotifyPayload.METRIC_JOB_ID, m_downloadOptions.getJobId());
	}

	private void downloadAlreadyDoneSyncMessage(KuraResponsePayload respPayload) {
		respPayload.setTimestamp(new Date());
		respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_SIZE, 0);
		respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_PROGRESS, 100);
		respPayload.addMetric(KuraNotifyPayload.METRIC_TRANSFER_STATUS, DOWNLOAD_STATUS.ALREADY_DONE);
		//respPayload.addMetric(METRIC_JOB_ID, m_options.getJobId());
	}

	private void installInProgressSyncMessage(KuraResponsePayload respPayload) {
		respPayload.setTimestamp(new Date());
		respPayload.addMetric(KuraInstallPayload.METRIC_INSTALL_STATUS, INSTALL_STATUS.IN_PROGRESS);
		respPayload.addMetric(KuraInstallPayload.METRIC_DP_NAME, m_installOptions.getDpName());
		respPayload.addMetric(KuraInstallPayload.METRIC_DP_VERSION, m_installOptions.getDpVersion());
	}

	private void installIdleSyncMessage(KuraResponsePayload respPayload) {
		respPayload.setTimestamp(new Date());
		respPayload.addMetric(KuraInstallPayload.METRIC_INSTALL_STATUS, INSTALL_STATUS.IDLE);
	}

	private void installCompleteAsync(DeploymentPackageOptions options, String dpName) throws KuraException{
		KuraInstallPayload notify = null;

		notify = new KuraInstallPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setInstallStatus(INSTALL_STATUS.COMPLETED.getStatusString());
		notify.setJobId(options.getJobId());
		notify.setDpName(dpName); //Probably split dpName and dpVersion?
		notify.setInstallProgress(100);

		getCloudApplicationClient().controlPublish(options.getRequestClientId(), "NOTIFY/"+options.getClientId()+"/install", notify, 2, DFLT_RETAIN, DFLT_PRIORITY);
	}

	private void installFailedAsync(DeploymentPackageInstallOptions options, String dpName, Exception e) throws KuraException{
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

		getCloudApplicationClient().controlPublish(options.getRequestClientId(), "NOTIFY/"+options.getClientId()+"/install", notify, 2, DFLT_RETAIN, DFLT_PRIORITY);
	}

	private void sendInstallConfirmations(){
		s_logger.info("Ready to send Confirmations");
		File verificationDir= new File(m_installVerificationDir);
		for (File fileEntry : verificationDir.listFiles()) {
			if (fileEntry.isFile() && fileEntry.getName().endsWith(".sh")) {
				SafeProcess proc = null;
				try {
					proc = ProcessUtil.exec("chmod +x " + fileEntry.getCanonicalPath());
				} catch (IOException e) {

				} finally {
					if (proc != null) ProcessUtil.destroy(proc);
				}

				SafeProcess proc2 = null;
				try {
					proc2 = ProcessUtil.exec(fileEntry.getCanonicalPath());
					int exitValue = proc2.exitValue();
					if(exitValue == 0){
						sendSysUpdateSuccess(fileEntry);
					} else {
						sendSysUpdateFailure(fileEntry);
					}
				} catch (Exception e) {

				} finally {
					fileEntry.delete();
					if (proc2 != null) ProcessUtil.destroy(proc2);
				}

			}
		}
	}

	private void sendSysUpdateSuccess(File verificationFile) throws KuraException {
		s_logger.info("Ready to send success after install");
		File installDir= new File(m_installPersistanceDir);
		for (File fileEntry : installDir.listFiles()) {

			if (fileEntry.isFile() && fileEntry.getName().endsWith(PERSISTANCE_SUFFIX)) { //&& fileEntry.getName().contains(verificationFile.getName()
				Properties downloadProperties= loadInstallPersistance(fileEntry);
				String deployUrl= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI);
				String dpName= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_NAME);
				String dpVersion= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_VERSION);
				String clientId= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_CLIENT_ID);
				Long jobId= Long.valueOf(downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_JOB_ID));
				String fileSystemFileName= downloadProperties.getProperty(PERSISTANCE_FILE_NAME);
				String requestClientId = downloadProperties.getProperty(METRIC_REQUESTER_CLIENT_ID);

				DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(deployUrl, dpName, dpVersion);
				options.setClientId(clientId);
				options.setJobId(jobId);
				options.setRequestClientId(requestClientId);

				try {
					installCompleteAsync(options, fileSystemFileName);
					s_logger.info("Sent install complete");
					fileEntry.delete();
					break;
				} catch (KuraException e) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
				}
			}
		}
	}

	private void sendSysUpdateFailure(File verificationFile) throws KuraException {
		File installDir= new File(m_installPersistanceDir);
		for (final File fileEntry : installDir.listFiles()) {
			if (fileEntry.isFile() && fileEntry.getName().endsWith(PERSISTANCE_SUFFIX)) { //&& fileEntry.getName().contains(verificationFile.getName())
				Properties downloadProperties= loadInstallPersistance(fileEntry);
				String deployUrl= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_DOWNLOAD_URI);
				String dpName= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_NAME);
				String dpVersion= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_VERSION);
				String clientId= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_CLIENT_ID);
				Long jobId= Long.valueOf(downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_JOB_ID));
				String fileSystemFileName= downloadProperties.getProperty(PERSISTANCE_FILE_NAME);
				String requestClientId = downloadProperties.getProperty(METRIC_REQUESTER_CLIENT_ID);

				DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(deployUrl, dpName, dpVersion);
				options.setClientId(clientId);
				options.setJobId(jobId);
				options.setRequestClientId(requestClientId);

				try {
					installFailedAsync(options, fileSystemFileName, new KuraException(KuraErrorCode.INTERNAL_ERROR));
					s_logger.info("Sent install failed");
					fileEntry.delete();
					break;
				} catch (KuraException e) {
					throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
				}
			}
		}
	}

	private void uninstallCompleteAsync(DeploymentPackageUninstallOptions options, String dpName) throws KuraException{
		KuraUninstallPayload notify = null;

		notify = new KuraUninstallPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setUninstallStatus(UNINSTALL_STATUS.COMPLETED.getStatusString());
		notify.setJobId(options.getJobId());
		notify.setDpName(dpName); //Probably split dpName and dpVersion?
		notify.setUninstallProgress(100);

		getCloudApplicationClient().controlPublish(options.getRequestClientId(), "NOTIFY/"+options.getClientId()+"/uninstall", notify, 2, DFLT_RETAIN, DFLT_PRIORITY);
	}

	private void uninstallFailedAsync(DeploymentPackageUninstallOptions options, String dpName, Exception e) throws KuraException{
		KuraUninstallPayload notify = null;

		notify = new KuraUninstallPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setUninstallStatus(UNINSTALL_STATUS.FAILED.getStatusString());
		notify.setJobId(options.getJobId());
		notify.setDpName(dpName); //Probably split dpName and dpVersion?
		notify.setUninstallProgress(0);
		if (e != null){
			notify.setErrorMessage(e.getMessage());
		}

		getCloudApplicationClient().controlPublish(options.getRequestClientId(), "NOTIFY/"+options.getClientId()+"/uninstall", notify, 2, DFLT_RETAIN, DFLT_PRIORITY);
	}


	//File Management
	private File getDpDownloadFile(DeploymentPackageInstallOptions options) throws IOException {
		// File dpFile = File.createTempFile("dpa", null);
		String packageFilename = null;
		if(!options.getSystemUpdate()){
			String dpName= getFileName(options.getDpName(), options.getDpVersion(), ".dp");
			packageFilename = new StringBuilder().append(File.separator)
					.append("tmp")
					.append(File.separator)
					.append(dpName)
					.toString();
		} else {
			String shName= getFileName(options.getDpName(), options.getDpVersion(), ".sh");
			packageFilename = new StringBuilder().append(File.separator)
					.append("tmp")
					.append(File.separator)
					.append(shName)
					.toString();
		}

		File dpFile = new File(packageFilename);
		return dpFile;
	}

	private File getDpVerifierFile(DeploymentPackageInstallOptions options) throws IOException {
		// File dpFile = File.createTempFile("dpa", null);
		String packageFilename = null;

		String shName= getFileName(options.getDpName(), options.getDpVersion(), "_verifier.sh");
		packageFilename = new StringBuilder().append(m_installVerificationDir)
				.append(File.separator)
				.append(shName)
				.toString();

		File dpFile = new File(packageFilename);
		return dpFile;
	}

	private String getFileName(String dpName, String dpVersion, String extension) {
		String packageFilename = null;
		packageFilename = new StringBuilder().append(dpName).append("-")
				.append(dpVersion)
				.append(extension).toString();
		return packageFilename;
	}
}
