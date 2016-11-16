/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.deployment.customizer.upgrade.rp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.deploymentadmin.spi.DeploymentSession;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgradeScriptResourceProcessorImpl implements ResourceProcessor {

    private static Logger s_logger = LoggerFactory.getLogger(UpgradeScriptResourceProcessorImpl.class);

    private BundleContext m_bundleContext;
    private final Map<String, File> m_sourceResourceFiles = new LinkedHashMap<String, File>(); // preserve insertion
 // order

    protected void activate(BundleContext bundleContext) {
        this.m_bundleContext = bundleContext;
        s_logger.info("Activating -> " + this.m_bundleContext.getBundle().getSymbolicName());
    }

    protected void deactivate() {
        s_logger.info("Deactivating -> " + this.m_bundleContext.getBundle().getSymbolicName());
    }

    @Override
    public void begin(DeploymentSession session) {
        s_logger.debug("Upgrade script resource processor: begin");
    }

    @Override
    public void process(String name, InputStream stream) throws ResourceProcessorException {
        s_logger.debug("Upgrade script resource processor: process");

        // Create temporary files for source resources
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("upgrade_", null);
        } catch (IOException ioe) {
            s_logger.error("Failed to create temporary file for resource: '{}'", name);
            throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE,
                    "Failed to create temporary file for resource: " + name, ioe);
        }

        try {
            FileUtils.copyInputStreamToFile(stream, tmpFile);
        } catch (IOException ioe) {
            s_logger.error("Failed to copy input stream for resource: '{}'", name);
            throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE,
                    "Failed to copy input stream for resource: " + name, ioe);
        }
        this.m_sourceResourceFiles.put(name, tmpFile);
    }

    @Override
    public void dropped(String resource) throws ResourceProcessorException {
        s_logger.debug("Upgrade script resource processor: dropped");

    }

    @Override
    public void dropAllResources() throws ResourceProcessorException {
        s_logger.debug("Upgrade script resource processor: droppedAllResources");

    }

    @Override
    public void prepare() throws ResourceProcessorException {
        s_logger.debug("Upgrade script resource processor: prepare");
        // Iterate over list of resources
        Set<Entry<String, File>> entrySet = this.m_sourceResourceFiles.entrySet();
        Iterator<Entry<String, File>> it = entrySet.iterator();
        try {
            while (it.hasNext()) {
                Entry<String, File> entry = it.next();
                File upgradeScript = entry.getValue();
                executeScript(upgradeScript);
            }
        } catch (Exception e) {
            s_logger.error("Error during prepare");
            throw new ResourceProcessorException(ResourceProcessorException.CODE_PREPARE, "Error during prepare", e);
        }
    }

    @Override
    public void commit() {
        s_logger.debug("Upgrade script resource processor: commit");
        cleanup();
    }

    @Override
    public void rollback() {
        s_logger.debug("Upgrade script resource processor: rollback");
        cleanup();
    }

    @Override
    public void cancel() {
        s_logger.debug("Upgrade script resource processor: cancel");
        cleanup();
    }

    private void executeScript(File file) throws Exception {
        String path = file.getCanonicalPath();
        String[] cmdarray = { "/bin/bash", path };
        Runtime rt = Runtime.getRuntime();
        Process proc = null;
        try {
            proc = rt.exec(cmdarray);
            if (proc.waitFor() != 0) {
                s_logger.error("Script {} failed with exit value {}", path, proc.exitValue());
            }
            // FIXME: streams must be consumed concurrently
        } catch (Exception e) {
            s_logger.error("Error executing process for script {}", path, e);
            throw e;
        } finally {
            if (proc != null) {
                proc.destroy();
            }
        }
    }

    private void cleanup() {
        Set<Entry<String, File>> entrySet = this.m_sourceResourceFiles.entrySet();
        Iterator<Entry<String, File>> it = entrySet.iterator();

        while (it.hasNext()) {
            Entry<String, File> entry = it.next();
            File file = entry.getValue();
            try {
                file.delete();
            } catch (Exception e) {
                s_logger.warn("Failed to delete file", e);
            }
        }
        this.m_sourceResourceFiles.clear();
    }
}
