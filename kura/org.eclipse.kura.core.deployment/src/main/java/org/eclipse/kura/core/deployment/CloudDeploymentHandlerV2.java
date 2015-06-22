package org.eclipse.kura.core.deployment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.core.configuration.util.XmlUtil;
import org.eclipse.kura.core.deployment.progress.ProgressEvent;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.message.KuraNotifyPayload;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.ssl.SslManagerService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudDeploymentHandlerV2 extends Cloudlet implements ProgressListener {

	/*
	 * Risorsa di prova:
	 * 
	 * http://esfdownload.eurotech-inc.com/update_site/esf3/3.0.2/
	 * user_workspace_archive_3.0.2.zip
	 */
	private static final String APP_ID = "DEPLOY-V2";
	
	public static final String  RESOURCE_PACKAGES = "packages";
	public static final String  RESOURCE_BUNDLES  = "bundles";

	private static final Logger s_logger = LoggerFactory.getLogger(CloudDeploymentHandlerV2.class);

	/* EXEC */
	public static final String RESOURCE_DOWNLOAD = "download";
	public static final String RESOURCE_INSTALL = "install";
	public static final String RESOURCE_CANCEL = "cancel";

	/* Metrics in the REPLY to RESOURCE_DOWNLOAD */
	public static final String METRIC_DOWNLOAD_STATUS = "download.status";

	/**
	 * Enum representing the different status of the download process
	 * 
	 * {@link DeploymentAgentService.DOWNLOAD_STATUS.PROGRESS} Download in
	 * progress {@link DeploymentAgentService.DOWNLOAD_STATUS.COMPLETE} Download
	 * completed {@link DeploymentAgentService.DOWNLOAD_STATUS.FAILED} Download
	 * failed
	 */
	public static enum DOWNLOAD_STATUS {
		PROGRESS("PROGRESS"), COMPLETE("COMPLETE"), FAILED("FAILED"), ALREADY_DONE("ALREADY DONE");

		private final String status;

		DOWNLOAD_STATUS(String status) {
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

	public CloudDeploymentHandlerV2() {
		super(APP_ID);
	}

	@Override
	protected void activate(ComponentContext componentContext) {
		// TODO Auto-generated method stub
		super.activate(componentContext);

		m_bundleContext = componentContext.getBundleContext();
		
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(5000);
					s_logger.info("STARTING DOWNLOAD...");
					CloudletTopic ct = CloudletTopic.parseAppTopic("EXEC/download");
					KuraRequestPayload request = new KuraRequestPayload();
					request.setRequestId("RequestID");
					request.setRequesterClientId("RequesterClientId");
					String url = "http://esfdownload.eurotech-inc.com/update_site/esf3/3.0.2/user_workspace_archive_3.0.2.zip";
					DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(url, "dpName", "dpVersion");
					options.setUsername("luca.dazi@eurotech.com");
					options.setPassword("lc2251981");
					// options.setPassword("errata");
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_DEPLOY_URL, options.getDeployUrl());
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_NAME, options.getDpName());
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_VERSION, options.getDpVersion());
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_HTTP_USER, options.getUsername());
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_HTTP_PASSWORD, options.getPassword());
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_BLOCK_SIZE, 1024 * 8);
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_NOTIFY_BLOCK_SIZE, 1024 * 1024);

					KuraResponsePayload response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

					doExec(ct, request, response);
					
					s_logger.info("*******************************************");
					s_logger.info(response.getMetric(KuraResponsePayload.METRIC_RESPONSE_CODE).toString());
					if(response.getBody() != null){
						s_logger.info(new String(response.getBody()));
					}
					if(response.getMetric(METRIC_DOWNLOAD_STATUS) != null){
						s_logger.info(response.getMetric(METRIC_DOWNLOAD_STATUS).toString());
					}
					s_logger.info("*******************************************");
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (KuraException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});


		Thread t3 = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(10000);
					s_logger.info("STARTING DOWNLOAD 2...");
					CloudletTopic ct = CloudletTopic.parseAppTopic("EXEC/download");
					KuraRequestPayload request = new KuraRequestPayload();
					request.setRequestId("RequestID");
					request.setRequesterClientId("RequesterClientId");
					String url = "http://esfdownload.eurotech-inc.com/update_site/esf3/3.0.2/user_workspace_archive_3.0.2.zip";
					DeploymentPackageDownloadOptions options = new DeploymentPackageDownloadOptions(url, "dpName", "dpVersion");
					options.setUsername("luca.dazi@eurotech.com");
					options.setPassword("lc2251981");
					// options.setPassword("errata");
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_DEPLOY_URL, options.getDeployUrl());
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_NAME, options.getDpName());
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_VERSION, options.getDpVersion());
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_HTTP_USER, options.getUsername());
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_HTTP_PASSWORD, options.getPassword());
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_BLOCK_SIZE, 1024 * 8);
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_NOTIFY_BLOCK_SIZE, 1024 * 1024);

					KuraResponsePayload response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

					doExec(ct, request, response);
					
					s_logger.info("*******************************************");
					s_logger.info(response.getMetric(KuraResponsePayload.METRIC_RESPONSE_CODE).toString());
					if(response.getBody() != null){
						s_logger.info(new String(response.getBody()));
					}
					if(response.getMetric(METRIC_DOWNLOAD_STATUS) != null){
						s_logger.info(response.getMetric(METRIC_DOWNLOAD_STATUS).toString());
					}
					s_logger.info("*******************************************");
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (KuraException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});

		//t.start();

		//t3.start();
		
		Thread t2 = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(30000);
					s_logger.info("CANCELLING DOWNLOAD...");
					CloudletTopic ct = CloudletTopic.parseAppTopic("EXEC/cancel");
					KuraRequestPayload request = new KuraRequestPayload();
					request.setRequestId("RequestID");
					request.setRequesterClientId("RequesterClientId");

					KuraResponsePayload response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);

					request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_NAME, "dpName");
					request.addMetric(DeploymentPackageDownloadOptions.METRIC_DP_VERSION, "dpVersion");

					doExec(ct, request, response);

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (KuraException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		});

		//t2.start();
	}

	
	private void doGetPackages(KuraPayload request, KuraResponsePayload response) {
		
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
			
			s_logger.info("Getting resource {}: {}", RESOURCE_PACKAGES, s);
			
			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK); 
			response.setTimestamp(new Date());
			
			try {
				response.setBody(s.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
		} catch (JAXBException e) {
			s_logger.error("Error getting resource {}: {}", RESOURCE_PACKAGES, e);
		}
		
	}

	private void doGetBundles(KuraPayload request, KuraResponsePayload response) {

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
			
			s_logger.info("Getting resource {}: {}", RESOURCE_BUNDLES, s);
			
			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK); 
			response.setTimestamp(new Date());
			
			try {
				response.setBody(s.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Ignore
			}
		} catch (JAXBException e) {
			s_logger.error("Error getting resource {}: {}", RESOURCE_BUNDLES, e);
		}

		
	}

	private void doGetResource(CloudletTopic requestTopic, KuraPayload request) {

		KuraResponsePayload response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
		response.setTimestamp(new Date());
		
		
		String resourceName = requestTopic.getResources()[0];
		
		if (resourceName == null) {

			s_logger.info("GET: null resource name");
			
			response = new KuraResponsePayload(
					KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
			
			response.setTimestamp(new Date());

		} else if (resourceName.equals(RESOURCE_PACKAGES)) {
			doGetPackages(request, response);
		} else if (resourceName.equals(RESOURCE_BUNDLES)) {
			doGetBundles(request, response);
		} else {
			s_logger.info("Resource {} not found", resourceName);
			
			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND); 			
			response.setTimestamp(new Date());
		}
	}

	@Override
	protected void doGet(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {
		
		doGetResource(reqTopic, reqPayload);
		
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

			// doExecInstall(reqPayload, respPayload);

		} else {
			s_logger.error("Bad request topic: {}", reqTopic.toString());
			s_logger.error("Cannot find resource with name: {}", resources[0]);
			respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
			return;
		}
	}

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

		if (s_pendingPackageUrl != null) {
			s_logger.info("Antother request seems still pending: {}. Checking if stale...", s_pendingPackageUrl);

			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
			response.setTimestamp(new Date());
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

		if (alreadyDownloaded) {
			response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
			response.addMetric(METRIC_DOWNLOAD_STATUS, DOWNLOAD_STATUS.ALREADY_DONE.getStatusString());
			try {
				response.setBody("Already downloaded!".getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
			return;
		}

		s_logger.info("About to download and install package at URL {}", options.getDeployUrl());

		try {
			s_pendingPackageUrl = options.getDeployUrl();
			// m_pendingInstRequestId = request.getRequestId();
			// m_pendingInstRequesterClientId = request.getRequesterClientId();
			options.setClientId(request.getRequestId());
			options.setRequestClientId(request.getRequesterClientId());

			s_logger.info("Downloading package from URL: " + options.getDeployUrl());
			
			
			downloaderFuture = executor.submit(new Runnable(){

				@Override
				public void run() {
					try {
						downloadDeploymentPackageInternal(options);
					} catch (IOException e) {
						
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

	private File getDpDownloadFile(DeploymentPackageDownloadOptions options) throws IOException {
		// File dpFile = File.createTempFile("dpa", null);
		String packageFilename = new StringBuilder().append(File.separator).append("tmp").append(File.separator).append(options.getDpName()).append("-")
				.append(options.getDpVersion()).append(".dp").toString();

		File dpFile = new File(packageFilename);
		// dpFile.deleteOnExit();

		return dpFile;
	}

	private DeploymentPackage downloadDeploymentPackageInternal(DeploymentPackageDownloadOptions options) throws IOException {

		DeploymentPackage dp = null;
		File dpFile = null;
		try {
			// Download the package to a temporary file.
			// Check for file existence has already been done
			dpFile = getDpDownloadFile(options);

			incrementalDownloadFromURL(dpFile, options);

			if (options.isInstall()) {
				// dp = installDeploymentPackageInternal(options);
			}

			// } catch (DeploymentException e) {
			// throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			dpFile = null;
		}

		return dp;
	}

	/**
	 * Checks if the deployment package contained in the download request has
	 * already been downloaded and is thus present in the temporary storage
	 * 
	 * @param request
	 *            Request payload as sent by the cloud platform
	 * @return true is the package has already been deployed
	 * @throws KuraException
	 *             {@link org.eclipse.kura.KuraInvalidMessageException} if the
	 *             request doesn't contain dp name and version
	 *             {@link org.eclipse.kura.KuraException} if failing in checking
	 *             the existance of the file
	 */
	private boolean deploymentPackageAlreadyDownloaded(DeploymentPackageDownloadOptions options) throws KuraException {

		try {
			File dp = getDpDownloadFile(options);

			return dp.exists();
		} catch (Exception e) {
			throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
		}

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

		try {
			getCloudApplicationClient().controlPublish(progress.getRequesterClientId(), "NOTIFY/progress", notify, 2, DFLT_RETAIN, DFLT_PRIORITY);
		} catch (KuraException e) {
			s_logger.error("Error publishing response for command {} {}", RESOURCE_DOWNLOAD, e);
		}
	}

}
