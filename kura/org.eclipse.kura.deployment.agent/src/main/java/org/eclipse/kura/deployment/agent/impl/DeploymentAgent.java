/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.deployment.agent.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.deployment.agent.DeploymentPackageDownloadOptions;
import org.eclipse.kura.ssl.SslManagerService;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cdealti
 *
 *         The bundles installed from deployment packages are managed by the
 *         deployment admin itself. Once installed they are persisted in the
 *         persistent storage area provided by the framework. The persistent
 *         storage area is wiped up if the framework is stated with the '-clean'
 *         option. The way deployment packages and their bundles are stored in
 *         the persistence storage area is implementation dependent and we
 *         should not rely on that.
 *
 *         In order to be able to reinstall deployment packages across reboots
 *         of the framework with the '-clean' option set, we need to store the
 *         deployment package files (.dp) in a different persistent location.
 *
 *         Limitations: We should also keep the entire installation history.
 *         This is needed because deployment packages can be partially upgraded
 *         through 'fix packages' and these must be reinstalled in the right
 *         order. We DO NOT support this yet. We assume that for every installed
 *         deployment package there is a single deployment package file (.dp)
 *         that needs to be reinstalled.
 */
public class DeploymentAgent implements DeploymentAgentService {
	private static Logger s_logger = LoggerFactory.getLogger(DeploymentAgent.class);

	private static final String DPA_CONF_PATH_PROPNAME = "dpa.configuration";
	private static final String KURA_CONF_URL_PROPNAME = "kura.configuration";
	private static final String PACKAGES_PATH_PROPNAME = "kura.packages";

	private static final String CONN_TIMEOUT_PROPNAME = "dpa.connection.timeout";
	private static final String READ_TIMEOUT_PROPNAME = "dpa.read.timeout";

	private final static long THREAD_TERMINATION_TOUT = 1; // in seconds

	// private static Future<?> s_installerTask;
	private static Future<?> s_uninstallerTask;
	private static Future<?> s_downloaderTask;

	private DeploymentAdmin m_deploymentAdmin;
	private EventAdmin m_eventAdmin;
	private SslManagerService m_sslManagerService;

	// private Queue<String> m_instPackageUrls;
	private Queue<String> m_uninstPackageNames;
	private Queue<DeploymentPackageDownloadOptions> m_downloadPackageOptions;

	// private ExecutorService m_installerExecutor;
	private ExecutorService m_uninstallerExecutor;
	private ExecutorService m_downloaderExecutor;

	private String m_dpaConfPath;
	private String m_packagesPath;

	private Properties m_deployedPackages;

	private int m_connTimeout;
	private int m_readTimeout;

	protected void activate(ComponentContext componentContext) {

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

		String sConnTimeout = kuraProperties.getProperty(CONN_TIMEOUT_PROPNAME);
		if (sConnTimeout != null) {
			m_connTimeout = Integer.valueOf(sConnTimeout);
		}

		String sReadTimeout = kuraProperties.getProperty(READ_TIMEOUT_PROPNAME);
		if (sReadTimeout != null) {
			m_readTimeout = Integer.valueOf(sReadTimeout);
		}

		File dpaConfFile = new File(m_dpaConfPath);
		if (dpaConfFile.getParentFile() != null && !dpaConfFile.getParentFile().exists()) {
			dpaConfFile.getParentFile().mkdirs();
		}
		if (!dpaConfFile.exists()) {
			try {
				dpaConfFile.createNewFile();
			} catch (IOException e) {
				throw new ComponentException("Cannot create empty DPA configuration file", e);
			}
		}

		File packagesDir = new File(m_packagesPath);
		if (!packagesDir.exists()) {
			if (!packagesDir.mkdir()) {
				throw new ComponentException("Cannot create packages directory");
			}
		}

		// m_instPackageUrls = new ConcurrentLinkedQueue<String>();
		m_uninstPackageNames = new ConcurrentLinkedQueue<String>();
		m_downloadPackageOptions = new ConcurrentLinkedQueue<DeploymentPackageDownloadOptions>();

		// m_installerExecutor = Executors.newSingleThreadExecutor();

		m_uninstallerExecutor = Executors.newSingleThreadExecutor();

		m_downloaderExecutor = Executors.newSingleThreadExecutor();

		// s_installerTask = m_installerExecutor.submit(new Runnable() {
		// @Override
		// public void run() {
		// Thread.currentThread().setName("DeploymentAgent:Install");
		// installer();
		// }
		// });

		s_uninstallerTask = m_uninstallerExecutor.submit(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("DeploymentAgent:Uninstall");
				uninstaller();
			}
		});

		s_downloaderTask = m_downloaderExecutor.submit(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("DeploymentAgent:Download");
				downloader();
			}
		});

		installPackagesFromConfFile();
	}

	protected void deactivate(ComponentContext componentContext) {
		m_dpaConfPath = null;

		m_deployedPackages = null;

		// if ((s_installerTask != null) && (!s_installerTask.isDone())) {
		// s_logger.debug("Cancelling DeploymentAgent task ...");
		// s_installerTask.cancel(true);
		// s_logger.info("DeploymentAgent task cancelled? = {}",
		// s_installerTask.isDone());
		// s_installerTask = null;
		// }

		// if (m_installerExecutor != null) {
		// s_logger.debug("Terminating DeploymentAgent Thread ...");
		// m_installerExecutor.shutdownNow();
		// try {
		// m_installerExecutor.awaitTermination(THREAD_TERMINATION_TOUT,
		// TimeUnit.SECONDS);
		// } catch (InterruptedException e) {
		// s_logger.warn("Interrupted", e);
		// }
		// s_logger.info("DeploymentAgent Thread terminated? - {}",
		// m_installerExecutor.isTerminated());
		// m_installerExecutor = null;
		// }

		if ((s_uninstallerTask != null) && (!s_uninstallerTask.isDone())) {
			s_logger.debug("Cancelling DeploymentAgent:Uninstall task ...");
			s_uninstallerTask.cancel(true);
			s_logger.info("DeploymentAgent:Uninstall task cancelled? = {}", s_uninstallerTask.isDone());
			s_uninstallerTask = null;
		}

		if (m_uninstallerExecutor != null) {
			s_logger.debug("Terminating DeploymentAgent:Uninstall Thread ...");
			m_uninstallerExecutor.shutdownNow();
			try {
				m_uninstallerExecutor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				s_logger.warn("Interrupted", e);
			}
			s_logger.info("DeploymentAgent:Uninstall Thread terminated? - {}", m_uninstallerExecutor.isTerminated());
			m_uninstallerExecutor = null;
		}

		if ((s_downloaderTask != null) && (!s_downloaderTask.isDone())) {
			s_logger.debug("Cancelling DeploymentAgent:Download task ...");
			s_downloaderTask.cancel(true);
			s_logger.info("DeploymentAgent:Download task cancelled? = {}", s_downloaderTask.isDone());
			s_downloaderTask = null;
		}

		if (m_downloaderExecutor != null) {
			s_logger.debug("Terminating DeploymentAgent:Download Thread ...");
			m_downloaderExecutor.shutdownNow();
			try {
				m_downloaderExecutor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				s_logger.warn("Interrupted", e);
			}
			s_logger.info("DeploymentAgent:Download Thread terminated? - {}", m_downloaderExecutor.isTerminated());
			m_downloaderExecutor = null;
		}

		m_uninstPackageNames = null;
		// m_instPackageUrls = null;
		m_downloadPackageOptions = null;
	}

	public void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
		m_deploymentAdmin = deploymentAdmin;
	}

	public void unsetDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
		m_deploymentAdmin = null;
	}

	protected void setEventAdmin(EventAdmin eventAdmin) {
		m_eventAdmin = eventAdmin;
	}

	protected void unsetEventAdmin(EventAdmin eventAdmin) {
		m_eventAdmin = null;
	}

	protected void setSslManagerService(SslManagerService sslManagerService) {
		m_sslManagerService = sslManagerService;
	}

	protected void unsetSslManagerService(SslManagerService sslManagerService) {
		m_sslManagerService = null;
	}

	@Override
	public void installDeploymentPackageAsync(String url) throws Exception {
		throw new Exception("Operation not supported!");

		// if (m_instPackageUrls.contains(url)) {
		// throw new Exception("Element already exists");
		// }
		//
		// m_instPackageUrls.offer(url);
		// synchronized (m_instPackageUrls) {
		// m_instPackageUrls.notifyAll();
		// }
	}

	@Override
	public void uninstallDeploymentPackageAsync(String name) throws Exception {
		if (m_uninstPackageNames.contains(name)) {
			throw new Exception("Element already exists");
		}

		m_uninstPackageNames.offer(name);
		synchronized (m_uninstPackageNames) {
			m_uninstPackageNames.notifyAll();
		}
	}

	@Override
	public boolean isInstallingDeploymentPackage(String url) {

		DeploymentPackageDownloadOptions[] downloadList = m_downloadPackageOptions.toArray(new DeploymentPackageDownloadOptions[0]);
		for (DeploymentPackageDownloadOptions element : downloadList) {
			if (element.getDeployUrl().equals(url)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isUninstallingDeploymentPackage(String name) {
		if (m_uninstPackageNames.contains(name)) {
			return true;
		}
		return false;
	}

	private void downloader() {
		do {
			try {
				try {
					synchronized (m_downloadPackageOptions) {
						m_downloadPackageOptions.wait();
					}

					while (!m_downloadPackageOptions.isEmpty()) {
						DeploymentPackageDownloadOptions options = m_downloadPackageOptions.peek();
						if (options != null) {
							s_logger.info("About to download package at URL {}", options.getDeployUrl());
							DeploymentPackage dp = null;
							Exception ex = null;
							try {
								dp = downloadDeploymentPackageInternal(options);
							} catch (Exception e) {
								ex = e;
								s_logger.error("Exception donwloading package at URL {}", options.getDeployUrl(), e);
							} finally {
								boolean successful = dp != null ? true : false;
								s_logger.info("Posting PROGRESS event for package at URL {}: status={}", options.getDeployUrl(), successful ? "successful"
										: "unsuccessful");
								m_downloadPackageOptions.poll();
								postProgressEvent(options, successful ? 0 : 1, ex);
							}
						}
					}
				} catch (InterruptedException e) {
					s_logger.info("Exiting...");
					Thread.interrupted();
					return;
				}
			} catch (Throwable t) {
				s_logger.error("Unexpected throwable", t);
			}
		} while (true);
	}

	// private void installer() {
	// do {
	// try {
	// try {
	// synchronized (m_instPackageUrls) {
	// m_instPackageUrls.wait();
	// }
	//
	// while (!m_instPackageUrls.isEmpty()) {
	// String url = m_instPackageUrls.peek();
	// if (url != null) {
	// s_logger.info("About to install package at URL {}", url);
	// DeploymentPackage dp = null;
	// Exception ex = null;
	// try {
	// dp = installDeploymentPackageInternal(url);
	// } catch (Exception e) {
	// ex = e;
	// s_logger.error("Exception installing package at URL {}", url, e);
	// } finally {
	// boolean successful = dp != null ? true : false;
	// s_logger.info("Posting INSTALLED event for package at URL {}: {}", url,
	// successful ? "successful" : "unsuccessful");
	// m_instPackageUrls.poll();
	// postInstalledEvent(dp, url, successful, ex);
	// }
	// }
	// }
	// } catch (InterruptedException e) {
	// s_logger.info("Exiting...");
	// Thread.interrupted();
	// return;
	// }
	// } catch (Throwable t) {
	// s_logger.error("Unexpected throwable", t);
	// }
	// } while (true);
	// }

	private void uninstaller() {
		do {
			try {
				try {
					synchronized (m_uninstPackageNames) {
						m_uninstPackageNames.wait();
					}

					while (!m_uninstPackageNames.isEmpty()) {
						String name = m_uninstPackageNames.peek();
						if (name != null) {
							s_logger.info("About to uninstall package ", name);
							DeploymentPackage dp = null;
							boolean successful = false;
							Exception ex = null;
							try {
								dp = m_deploymentAdmin.getDeploymentPackage(name);
								if (dp != null) {
									dp.uninstall();

									String sUrl = m_deployedPackages.getProperty(name);
									File dpFile = new File(new URL(sUrl).getPath());
									if (!dpFile.delete()) {
										s_logger.warn("Cannot delete file at URL: {}", sUrl);
									}
									successful = true;
									removePackageFromConfFile(name);
								}
							} catch (Exception e) {
								ex = e;
								s_logger.error("Exception uninstalling package {}", name, e);
							} finally {
								s_logger.info("Posting UNINSTALLED event for package {}: {}", name, successful ? "successful" : "unsuccessful");
								m_uninstPackageNames.poll();
								postUninstalledEvent(name, successful, ex);
							}
						}
					}
				} catch (InterruptedException e) {
					s_logger.info("Exiting...");
					Thread.interrupted();
					return;
				}
			} catch (Throwable t) {
				s_logger.error("Unexpected throwable", t);
			}
		} while (true);
	}

	private void postProgressEvent(DeploymentPackageDownloadOptions options, int downloadStatus, Exception e) {
		Map<String, Object> props = new HashMap<String, Object>();

		props.put(EVENT_PACKAGE_NAME, options.getDpName());
		Version version = new Version(options.getDpVersion());
		props.put(EVENT_PACKAGE_VERSION, version.toString());
		props.put(EVENT_PACKAGE_URL, options.getDeployUrl());
		props.put(EVENT_PROGRESS_STATUS, downloadStatus);
		props.put(EVENT_EXCEPTION, e);
		EventProperties eventProps = new EventProperties(props);
		m_eventAdmin.postEvent(new Event(EVENT_PROGRESS_TOPIC, eventProps));

	}

	private void postInstalledEvent(DeploymentPackage dp, String url, boolean successful, Exception e) {
		Map<String, Object> props = new HashMap<String, Object>();

		if (dp != null) {
			props.put(EVENT_PACKAGE_NAME, dp.getName());
			Version version = dp.getVersion();
			props.put(EVENT_PACKAGE_VERSION, version.toString());
		} else {
			props.put(EVENT_PACKAGE_NAME, "UNKNOWN");
			props.put(EVENT_PACKAGE_VERSION, "UNKNOWN");
		}
		props.put(EVENT_PACKAGE_URL, url);
		props.put(EVENT_SUCCESSFUL, successful);
		props.put(EVENT_EXCEPTION, e);
		EventProperties eventProps = new EventProperties(props);
		m_eventAdmin.postEvent(new Event(EVENT_INSTALLED_TOPIC, eventProps));
	}

	private void postUninstalledEvent(String name, boolean successful, Exception e) {
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(EVENT_PACKAGE_NAME, name);
		props.put(EVENT_SUCCESSFUL, successful);
		props.put(EVENT_EXCEPTION, e);
		EventProperties eventProps = new EventProperties(props);
		m_eventAdmin.postEvent(new Event(EVENT_UNINSTALLED_TOPIC, eventProps));
	}

	private void installPackagesFromConfFile() {

		if (m_dpaConfPath != null) {
			try {
				m_deployedPackages.load(new FileReader(m_dpaConfPath));
			} catch (IOException e) {
				s_logger.error("Exception loading deployment packages configuration file", e);
			}
		}

		Set<Object> packageNames = m_deployedPackages.keySet();
		for (Object packageName : packageNames) {
			String packageUrl = (String) m_deployedPackages.get(packageName);

			s_logger.info("Deploying package name {} at URL {}", packageName, packageUrl);
			try {
				installDeploymentPackageAsync(packageUrl);
			} catch (Exception e) {
				s_logger.error("Error installing package {}", packageName, e);
			}
		}
	}

	private void incrementalDownloadFromURL(File dpFile, DeploymentPackageDownloadOptions options) {
		OutputStream os = null;

		try {
			os = new FileOutputStream(dpFile);

			DownloadCountingOutputStream dcount = new DownloadCountingOutputStream(os, options, m_eventAdmin, m_sslManagerService);

			dcount.startWork();

			dcount.close();

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

	private File getDpDownloadFile(DeploymentPackageDownloadOptions options) throws IOException{
		File dpFile = File.createTempFile("dpa", null);
		String packageFilename = new StringBuilder()
			.append(options.getDpName())
			.append(File.separator)
			.append(options.getDpVersion())
			.append(".dp").toString();
		
		dpFile = new File(dpFile, packageFilename);
		//dpFile.deleteOnExit();

		return dpFile;
	}
	
	private DeploymentPackage installDeploymentPackageInternal(DeploymentPackageDownloadOptions options) 
			throws DeploymentException, IOException {

		InputStream dpInputStream = null;
		DeploymentPackage dp = null;
		File dpPersistentFile = null;
		File downloadedFile = null;
		
		try {
			String dpBasename = options.getDpName() + "_" + options.getDpVersion() + ".dp";
			String dpPersistentFilePath = m_packagesPath + File.separator + dpBasename;
			dpPersistentFile = new File(dpPersistentFilePath);
			downloadedFile = getDpDownloadFile(options);
			

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

	private DeploymentPackage downloadDeploymentPackageInternal(DeploymentPackageDownloadOptions options) throws DeploymentException, IOException,
			URISyntaxException {

		DeploymentPackage dp = null;
		File dpFile = null;
		try {
			// Download the package to a temporary file.
			// Check for file existence has already been done
			dpFile = getDpDownloadFile(options);

			incrementalDownloadFromURL(dpFile, options);

			if(options.isInstall()){
				dp = installDeploymentPackageInternal(options);
			}

		} catch (DeploymentException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			dpFile = null;
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

	@Override
	public void installDeploymentPackageAsync(DeploymentPackageDownloadOptions options) throws Exception {
		// TODO Auto-generated method stub

	}
}
