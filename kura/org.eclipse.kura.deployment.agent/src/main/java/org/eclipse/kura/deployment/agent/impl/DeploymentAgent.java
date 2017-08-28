/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and others
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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

    private Properties deployedPackages;

    private int connTimeout;
    private int readTimeout;

    protected void activate(ComponentContext componentContext) {

        this.deployedPackages = new Properties();

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
        if (!dpaConfFile.exists()) {
            try {
                dpaConfFile.createNewFile();
            } catch (IOException e) {
                throw new ComponentException("Cannot create empty DPA configuration file", e);
            }
        }

        File packagesDir = new File(this.packagesPath);
        if (!packagesDir.exists()) {
            if (!packagesDir.mkdirs()) {
                throw new ComponentException("Cannot create packages directory");
            }
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
                logger.warn("Interrupted", e);
            }
            logger.info("DeploymentAgent:Uninstall Thread terminated? - {}", this.uninstallerExecutor.isTerminated());
            this.uninstallerExecutor = null;
        }

        this.dpaConfPath = null;
        this.deployedPackages = null;
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
        if (this.instPackageUrls.contains(url)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isUninstallingDeploymentPackage(String name) {
        if (this.uninstPackageNames.contains(name)) {
            return true;
        }
        return false;
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
                    DeploymentPackage dp = null;
                    Exception ex = null;
                    try {
                        dp = installDeploymentPackageInternal(url);
                    } catch (Exception e) {
                        ex = e;
                        logger.error("Exception installing package at URL {}", url, e);
                    } finally {
                        boolean successful = dp != null ? true : false;
                        logger.info("Posting INSTALLED event for package at URL {}: {}", url,
                                successful ? "successful" : "unsuccessful");
                        this.instPackageUrls.poll();
                        postInstalledEvent(dp, url, successful, ex);
                    }
                }
            } catch (InterruptedException e) {
                logger.info("Exiting...");
                Thread.interrupted();
                return;
            } catch (Throwable t) {
                logger.error("Unexpected throwable", t);
            }
        } while (true);
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
                    logger.info("About to uninstall package ", name);
                    DeploymentPackage dp = null;
                    boolean successful = false;
                    Exception ex = null;
                    try {
                        dp = this.deploymentAdmin.getDeploymentPackage(name);
                        if (dp != null) {
                            dp.uninstall();

                            String sUrl = this.deployedPackages.getProperty(name);
                            File dpFile = new File(new URL(sUrl).getPath());
                            if (!dpFile.delete()) {
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
            } catch (InterruptedException e) {
                logger.info("Exiting...");
                Thread.interrupted();
                return;
            } catch (Throwable t) {
                logger.error("Unexpected throwable", t);
            }
        } while (true);
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

        if (this.dpaConfPath != null) {
            try (FileReader fr = new FileReader(this.dpaConfPath)) {
                this.deployedPackages.load(fr);
            } catch (IOException e) {
                logger.error("Exception loading deployment packages configuration file", e);
            }
        }

        Set<Object> packageNames = this.deployedPackages.keySet();
        for (Object packageName : packageNames) {
            String packageUrl = (String) this.deployedPackages.get(packageName);

            logger.info("Deploying package name {} at URL {}", packageName, packageUrl);
            try {
                installDeploymentPackageAsync(packageUrl);
            } catch (Exception e) {
                logger.error("Error installing package {}", packageName, e);
            }
        }
    }

    private DeploymentPackage installDeploymentPackageInternal(String urlSpec)
            throws DeploymentException, IOException, URISyntaxException {
        URL url = new URL(urlSpec);
        // Get the file base name from the URL
        String urlPath = url.getPath();
        String[] parts = urlPath.split("/");
        String dpBasename = parts[parts.length - 1];
        String dpPersistentFilePath = this.packagesPath + File.separator + dpBasename;
        File dpPersistentFile = new File(dpPersistentFilePath);

        DeploymentPackage dp = null;
        File dpFile = null;
        InputStream dpInputStream = null;
        BufferedReader br = null;
        try {
            // Download the package to a temporary file unless it already resides
            // on the local filesystem.
            if (!"file".equals(url.getProtocol())) {
                dpFile = File.createTempFile("dpa", null);
                dpFile.deleteOnExit();

                FileUtils.copyURLToFile(url, dpFile, this.connTimeout, this.readTimeout);
            } else {
                dpFile = new File(url.getPath());
            }

            dpInputStream = new FileInputStream(dpFile);
            dp = this.deploymentAdmin.installDeploymentPackage(dpInputStream);

            // Now we need to copy the deployment package file to the Kura
            // packages directory unless it's already there.
            if (!dpFile.getCanonicalPath().equals(dpPersistentFile.getCanonicalPath())) {
                logger.debug("dpFile.getCanonicalPath(): {}", dpFile.getCanonicalPath());
                logger.debug("dpPersistentFile.getCanonicalPath(): {}", dpPersistentFile.getCanonicalPath());
                FileUtils.copyFile(dpFile, dpPersistentFile);
                addPackageToConfFile(dp.getName(), "file:" + dpPersistentFilePath);
            }
        } catch (DeploymentException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    logger.error("I/O Exception while closing BufferedReader!");
                }
            }

            if (dpInputStream != null) {
                try {
                    dpInputStream.close();
                } catch (IOException e) {
                    logger.warn("Cannot close input stream", e);
                }
            }
            // The file from which we have installed the deployment package will be deleted
            // unless it's a persistent deployment package file.
            if (dpFile != null && !dpFile.getCanonicalPath().equals(dpPersistentFile.getCanonicalPath())) {
                dpFile.delete();
            }
        }

        return dp;
    }

    private void addPackageToConfFile(String packageName, String packageUrl) {
        this.deployedPackages.setProperty(packageName, packageUrl);

        if (this.dpaConfPath == null) {
            logger.warn("Configuration file not specified");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(this.dpaConfPath);
            this.deployedPackages.store(fos, null);
            fos.flush();
            fos.getFD().sync();
            fos.close();
        } catch (IOException e) {
            logger.error("Error writing package configuration file", e);
        }
    }

    private void removePackageFromConfFile(String packageName) {
        this.deployedPackages.remove(packageName);

        if (this.dpaConfPath == null) {
            logger.warn("Configuration file not specified");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(this.dpaConfPath);
            this.deployedPackages.store(fos, null);
            fos.flush();
            fos.getFD().sync();
            fos.close();
        } catch (IOException e) {
            logger.error("Error writing package configuration file", e);
        }
    }
}