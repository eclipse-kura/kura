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
import org.eclipse.kura.core.deployment.install.DeploymentPackageInstallOptions;
import org.eclipse.kura.core.deployment.install.KuraInstallPayload;
import org.eclipse.kura.core.deployment.progress.ProgressEvent;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.core.deployment.xml.XmlBundle;
import org.eclipse.kura.core.deployment.xml.XmlBundleInfo;
import org.eclipse.kura.core.deployment.xml.XmlBundles;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackage;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackages;
import org.eclipse.kura.core.deployment.xml.XmlUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.message.KuraNotifyPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.ssl.SslManagerService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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
	private static final String APP_ID = "DEPLOY-V2";

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
	public static final String RESOURCE_CANCEL = "cancel";

	/* Metrics in the REPLY to RESOURCE_DOWNLOAD */
	public static final String METRIC_DOWNLOAD_STATUS = "download.status";

	private static final String METRIC_CLIENT_ID = "client.id";
	private static final String METRIC_TRASNFER_SIZE = "dp.http.transfer.size";
	private static final String METRIC_TRANSFER_PROGRESS = "dp.http.transfer.progress";
	private static final String METRIC_TRANSFER_STATUS = "dp.http.transfer.status";
	private static final String METRIC_JOB_ID = "job.id";

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

	private static String s_pendingPackageUrl = null;

	private SslManagerService m_sslManagerService;
	private DeploymentAdmin   m_deploymentAdmin;

	private DownloadCountingOutputStream downloadHelper;

	private static ExecutorService executor = Executors.newSingleThreadExecutor();


	private Future<?> downloaderFuture;

	private BundleContext m_bundleContext;

	private DataTransportService m_dataTransportService;

	private Properties m_deployedPackages;
	private Properties m_installPersistance;
	private String m_dpaConfPath;
	private String m_packagesPath;
	private String persistanceFileName= "persistance";

	private String m_installPersistanceDir;
	private DeploymentPackageDownloadOptions m_options;



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
		// TODO Auto-generated method stub
		s_logger.info("Cloud Deployment v2 is starting");
		super.activate(componentContext);

		m_bundleContext = componentContext.getBundleContext();

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
		m_installPersistanceDir= kuraDataDir + File.separator + persistanceFileName;
		File installPersistanceDir = new File(m_installPersistanceDir);
		if (!installPersistanceDir.exists()) {
			installPersistanceDir.mkdir();
		}


		sendInstallConfirmations();
	}

	@Override
	protected void deactivate(ComponentContext componentContext) {
		s_logger.info("Bundle " + APP_ID + " is deactivating!");
		if(downloaderFuture != null){
			downloaderFuture.cancel(true);
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
		}else {
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
		m_options = options;

		if (s_pendingPackageUrl != null) {
			s_logger.info("Another request seems still pending: {}. Checking if stale...", s_pendingPackageUrl);

			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
			response.setTimestamp(new Date());
			response.addMetric(METRIC_DOWNLOAD_STATUS, DOWNLOAD_STATUS.IN_PROGRESS);
			try {
				response.setBody("Only one request at a time is allowed".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
			return;
		}

		boolean alreadyDownloaded = false;
		boolean forceDownload = options.isDownloadForced();

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
		final boolean forceDownloadFinal = forceDownload;

		s_logger.info("About to download and install package at URL {}", options.getDeployUrl());

		try {
			s_pendingPackageUrl = options.getDeployUrl();
			// m_pendingInstRequestId = request.getRequestId();
			// m_pendingInstRequesterClientId = request.getRequesterClientId();

			String clientId= m_dataTransportService.getClientId();

			options.setClientId(clientId);
			options.setRequestClientId(request.getRequesterClientId());

			s_logger.info("Downloading package from URL: " + options.getDeployUrl());


			downloaderFuture = executor.submit(new Runnable(){

				@Override
				public void run() {
					try {
						downloadDeploymentPackageInternal(options, alreadyDownloadedFinal, forceDownloadFinal);
					} catch (Exception e) {

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

		if(alreadyDownloaded){
			//Check if file exists
			File dpFile;
			try {
				dpFile = getDpDownloadFile(options);
				//if yes, install
				installDownloadedFile(dpFile, options);
			} catch (IOException e) {
				response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
				response.setException(e);
				response.setTimestamp(new Date());
				try {
					response.setBody("Exception during install".getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e1) {
				}
			} catch (KuraException e) {
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
				response.setBody("The requested file does not exist in this device".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
			return;
		}
	}

	private void doGetInstall(KuraRequestPayload reqPayload, KuraResponsePayload respPayload) {
		// TODO Auto-generated method stub

	}

	private void doGetDownload(KuraRequestPayload reqPayload, KuraResponsePayload respPayload) {
		// TODO Auto-generated method stub

		if (s_pendingPackageUrl != null){ //A download is pending
			downloadInProgressMessage(respPayload);
		} else { //No pending downloads
			downloadEndedMessage(respPayload); //is it right? Do we remove the last object
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

			response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK); 
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

			response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK); 
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
		m_installPersistance.setProperty(PERSISTANCE_FILE_NAME, fileName);

		if (m_installPersistanceDir == null) {
			s_logger.warn("Configuration file not specified");
			return;
		}

		try {
			String persistanceFile= m_installPersistanceDir + File.separator + fileName;
			FileOutputStream fos = new FileOutputStream(persistanceFile);
			m_installPersistance.store(fos, null);
			fos.flush();
			fos.getFD().sync();
			fos.close();
		} catch (IOException e) {
			s_logger.error("Error writing remote install configuration file", e);
		}
	}

	private void incrementalDownloadFromURL(File dpFile, DeploymentPackageDownloadOptions options) {
		OutputStream os = null;

		try {
			os = new FileOutputStream(dpFile);

			downloadHelper = new DownloadCountingOutputStream(os, options, this, m_sslManagerService);

			downloadHelper.startWork();

			downloadHelper.close();

		} catch (Exception e) {
			System.out.println(e);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}

	private File getDpDownloadFile(DeploymentPackageOptions options) throws IOException {
		// File dpFile = File.createTempFile("dpa", null);
		String packageFilename = null;
		if(!options.getSystemUpdate()){
			packageFilename = new StringBuilder().append(File.separator).append("tmp").append(File.separator).append(options.getDpName()).append("-")
					.append(options.getDpVersion()).append(".dp").toString();
		} else {
			packageFilename = new StringBuilder().append(File.separator).append("tmp").append(File.separator).append(options.getDpName()).append("-")
					.append(options.getDpVersion()).append(".sh").toString();
		}

		File dpFile = new File(packageFilename);
		return dpFile;
	}

	private void downloadDeploymentPackageInternal(DeploymentPackageDownloadOptions options, boolean alreadyDownloaded, boolean forceDownload) throws Exception {

		File dpFile = null;
		try {
			// Download the package to a temporary file.
			// Check for file existence has already been done
			dpFile = getDpDownloadFile(options);

			if (!alreadyDownloaded || forceDownload) {
				incrementalDownloadFromURL(dpFile, options);
			}

			if (options.isInstall()) {
				installDownloadedFile(dpFile, options);
			}
		} catch (Exception e) {
			throw e;
		} 
	}

	private void installDownloadedFile(File dpFile, DeploymentPackageOptions options) throws KuraException {
		if(options.getSystemUpdate()){
			installSh(options, dpFile);
		} else {
			installDp(options, dpFile);
		}
	}

	private void installDp(DeploymentPackageOptions options, File dpFile) throws KuraException{
		try {
			installDeploymentPackageInternal(dpFile, options);
			installComplete(options, dpFile.getName());
			s_logger.info("Install completed!");

			if(options.isReboot()){
				Thread.sleep(options.getRebootDelay());
				ProcessUtil.exec("reboot");
			}
		} catch (Exception e) {
			s_logger.info("Install failed!");
			installFailed(options, dpFile.getName());
		} finally {
			dpFile = null;
		}
	}

	private void installSh(DeploymentPackageOptions options, File shFile) throws KuraException{	

		updateInstallPersistance(shFile.getName(), options);

		//Esecuzione script
		try {
			ProcessUtil.exec(shFile.getAbsolutePath());
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
		}
	}


	private boolean deploymentPackageAlreadyDownloaded(DeploymentPackageOptions options) throws KuraException {
		try {
			File dp = getDpDownloadFile(options);

			return dp.exists();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}
	}

	private void downloadInProgressMessage(KuraResponsePayload respPayload) {
		respPayload.setTimestamp(new Date());
		respPayload.addMetric(METRIC_TRASNFER_SIZE, downloadHelper.getTotalBytes().intValue());
		respPayload.addMetric(METRIC_TRANSFER_PROGRESS, downloadHelper.getDownloadTransferProgressPercentage().intValue());
		respPayload.addMetric(METRIC_TRANSFER_STATUS, downloadHelper.getDownloadTransferStatus().getStatusString());
		respPayload.addMetric(METRIC_JOB_ID, m_options.getJobId());
	}

	private void downloadEndedMessage(KuraResponsePayload respPayload) {
		respPayload.setTimestamp(new Date());
		respPayload.addMetric(METRIC_TRASNFER_SIZE, 0);
		respPayload.addMetric(METRIC_TRANSFER_PROGRESS, 100);
		respPayload.addMetric(METRIC_TRANSFER_STATUS, DOWNLOAD_STATUS.ALREADY_DONE);
		//respPayload.addMetric(METRIC_JOB_ID, m_options.getJobId());
	}

	private void installComplete(DeploymentPackageOptions options, String dpName) throws KuraException{
		KuraInstallPayload notify = null;

		notify = new KuraInstallPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setInstallStatus(INSTALL_STATUS.COMPLETED.getStatusString());
		notify.setJobId(options.getJobId());
		notify.setDpName(dpName);
		notify.setInstallProgress(100);

		getCloudApplicationClient().controlPublish(options.getRequestClientId(), "NOTIFY/"+options.getClientId()+"/install", notify, 2, DFLT_RETAIN, DFLT_PRIORITY);
	}

	private void installFailed(DeploymentPackageOptions options, String dpName) throws KuraException{
		KuraInstallPayload notify = null;

		notify = new KuraInstallPayload(options.getClientId());
		notify.setTimestamp(new Date());
		notify.setInstallStatus(INSTALL_STATUS.FAILED.getStatusString());
		notify.setJobId(options.getJobId());
		notify.setDpName(dpName);
		notify.setInstallProgress(0);

		getCloudApplicationClient().controlPublish(options.getRequestClientId(), "NOTIFY/"+options.getClientId()+"/install", notify, 2, DFLT_RETAIN, DFLT_PRIORITY);
	}

	private void sendInstallConfirmations(){
		File installDir= new File(m_installPersistanceDir);
		for (final File fileEntry : installDir.listFiles()) {
			if (fileEntry.isFile() && (fileEntry.getName().endsWith(".dp") || fileEntry.getName().endsWith(".sh"))) {
				Properties downloadProperties= loadInstallPersistance(fileEntry);
				String deployUrl= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DEPLOY_URL);
				String dpName= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_NAME);
				String dpVersion= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_VERSION);
				String clientId= downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_DP_CLIENT_ID);
				Long jobId= Long.valueOf(downloadProperties.getProperty(DeploymentPackageDownloadOptions.METRIC_JOB_ID));
				String fileSystemFileName= downloadProperties.getProperty(PERSISTANCE_FILE_NAME);

				DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(deployUrl, dpName, dpVersion);
				options.setClientId(clientId);
				options.setJobId(jobId);

				try {
					installComplete(options, fileSystemFileName);
				} catch (KuraException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
