/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
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
package org.eclipse.kura.deployment.rp.sh.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.system.SystemService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.deploymentadmin.spi.DeploymentSession;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShellScriptResourceProcessorImpl implements ResourceProcessor {

    private static final Logger s_logger = LoggerFactory.getLogger(ShellScriptResourceProcessorImpl.class);

    private static final String PACKAGES_PATH_PROPNAME = "kura.packages";

    private static final String INSTALL_ACTION = "install";
    private static final String UNINSTALL_ACTION = "uninstall";

    private File m_resourcesRootDirectory;

    private DeploymentPackage m_sourceDP;
    private DeploymentPackage m_targetDP;

    private final Map<String, File> m_sourceResourceFiles = new HashMap<String, File>();
    private final List<String> m_uninstalledResources = new ArrayList<String>();
    private final List<String> m_installedResources = new ArrayList<String>();
    private final List<String> m_droppedResources = new ArrayList<String>();

    BundleContext m_bundleContext;

    protected void activate(BundleContext bundleContext) {
        s_logger.info("activate");
        this.m_bundleContext = bundleContext;

        final Properties kuraProperties;
        final ServiceReference<SystemService> systemServiceRef = bundleContext.getServiceReference(SystemService.class);
        if (systemServiceRef == null) {
            throw new IllegalStateException("Unable to find instance of: " + SystemService.class.getName());
        }

        final SystemService systemService = bundleContext.getService(systemServiceRef);
        if (systemService == null) {
            throw new IllegalStateException("Unable to get instance of: " + SystemService.class.getName());
        }

        try {
            kuraProperties = systemService.getProperties();
        } finally {
            bundleContext.ungetService(systemServiceRef);
        }

        String packagesPath = kuraProperties.getProperty(PACKAGES_PATH_PROPNAME);
        if (packagesPath == null || packagesPath.isEmpty()) {
            throw new ComponentException("The value of '" + PACKAGES_PATH_PROPNAME + "' is not defined");
        }
        if (kuraProperties.getProperty(PACKAGES_PATH_PROPNAME) != null
                && kuraProperties.getProperty(PACKAGES_PATH_PROPNAME).trim().equals("kura/packages")) {
            kuraProperties.setProperty(PACKAGES_PATH_PROPNAME, "/opt/eurotech/kura/kura/packages");
            packagesPath = kuraProperties.getProperty(PACKAGES_PATH_PROPNAME);
            s_logger.warn("Overridding invalid kura.packages location");
        }

        this.m_resourcesRootDirectory = new File(packagesPath, "resources");
        if (!this.m_resourcesRootDirectory.exists()) {
            boolean success = this.m_resourcesRootDirectory.mkdirs();
            if (!success) {
                throw new ComponentException("Failed to make directory: " + this.m_resourcesRootDirectory.getPath());
            }
        }
    }

    protected void deactivate(BundleContext bundleContext) {
        s_logger.info("deactivate");
    }

    @Override
    public void begin(DeploymentSession dpSession) {
        s_logger.info("begin");

        this.m_sourceDP = dpSession.getSourceDeploymentPackage();
        this.m_targetDP = dpSession.getTargetDeploymentPackage();

        s_logger.info("Source Deployment Package name: '{}'", this.m_sourceDP.getName());
        s_logger.info("Target Deployment Package name: '{}'", this.m_targetDP.getName());
    }

    @Override
    public void cancel() {
        s_logger.info("cancel");
    }

    @Override
    public void commit() {
        s_logger.info("commit");

        // Delete dropped resources files
        for (String resource : this.m_droppedResources) {
            s_logger.info("Delete file for resource: '{}", resource);
            File file = getDPResourceFile(resource);
            if (file == null) {
                s_logger.warn("Resource file missing for resource: '{}'", resource);
            } else {
                boolean deleted = file.delete();
                if (!deleted) {
                    s_logger.warn("Failed to delete file for resource: '{}'", resource);
                }
            }
        }

        // Copy source resource files to Deployment Package resource directory
        for (Map.Entry<String, File> entry : this.m_sourceResourceFiles.entrySet()) {
            String resource = entry.getKey();
            s_logger.info("Copy file for resource: '{}'", resource);
            // Get the source resource file
            File sourceResourceFile = entry.getValue();

            // Construct the destination file
            File dir = new File(getResourcesRootDirectory(), this.m_sourceDP.getName());
            s_logger.info("getRootResourceDirectory() :'{}'", getResourcesRootDirectory());
            s_logger.info("m_sourceDP.getName() :'{}'", this.m_sourceDP.getName());
            File destFile = new File(dir, resource);
            try {
                s_logger.info("Copy file for resource: '{}' to path: '{}'", resource, destFile.getPath());
                FileUtils.copyFile(sourceResourceFile, destFile);
            } catch (IOException e) {
                s_logger.warn("Failed to copy file for resource: '{}'", resource);
            }
            sourceResourceFile.delete();
        }
    }

    @Override
    public void dropAllResources() throws ResourceProcessorException {
        s_logger.info("dropAllResources");

        String[] resources = this.m_targetDP.getResources();
        if (resources != null) {
            for (String resource : resources) {
                // FIXME?: we also get bundle resources here, not only
                // resources associated to this Resource Processor.
                ServiceReference<?> serviceReference = this.m_targetDP.getResourceProcessor(resource);
                if (serviceReference != null) {
                    if (serviceReference
                            .compareTo(this.m_bundleContext.getServiceReference(ResourceProcessor.class)) == 0) {
                        this.m_droppedResources.add(resource);
                    }
                }
            }
        }
    }

    @Override
    public void dropped(String resource) throws ResourceProcessorException {
        s_logger.info("Dropped resource: '{}'", resource);
        this.m_droppedResources.add(resource);
    }

    @Override
    public void prepare() throws ResourceProcessorException {
        s_logger.info("prepare");

        // Iterate over all resources belonging to the source Deployment Package.
        // Some resources might be new and other resources might be updated.
        for (Map.Entry<String, File> entry : this.m_sourceResourceFiles.entrySet()) {
            String resource = entry.getKey();
            // Get the source resource file
            File sourceResourceFile = entry.getValue();

            // Get the target resource file
            File targetResourceFile = getDPResourceFile(resource);
            if (targetResourceFile != null) {
                s_logger.info("Executing uninstall action for resource: '{}'", resource);
                try {
                    executeScript(targetResourceFile, UNINSTALL_ACTION);
                    this.m_uninstalledResources.add(resource);
                } catch (Exception e) {
                    s_logger.error("Failed to execute uninstall action for resource: '{}'", resource, e);
                    throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE,
                            "Failed to execute uninstall action for resource: " + resource, e);
                }
            }

            try {
                s_logger.info("Executing install action for resource: '{}'", resource);
                executeScript(sourceResourceFile, INSTALL_ACTION);
                this.m_installedResources.add(resource);
            } catch (Exception e) {
                s_logger.error("Failed to execute install action for resource: '{}'", resource, e);
                throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE,
                        "Failed to execute install action for resource: " + resource, e);
            }
        }

        for (String resource : this.m_droppedResources) {
            File targetResourceFile = getDPResourceFile(resource);
            if (targetResourceFile == null) {
                s_logger.warn(
                        "Target resource file missing for resource: '{}'. Proceed anyway but we might not be able to completely rollback the changes",
                        resource);
            } else {
                s_logger.info("Executing uninstall action for resource: '{}'", resource);
                try {
                    executeScript(targetResourceFile, UNINSTALL_ACTION);
                    this.m_uninstalledResources.add(resource);
                } catch (Exception e) {
                    s_logger.error("Failed to execute uninstall action for resource: '{}'", resource, e);
                    throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE,
                            "Failed to execute uninstall action for resource: " + resource, e);
                }
            }
        }
    }

    @Override
    public void process(String resource, InputStream is) throws ResourceProcessorException {
        s_logger.info("Processing resource: '{}'", resource);

        // Check deltas.
        // Caveat: we should calculate the resource deltas against the "target" Deployment Package, i.e.
        // a previous version of the Deployment Package.
        // The problem is that we start the framework with the osgi.clean property set to true
        // and we reinstall all the Deployment Packages from an external directory.
        // Since the OSGi cache is clean there will be no target Deployment Package when the Deployment Package is
        // reinstalled.
        // This Resource Processor stores Deployment Package resources
        // as files in a persistent directory on disk.
        // The delta is then calculated against the resources files in that directory.
        File targetResourceFile = getDPResourceFile(resource);
        if (targetResourceFile != null) {
            s_logger.info("Resource: '{}' already exists in Deployment Package: '{}'", resource, this.m_sourceDP);
            InputStream tis = null;
            try {
                tis = new FileInputStream(targetResourceFile);
                byte[] d1 = computeDigest(tis);
                byte[] d2 = computeDigest(is);

                if (digestsMatch(d1, d2)) {
                    s_logger.info("Digests for source and target resource: '{}' match. No need to update resource",
                            resource);
                    return;
                }
            } catch (FileNotFoundException e) {
                s_logger.warn("Unexpected exception. Proceed anyway", e);
            } catch (NoSuchAlgorithmException e) {
                s_logger.warn("Unexpected exception. Proceed anyway", e);
            } catch (IOException e) {
                s_logger.warn("Unexpected exception. Proceed anyway", e);
            } finally {
                if (tis != null) {
                    try {
                        tis.close();
                    } catch (IOException ex) {
                        s_logger.error("I/O Exception while closing BufferedReader!");
                    }
                }
            }
        }

        // Create temporary files for source resources
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("shrp", null);
        } catch (IOException e) {
            s_logger.error("Failed to create temporary file for resource: '{}'", resource);
            throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE,
                    "Failed to create temporary file for resource: " + resource, e);
        }
        tmpFile.deleteOnExit();
        try {
            FileUtils.copyInputStreamToFile(is, tmpFile);
        } catch (IOException e) {
            s_logger.error("Failed to copy input stream for resource: '{}'", resource);
            throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE,
                    "Failed to copy input stream for resource: " + resource, e);
        }
        this.m_sourceResourceFiles.put(resource, tmpFile);
    }

    @Override
    public void rollback() {
        s_logger.info("rollback");

        for (String resource : this.m_installedResources) {
            // Get the source resource file
            File sourceResourceFile = this.m_sourceResourceFiles.get(resource);
            try {
                s_logger.info("Executing uninstall action for resource: '{}'", resource);
                executeScript(sourceResourceFile, UNINSTALL_ACTION);
            } catch (Exception e) {
                s_logger.warn("Failed to execute uninstall action for resource: '{}'", resource, e);
            }
        }

        for (String resource : this.m_uninstalledResources) {
            // Get the target resource file
            File targetResourceFile = getDPResourceFile(resource);
            if (targetResourceFile == null) {
                s_logger.warn(
                        "Target resource file missing for resource: '{}'. Proceed anyway but we might not be able to completely rollback the changes",
                        resource);
            } else {
                s_logger.info("Executing install action for resource: '{}'", resource);
                try {
                    executeScript(targetResourceFile, INSTALL_ACTION);
                } catch (Exception e) {
                    s_logger.warn("Failed to execute install action for resource: '{}'", resource, e);
                }
            }
        }
    }

    private static void executeScript(File file, String action) throws Exception {
        String path = file.getCanonicalPath();
        String[] cmdarray = { "/bin/bash", path, action };
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmdarray);

        ProcessMonitorThread pmt = new ProcessMonitorThread(proc, null, 0);
        pmt.start();
        pmt.join();
        s_logger.error("Script stdout: {}", pmt.getStdout());
        s_logger.error("Script stderr: {}", pmt.getStderr());
        Exception e = pmt.getException();
        if (e != null) {
            s_logger.error("Exception executing install action for script: '{}'", e);
            throw e;
        } else {
            if (!pmt.isTimedOut()) {
                Integer exitValue = pmt.getExitValue();
                if (exitValue != 0) {
                    s_logger.error("Install action for script: '{}' failed with exit value: {}", path, exitValue);
                    throw new Exception("Install action for script: " + path + " failed with exit value: " + exitValue);
                }
            }
        }
    }

    // private File getTargetDPResourceFile(String resource) {
    // File dir = getTargetDPResourceDirectory();
    // if (dir == null) {
    // return null;
    // } else {
    // File file = new File(dir, resource);
    // if (file.exists()) {
    // return file;
    // }
    // }
    // return null;
    // }
    //
    // private File getTargetDPResourceDirectory() {
    // File root = getRootResourceDirectory();
    // if (root == null) {
    // return null;
    // } else {
    // File dir = new File(root, m_targetDP.getName());
    // if (dir.exists()) {
    // return dir;
    // }
    // }
    // return null;
    // }

    private File getDPResourceFile(String resource) {
        File dir = getDPResourceDirectory();
        if (dir == null) {
            return null;
        } else {
            File file = new File(dir, resource);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    private File getDPResourceDirectory() {
        File root = getResourcesRootDirectory();
        if (root == null) {
            return null;
        } else {
            String dpName;
            if (!this.m_sourceDP.getName().isEmpty()) {
                dpName = this.m_sourceDP.getName();
            } else {
                dpName = this.m_targetDP.getName();
            }

            File dir = new File(root, dpName);
            if (dir.exists()) {
                return dir;
            }
        }
        return null;
    }

    private File getResourcesRootDirectory() {
        return this.m_resourcesRootDirectory;
    }

    // private boolean belongsToTargetDP(String resource) {
    // boolean result = false;
    // String[] resources = m_targetDP.getResources();
    // if (resources != null) {
    // for (String targetResource : resources) {
    // if (resource.equals(targetResource)) {
    // result = true;
    // break;
    // }
    // }
    // }
    // return result;
    // }

    private static byte[] computeDigest(InputStream is) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream dis = new DigestInputStream(is, md);
        while (dis.read() != -1) {
            ;
        }
        byte[] digest = md.digest();
        return digest;
    }

    private static boolean digestsMatch(byte[] d1, byte[] d2) {
        if (d1 == null || d2 == null) {
            return false;
        }

        if (d1.length != d2.length) {
            return false;
        }

        for (int i = 0; i < d1.length; i++) {
            if (d1[i] != d2[i]) {
                return false;
            }
        }

        return true;
    }
}
