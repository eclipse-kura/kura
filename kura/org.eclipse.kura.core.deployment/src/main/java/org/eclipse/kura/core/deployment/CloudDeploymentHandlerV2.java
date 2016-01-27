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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.core.deployment.download.DeploymentPackageDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadCountingOutputStream;
import org.eclipse.kura.core.deployment.download.DownloadFileUtilities;
import org.eclipse.kura.core.deployment.download.impl.DownloadImpl;
import org.eclipse.kura.core.deployment.install.DeploymentPackageInstallOptions;
import org.eclipse.kura.core.deployment.install.InstallImpl;
import org.eclipse.kura.core.deployment.uninstall.DeploymentPackageUninstallOptions;
import org.eclipse.kura.core.deployment.uninstall.UninstallImpl;
import org.eclipse.kura.core.deployment.xml.XmlBundle;
import org.eclipse.kura.core.deployment.xml.XmlBundleInfo;
import org.eclipse.kura.core.deployment.xml.XmlBundles;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackage;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackages;
import org.eclipse.kura.core.deployment.xml.XmlUtil;
import org.eclipse.kura.core.util.ThrowableUtil;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.message.KuraPayload;
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
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudDeploymentHandlerV2 extends Cloudlet {

	private static final Logger s_logger = LoggerFactory.getLogger(CloudDeploymentHandlerV2.class);
	public static final String APP_ID = "DEPLOY-V2";

	private static final String DPA_CONF_PATH_PROPNAME = "dpa.configuration";
	private static final String KURA_CONF_URL_PROPNAME = "kura.configuration";
	private static final String PACKAGES_PATH_PROPNAME = "kura.packages";
	private static final String KURA_DATA_DIR = "kura.data";


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


	/**
	 * Enum representing the different status of the download process
	 * 
	 * {@link DeploymentAgentService.DOWNLOAD_STATUS.PROGRESS} Download in
	 * progress {@link DeploymentAgentService.DOWNLOAD_STATUS.COMPLETE} Download
	 * completed {@link DeploymentAgentService.DOWNLOAD_STATUS.FAILED} Download
	 * failed
	 */
	public enum DOWNLOAD_STATUS {
		IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED"), FAILED("FAILED"), ALREADY_DONE("ALREADY DONE"), CANCELLED("CANCELLED");

		private final String status;

		DOWNLOAD_STATUS(String status) {
			this.status = status;
		}

		public String getStatusString() {
			return status;
		}
	}

	public enum INSTALL_STATUS {
		IDLE("IDLE"), IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED"), FAILED("FAILED"), ALREADY_DONE("ALREADY DONE");

		private final String status;

		INSTALL_STATUS(String status) {
			this.status = status;
		}

		public String getStatusString() {
			return status;
		}
	}

	public enum UNINSTALL_STATUS {
		IDLE("IDLE"), IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED"), FAILED("FAILED"), ALREADY_DONE("ALREADY DONE");

		private final String status;

		UNINSTALL_STATUS(String status) {
			this.status = status;
		}

		public String getStatusString() {
			return status;
		}
	}

	private static String        s_pendingPackageUrl = null;
	private static DownloadImpl  s_downloadImplementation;
	private static UninstallImpl s_uninstallImplementation;
	public  static InstallImpl   s_installImplementation;

	private SslManagerService m_sslManagerService;
	private DeploymentAdmin   m_deploymentAdmin;

	private static ExecutorService executor = Executors.newSingleThreadExecutor();


	private Future<?> downloaderFuture;
	private Future<?> installerFuture;

	private BundleContext m_bundleContext;

	private DataTransportService m_dataTransportService;

	private String m_dpaConfPath;
	private String m_packagesPath;

	private DeploymentPackageDownloadOptions m_downloadOptions;

	private boolean m_isInstalling = false;
	private DeploymentPackageInstallOptions m_installOptions;

	private String m_pendingUninstPackageName;
	private String m_installVerificationDir;


	public CloudDeploymentHandlerV2() {
		super(APP_ID);
	}

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
		if (    kuraProperties.getProperty(PACKAGES_PATH_PROPNAME) != null && 
				"kura/packages".equals(kuraProperties.getProperty(PACKAGES_PATH_PROPNAME).trim())
				) {
			kuraProperties.setProperty(PACKAGES_PATH_PROPNAME, "/opt/eclipse/kura/kura/packages");
			m_packagesPath = kuraProperties.getProperty(PACKAGES_PATH_PROPNAME);
			s_logger.warn("Overridding invalid kura.packages location");
		}

		String kuraDataDir= kuraProperties.getProperty(KURA_DATA_DIR);

		s_installImplementation = new InstallImpl(this, kuraDataDir);
		s_installImplementation.setPackagesPath(m_packagesPath);
		s_installImplementation.setDpaConfPath(m_dpaConfPath);
		s_installImplementation.setDeploymentAdmin(m_deploymentAdmin);
		s_installImplementation.sendInstallConfirmations();
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

	public void publishMessage(DeploymentPackageOptions options, KuraPayload messagePayload, String messageType){
		try {
			String messageTopic = new StringBuilder("NOTIFY/").append(options.getClientId())
					.append("/")
					.append(messageType)
					.toString();

			getCloudApplicationClient().controlPublish(options.getRequestClientId(), messageTopic, messagePayload, 2, DFLT_RETAIN, DFLT_PRIORITY);
		} catch (KuraException e) {
			s_logger.error("Error publishing response for command {} {}", messageType, e);
		}
	}



	// ----------------------------------------------------------------
	//
	// Protected methods
	//
	// ----------------------------------------------------------------

	@Override
	protected void doGet(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {

		String[] resources = reqTopic.getResources();

		if (resources == null || resources.length == 0) {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Expected one resource but found {}", resources != null ? resources.length : "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}

		if (resources[0].equals(RESOURCE_DOWNLOAD)) {
			doGetDownload(respPayload);
		} else if (resources[0].equals(RESOURCE_INSTALL)) {
			doGetInstall(respPayload);
		} else if (resources[0].equals(RESOURCE_PACKAGES)) {
			doGetPackages(respPayload);
		} else if (resources[0].equals(RESOURCE_BUNDLES)) {
			doGetBundles(respPayload);
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
		} else if (resources[0].equals(RESOURCE_INSTALL)) {
			doExecInstall(reqPayload, respPayload);
		} else if (resources[0].equals(RESOURCE_UNINSTALL)) {
			doExecUninstall(reqPayload, respPayload);
		} else if (resources[0].equals(RESOURCE_START)) {
			String bundleId = resources[1];
			doExecStartStopBundle(respPayload, true, bundleId);
		} else if (resources[0].equals(RESOURCE_STOP)) {
			String bundleId = resources[1];
			doExecStartStopBundle(respPayload, false, bundleId);
		} else {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Cannot find resource with name: {}", resources[0]);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			return;
		}
	}

	@Override
	protected void doDel(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {

		String[] resources = reqTopic.getResources();

		if (resources == null || resources.length == 0) {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Expected one resource but found {}", resources != null ? resources.length : "none");
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			return;
		}

		if (resources[0].equals(RESOURCE_DOWNLOAD)) {
			doDelDownload(reqPayload, respPayload);
		} else {
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

	private void doDelDownload(KuraRequestPayload request, KuraResponsePayload response) {

		try{
			DownloadCountingOutputStream downloadHelper= s_downloadImplementation.getDownloadHelper();
			if(downloadHelper != null){
				downloadHelper.cancelDownload();
				s_downloadImplementation.deleteDownloadedFile();
			}
		} catch(Exception ex){
			String errMsg = "Error cancelling download!";
			s_logger.warn(errMsg, ex);
			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
			try {
				response.setBody(errMsg.getBytes("UTF-8"));
				response.setException(ex);
			} catch (UnsupportedEncodingException uee) {
			}
		}

	}

	private void doExecDownload(KuraRequestPayload request, KuraResponsePayload response) {

		final DeploymentPackageDownloadOptions options;
		try {
			options = new DeploymentPackageDownloadOptions(request);
			options.setClientId(m_dataTransportService.getClientId());
			s_downloadImplementation= new DownloadImpl(options, this);
		} catch (Exception ex) {
			s_logger.info("Malformed download request!");
			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
			try {
				response.setBody("Malformed download request".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				s_logger.info("Unsupported encoding");
			}
			response.setException(ex);

			return;
		}
		m_downloadOptions = options;

		if (s_pendingPackageUrl != null) {
			s_logger.info("Another request seems for the same URL is pending: {}.", s_pendingPackageUrl);

			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
			response.addMetric(METRIC_DOWNLOAD_STATUS, DOWNLOAD_STATUS.IN_PROGRESS.getStatusString());
			try {
				response.setBody("Another resource is already in download".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
			return;
		}

		boolean alreadyDownloaded = false;

		try {
			alreadyDownloaded = s_downloadImplementation.isAlreadyDownloaded();
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

		s_logger.info("About to download and install package at URL {}", options.getDeployUri());

		try {
			s_pendingPackageUrl = options.getDeployUri();

			s_downloadImplementation.setSslManager(m_sslManagerService);
			s_downloadImplementation.setAlreadyDownloadedFlag(alreadyDownloaded);
			s_downloadImplementation.setVerificationDirectory(m_installVerificationDir);

			s_logger.info("Downloading package from URL: " + options.getDeployUri());


			downloaderFuture = executor.submit(new Runnable(){

				@Override
				public void run() {
					try {

						s_downloadImplementation.downloadDeploymentPackageInternal();
					} catch (KuraException e) {
						try {
							File dpFile = DownloadFileUtilities.getDpDownloadFile(options);
							if (dpFile != null){
								dpFile.delete();
							}
						} catch (IOException e1) {
						}
					} finally{
						s_pendingPackageUrl = null;
					}
				}
			});

		} catch (Exception e) {
			s_logger.error("Failed to download and install package at URL {}: {}", options.getDeployUri(), e);

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
			options.setClientId(m_dataTransportService.getClientId());
		} catch (Exception ex) {
			s_logger.error("Malformed install request!");
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
			alreadyDownloaded = s_downloadImplementation.isAlreadyDownloaded();
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
				final File dpFile = DownloadFileUtilities.getDpDownloadFile(options);

				s_installImplementation.setOptions(options);

				//if yes, install

				installerFuture = executor.submit(new Runnable(){

					@Override
					public void run() {
						try {
							installDownloadedFile(dpFile, m_installOptions);
						} catch (KuraException e) {
							s_logger.error("Impossible to send an exception message to the cloud platform");
							if (dpFile != null){
								dpFile.delete();
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
			options.setClientId(m_dataTransportService.getClientId());
		} catch (Exception ex) {
			s_logger.error("Malformed uninstall request!");
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
			s_logger.info("Another request seems still pending: {}. Checking if stale...", m_pendingUninstPackageName);

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
				s_uninstallImplementation= new UninstallImpl(this, m_deploymentAdmin);

				s_logger.info("Uninstalling package...");
				installerFuture = executor.submit(new Runnable(){

					@Override
					public void run() {
						try {
							s_uninstallImplementation.uninstaller(options, packageName);
						} catch (Exception e) {
							try {
								s_uninstallImplementation.uninstallFailedAsync(options, packageName, e);
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

	private void doExecStartStopBundle(KuraResponsePayload response, boolean start, String bundleId) {
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

	private void doGetInstall(KuraResponsePayload respPayload) {
		if(m_isInstalling){
			s_installImplementation.installInProgressSyncMessage(respPayload);
		} else {
			s_installImplementation.installIdleSyncMessage(respPayload);
		}
	}

	private void doGetDownload(KuraResponsePayload respPayload) {
		if (s_pendingPackageUrl != null){ //A download is pending
			DownloadCountingOutputStream downloadHelper= s_downloadImplementation.getDownloadHelper();
			DownloadImpl.downloadInProgressSyncMessage(respPayload, downloadHelper, m_downloadOptions);
		} else { //No pending downloads
			DownloadImpl.downloadAlreadyDoneSyncMessage(respPayload); //is it right? Do we remove the last object
		}
	}

	private void doGetPackages(KuraResponsePayload response) {
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
			response.setTimestamp(new Date());
			response.setBody(s.getBytes("UTF-8"));
		} catch (Exception e) {
			s_logger.error("Error getting resource {}: {}", RESOURCE_PACKAGES, e);
		}
	}

	private void doGetBundles(KuraResponsePayload response) {
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
			response.setTimestamp(new Date());
			response.setBody(s.getBytes("UTF-8"));
		} catch (Exception e) {
			s_logger.error("Error getting resource {}: {}", RESOURCE_BUNDLES, e);
		}
	}

	public void installDownloadedFile(File dpFile, DeploymentPackageInstallOptions options) throws KuraException {
		try{
			if(options.getSystemUpdate()){
				s_installImplementation.installSh(options, dpFile);
			} else {
				s_installImplementation.installDp(options, dpFile);
			}
		} catch (Exception e) {
			s_logger.info("Install exception");
			s_installImplementation.installFailedAsync(options, dpFile.getName(), e);
		}
	}
}
