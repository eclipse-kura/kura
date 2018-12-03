/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc - Clean up kura properties handling
 *******************************************************************************/
package org.eclipse.kura.deployment.agent.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
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

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.deployment.agent.DeploymentAgentService;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.system.SystemService;
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
 *         The bundles installed from deployment packages are managed by the deployment admin itself.
 *         Once installed they are persisted in the persistent storage area provided by the framework.
 *         The persistent storage area is wiped up if the framework is stated with the '-clean' option.
 *         The way deployment packages and their bundles are stored in the persistence storage area
 *         is implementation dependent and we should not rely on that.
 *
 *         In order to be able to reinstall deployment packages across reboots of the framework with
 *         the '-clean' option set, we need to store the deployment package files (.dp) in a different
 *         persistent location.
 *
 *         Limitations:
 *         We should also keep the entire installation history. This is needed because deployment
 *         packages can be partially upgraded through 'fix packages' and these must be reinstalled in the
 *         right order.
 *         We DO NOT support this yet. We assume that for every installed deployment package
 *         there is a single deployment package file (.dp) that needs to be reinstalled.
 */
public class DeploymentAgent implements DeploymentAgentService {

    private static Logger logger = LoggerFactory.getLogger(DeploymentAgent.class);

    private static final String DPA_CONF_PATH_PROPNAME = "dpa.configuration";

    private static final String PACKAGES_PATH_PROPNAME = "kura.packages";

    private static final String CONN_TIMEOUT_PROPNAME = "dpa.connection.timeout";
    private static final String READ_TIMEOUT_PROPNAME = "dpa.read.timeout";

    private static final long THREAD_TERMINATION_TOUT = 1; // in seconds

    private static Future<?> installerTask;
    private static Future<?> uninstallerTask;

    private DeploymentAdmin deploymentAdmin;
    private EventAdmin eventAdmin;
    private SystemService systemService;

    private Queue<String> instPackageUrls;
    private Queue<String> uninstPackageNames;

    private ExecutorService installerExecutor;
    private ExecutorService uninstallerExecutor;

    private String dpaConfPath;
    private String packagesPath;

    private int connTimeout;
    private int readTimeout;

    private SslManagerService sslManagerService;

    public void setSslManagerService(SslManagerService sslManagerService) {
        this.sslManagerService = sslManagerService;
    }

    public void unsetSslManagerService(SslManagerService sslManagerService) {
        this.sslManagerService = null;
    }

    protected void activate(ComponentContext componentContext) {

        this.dpaConfPath = System.getProperty(DPA_CONF_PATH_PROPNAME);
        if (this.dpaConfPath == null || this.dpaConfPath.isEmpty()) {
            throw new ComponentException("The value of '" + DPA_CONF_PATH_PROPNAME + "' is not defined");
        }

        final Properties kuraProperties = this.systemService.getProperties();

        this.packagesPath = kuraProperties.getProperty(PACKAGES_PATH_PROPNAME);
        if (this.packagesPath == null || this.packagesPath.isEmpty()) {
            throw new ComponentException("The value of '" + PACKAGES_PATH_PROPNAME + "' is not defined");
        }
        if (kuraProperties.getProperty(PACKAGES_PATH_PROPNAME) != null
                && kuraProperties.getProperty(PACKAGES_PATH_PROPNAME).trim().equals("kura/packages")) {
            kuraProperties.setProperty(PACKAGES_PATH_PROPNAME, "/opt/eclipse/kura/kura/packages");
            this.packagesPath = kuraProperties.getProperty(PACKAGES_PATH_PROPNAME);
            logger.warn("Overridding invalid kura.packages location");
        }

        String sConnTimeout = kuraProperties.getProperty(CONN_TIMEOUT_PROPNAME);
        if (sConnTimeout != null) {
            this.connTimeout = Integer.valueOf(sConnTimeout);
        }

        String sReadTimeout = kuraProperties.getProperty(READ_TIMEOUT_PROPNAME);
        if (sReadTimeout != null) {
            this.readTimeout = Integer.valueOf(sReadTimeout);
        }

        File dpaConfFile = new File(this.dpaConfPath);
        if (dpaConfFile.getParentFile() != null && !dpaConfFile.getParentFile().exists()) {
            dpaConfFile.getParentFile().mkdirs();
        }
        try {
            if (!dpaConfFile.createNewFile()) {
                logger.debug("DPA configuration file already available");
            }
        } catch (IOException e) {
            throw new ComponentException("Cannot create empty DPA configuration file", e);
        }

        File packagesDir = new File(this.packagesPath);
        if (!packagesDir.exists() && !packagesDir.mkdirs()) {
            throw new ComponentException("Cannot create packages directory");
        }

        this.instPackageUrls = new ConcurrentLinkedQueue<>();
        this.uninstPackageNames = new ConcurrentLinkedQueue<>();

        this.installerExecutor = Executors.newSingleThreadExecutor();

        this.uninstallerExecutor = Executors.newSingleThreadExecutor();

        installerTask = this.installerExecutor.submit(() -> {
            Thread.currentThread().setName("DeploymentAgent");
            installer();
        });

        uninstallerTask = this.uninstallerExecutor.submit(() -> {
            Thread.currentThread().setName("DeploymentAgent:Uninstall");
            uninstaller();
        });

        installPackagesFromConfFile();
    }

    protected void deactivate(ComponentContext componentContext) {
        if (installerTask != null && !installerTask.isDone()) {
            logger.debug("Cancelling DeploymentAgent task ...");
            installerTask.cancel(true);
            logger.info("DeploymentAgent task cancelled? = {}", installerTask.isDone());
            installerTask = null;
        }

        if (this.installerExecutor != null) {
            logger.debug("Terminating DeploymentAgent Thread ...");
            this.installerExecutor.shutdownNow();
            try {
                this.installerExecutor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted", e);
            }
            logger.info("DeploymentAgent Thread terminated? - {}", this.installerExecutor.isTerminated());
            this.installerExecutor = null;
        }

        if (uninstallerTask != null && !uninstallerTask.isDone()) {
            logger.debug("Cancelling DeploymentAgent:Uninstall task ...");
            uninstallerTask.cancel(true);
            logger.info("DeploymentAgent:Uninstall task cancelled? = {}", uninstallerTask.isDone());
            uninstallerTask = null;
        }

        if (this.uninstallerExecutor != null) {
            logger.debug("Terminating DeploymentAgent:Uninstall Thread ...");
            this.uninstallerExecutor.shutdownNow();
            try {
                this.uninstallerExecutor.awaitTermination(THREAD_TERMINATION_TOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted", e);
            }
            logger.info("DeploymentAgent:Uninstall Thread terminated? - {}", this.uninstallerExecutor.isTerminated());
            this.uninstallerExecutor = null;
        }

        this.dpaConfPath = null;
        this.uninstPackageNames = null;
        this.instPackageUrls = null;
    }

    public void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        this.deploymentAdmin = deploymentAdmin;
    }

    public void unsetDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        this.deploymentAdmin = null;
    }

    protected void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    protected void unsetEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    @Override
    public void installDeploymentPackageAsync(String url) throws Exception {
        if (this.instPackageUrls.contains(url)) {
            throw new Exception("Element already exists");
        }

        this.instPackageUrls.offer(url);
        synchronized (this.instPackageUrls) {
            this.instPackageUrls.notifyAll();
        }
    }

    @Override
    public void uninstallDeploymentPackageAsync(String name) throws Exception {
        if (this.uninstPackageNames.contains(name)) {
            throw new Exception("Element already exists");
        }

        this.uninstPackageNames.offer(name);
        synchronized (this.uninstPackageNames) {
            this.uninstPackageNames.notifyAll();
        }
    }

    @Override
    public boolean isInstallingDeploymentPackage(String url) {
        boolean result = false;
        if (this.instPackageUrls.contains(url)) {
            result = true;
        }
        return result;
    }

    @Override
    public boolean isUninstallingDeploymentPackage(String name) {
        boolean result = false;
        if (this.uninstPackageNames.contains(name)) {
            result = true;
        }
        return result;
    }

    private void installer() {
        do {
            try {
                while (this.instPackageUrls.isEmpty()) {
                    synchronized (this.instPackageUrls) {
                        this.instPackageUrls.wait();
                    }
                }

                String url = this.instPackageUrls.peek();
                if (url != null) {
                    logger.info("About to install package at URL {}", url);
                    execInstall(url);
                }
            } catch (InterruptedException e) {
                logger.info("Exiting...");
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                logger.error("Unexpected exception", e);
            }
        } while (true);
    }

    private void execInstall(String url) {
        DeploymentPackage dp = null;
        Exception ex = null;
        try {
            dp = installDeploymentPackageInternal(url);
        } catch (Exception e) {
            ex = e;
            logger.error("Exception installing package at URL {}", url, e);
        } finally {
            boolean successful = dp != null;
            logger.info("Posting INSTALLED event for package at URL {}: {}", url,
                    successful ? "successful" : "unsuccessful");
            this.instPackageUrls.poll();
            postInstalledEvent(dp, url, successful, ex);
        }
    }

    private void uninstaller() {
        do {
            try {
                while (this.uninstPackageNames.isEmpty()) {
                    synchronized (this.uninstPackageNames) {
                        this.uninstPackageNames.wait();
                    }
                }

                String name = this.uninstPackageNames.peek();
                if (name != null) {
                    logger.info("About to uninstall package {}", name);
                    execUninstall(name);
                }
            } catch (InterruptedException e) {
                logger.info("Exiting...");
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable t) {
                logger.error("Unexpected throwable", t);
            }
        } while (true);
    }

    private void execUninstall(String name) {
        DeploymentPackage dp = null;
        boolean successful = false;
        Exception ex = null;
        try {
            dp = this.deploymentAdmin.getDeploymentPackage(name);
            if (dp != null) {
                dp.uninstall();

                Properties deployedPackages = readDeployedPackages();
                String sUrl = deployedPackages.getProperty(name);
                File dpFile = new File(new URL(sUrl).getPath());
                if (!Files.deleteIfExists(dpFile.toPath())) {
                    logger.warn("Cannot delete file at URL: {}", sUrl);
                }
                successful = true;
                removePackageFromConfFile(name);
            }
        } catch (Exception e) {
            ex = e;
            logger.error("Exception uninstalling package {}", name, e);
        } finally {
            logger.info("Posting UNINSTALLED event for package {}: {}", name,
                    successful ? "successful" : "unsuccessful");
            this.uninstPackageNames.poll();
            postUninstalledEvent(name, successful, ex);
        }
    }

    private void postInstalledEvent(DeploymentPackage dp, String url, boolean successful, Exception e) {
        Map<String, Object> props = new HashMap<>();

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
        this.eventAdmin.postEvent(new Event(EVENT_INSTALLED_TOPIC, eventProps));
    }

    private void postUninstalledEvent(String name, boolean successful, Exception e) {
        Map<String, Object> props = new HashMap<>();
        props.put(EVENT_PACKAGE_NAME, name);
        props.put(EVENT_SUCCESSFUL, successful);
        props.put(EVENT_EXCEPTION, e);
        EventProperties eventProps = new EventProperties(props);
        this.eventAdmin.postEvent(new Event(EVENT_UNINSTALLED_TOPIC, eventProps));
    }

    private void installPackagesFromConfFile() {

        Properties deployedPackages = readDeployedPackages();

        Set<Object> packageNames = deployedPackages.keySet();
        for (Object packageName : packageNames) {
            String packageUrl = (String) deployedPackages.get(packageName);

            logger.info("Deploying package name {} at URL {}", packageName, packageUrl);
            try {
                installDeploymentPackageAsync(packageUrl);
            } catch (Exception e) {
                logger.error("Error installing package {}", packageName, e);
            }
        }
    }

    protected Properties readDeployedPackages() {
        Properties deployedPackages = new Properties();
        if (this.dpaConfPath != null) {
            try (FileReader fr = new FileReader(this.dpaConfPath)) {
                deployedPackages.load(fr);
            } catch (IOException e) {
                logger.error("Exception loading deployment packages configuration file", e);
            }
        }
        return deployedPackages;
    }

    private DeploymentPackage installDeploymentPackageInternal(String urlSpec)
            throws DeploymentException, IOException, GeneralSecurityException {
        URL url = new URL(urlSpec);
        File dpFile = null;
        if (!"file".equals(url.getProtocol())) {
            dpFile = getFileFromRemote(url);
        } else {
            dpFile = getFileFromFilesystem(url);
        }

        // Get the file base name from the URL
        String urlPath = url.getPath();
        String[] parts = urlPath.split("/");
        String dpBasename = parts[parts.length - 1];
        String dpPersistentFilePath = this.packagesPath + File.separator + dpBasename;
        File dpPersistentFile = new File(dpPersistentFilePath);

        DeploymentPackage dp = null;
        try (InputStream dpInputStream = new FileInputStream(dpFile);) {
            dp = this.deploymentAdmin.installDeploymentPackage(dpInputStream);

            // Now we need to copy the deployment package file to the Kura
            // packages directory unless it's already there.
            if (!dpFile.getCanonicalPath().equals(dpPersistentFile.getCanonicalPath())) {
                logger.debug("dpFile.getCanonicalPath(): {}", dpFile.getCanonicalPath());
                logger.debug("dpPersistentFile.getCanonicalPath(): {}", dpPersistentFile.getCanonicalPath());
                FileUtils.copyFile(dpFile, dpPersistentFile);
                addPackageToConfFile(dp.getName(), "file:" + dpPersistentFilePath);
            }
        } finally {
            // The file from which we have installed the deployment package will be deleted
            // unless it's a persistent deployment package file.
            if (!dpFile.getCanonicalPath().equals(dpPersistentFile.getCanonicalPath())) {
                Files.delete(dpFile.toPath());
                logger.debug("Deleted file: {}", dpFile.getName());
            }
        }

        return dp;
    }

    private File getFileFromRemote(URL url) throws GeneralSecurityException, IOException {

        File dpFile = File.createTempFile("dpa", null);
        dpFile.deleteOnExit();

        URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout(this.connTimeout);
        urlConnection.setReadTimeout(this.readTimeout);
        if (urlConnection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) urlConnection).setSSLSocketFactory(this.sslManagerService.getSSLSocketFactory());
        }

        try (InputStream is = urlConnection.getInputStream();) {
            FileUtils.copyInputStreamToFile(is, dpFile);
            return dpFile;
        }

    }

    private File getFileFromFilesystem(URL url) {
        return new File(url.getPath());
    }

    private void addPackageToConfFile(String packageName, String packageUrl) {
        Properties deployedPackages = readDeployedPackages();
        deployedPackages.setProperty(packageName, packageUrl);

        if (this.dpaConfPath == null) {
            logger.warn("Configuration file not specified");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(this.dpaConfPath);
            deployedPackages.store(fos, null);
            fos.flush();
            fos.getFD().sync();
            fos.close();
        } catch (IOException e) {
            logger.error("Error writing package configuration file", e);
        }
    }

    private void removePackageFromConfFile(String packageName) {
        Properties deployedPackages = readDeployedPackages();
        deployedPackages.remove(packageName);

        if (this.dpaConfPath == null) {
            logger.warn("Configuration file not specified");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(this.dpaConfPath);
            deployedPackages.store(fos, null);
            fos.flush();
            fos.getFD().sync();
            fos.close();
        } catch (IOException e) {
            logger.error("Error writing package configuration file", e);
        }
    }
}